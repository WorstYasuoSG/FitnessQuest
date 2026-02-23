package com.example.fitnessquest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context

// Imports from our new packages
import com.example.fitnessquest.data.*
import com.example.fitnessquest.sensor.*
import com.example.fitnessquest.utils.*
import com.example.fitnessquest.ui.theme.FitnessQuestTheme

//Firebase imports

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitnessQuestTheme {
                FitnessQuestApp()
            }
        }
    }
}

@Composable
fun FitnessQuestApp() {
    // 1. Initialize Sensor & Storage
    val stepViewModel: StepViewModel = viewModel()
    val realSessionSteps by stepViewModel.steps
    val context = LocalContext.current
    val storageManager = remember { StorageManager(context.getSharedPreferences("FitnessQuestDB", Context.MODE_PRIVATE)) }

    // 2. Global State Variables
    val userDatabase = remember { mutableStateMapOf<String, UserStats>() }
    var currentScreen by remember { mutableStateOf("home") }
    var isInitialized by remember { mutableStateOf(false) }
    var sessionStepsProcessed by remember { mutableStateOf(0) }
    var achievementToPopUp by remember { mutableStateOf<Achievement?>(null) }

    // 3. Permission Handling
    var hasNotificationPermission by remember { mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else true) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> hasNotificationPermission = isGranted }

    var hasActivityPermission by remember { mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED else true) }
    val activityPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasActivityPermission = it }

    // 4. Start Hardware Sensor
    var sensorManager by remember { mutableStateOf<StepSensorManager?>(null) }

    val authVm: AuthViewModel = viewModel()
    val authState by authVm.uiState.collectAsState()
    val handleAuth: (String, String?, String, Boolean) -> Unit =
        { identifier, email, password, isSignUp ->

            if (isSignUp) {

                val safeEmail = email

                if (safeEmail.isNullOrBlank()) {
                }

                authVm.signUpWithEmail(
                    email = safeEmail!!,
                    password = password,
                    username = identifier
                )

            } else {

                authVm.signInWithEmailOrUsername(
                    identifier,
                    password
                )
            }
        }

    LaunchedEffect(hasActivityPermission) {
        if (hasActivityPermission) {
            sensorManager = StepSensorManager(context) { stepViewModel.updateSteps(it) }
            sensorManager?.start()
        }
    }

    LaunchedEffect(authState.uid) {
        if (authState.uid != null) currentScreen = "home"
    }
    DisposableEffect(Unit) { onDispose { sensorManager?.stop() } }

    // 5. Load Initial Data
    LaunchedEffect(Unit) {
        userDatabase.putAll(storageManager.getAllUsers().associateBy { it.username })
        isInitialized = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasActivityPermission) activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    if (!isInitialized) return


    // --- Authentication Flow ---
    if (authState.uid == null) {
        LoginScreen(
           /* onAuthenticate = { identifier, email, password, isSignUp ->
                if (isSignUp) {

                    authVm.signUpWithEmail(
                        email = email!!,
                        password = password,
                        username = identifier
                    )
                } else {
                    authVm.signInWithEmailOrUsername(identifier, password)
                }
            },*/
            onAuthenticate = handleAuth,
            externalError = authState.error,
            isLoading = authState.loading
        )
    } else {
        val uid = authState.uid!!
        val currentUser = remember(uid) { UserStats(username = "Loading...") }
        // --- Live Hardware Sensor Processor ---
        LaunchedEffect(realSessionSteps) {
            if (realSessionSteps > sessionStepsProcessed) {
                val deltaSteps = realSessionSteps - sessionStepsProcessed
                sessionStepsProcessed = realSessionSteps

                val oldAchievements = getDynamicAchievements(currentUser)
                val newTotalSteps = currentUser.steps + deltaSteps
                val (newLevel, newCurrentXp, newMaxXp) = getLevelData(newTotalSteps)

                val updatedStepHistory = currentUser.stepHistory.toMutableList()
                updatedStepHistory[updatedStepHistory.lastIndex] += deltaSteps
                val updatedXpHistory = currentUser.xpHistory.toMutableList()
                updatedXpHistory[updatedXpHistory.lastIndex] += deltaSteps

                val updatedUser = currentUser.copy(steps = newTotalSteps, level = newLevel, currentXp = newCurrentXp, maxXp = newMaxXp, stepHistory = updatedStepHistory, xpHistory = updatedXpHistory)

                val newlyUnlocked = getDynamicAchievements(updatedUser).filter { newAch -> newAch.isUnlocked && oldAchievements.none { it.title == newAch.title && it.isUnlocked } }
                if (newlyUnlocked.isNotEmpty()) {
                    achievementToPopUp = newlyUnlocked.first()
                    if (hasNotificationPermission) sendAchievementNotification(context, achievementToPopUp!!.title, achievementToPopUp!!.description)
                }
            }
        }

        // --- Mock Workout Simulator (for testing on emulator) ---
        val simulateWorkout = {
            val oldAchievements = getDynamicAchievements(currentUser)
            val newTotalSteps = currentUser.steps + 1500
            val (newLevel, newCurrentXp, newMaxXp) = getLevelData(newTotalSteps)

            val updatedStepHistory = currentUser.stepHistory.toMutableList()
            updatedStepHistory[updatedStepHistory.lastIndex] += 1500
            val updatedXpHistory = currentUser.xpHistory.toMutableList()
            updatedXpHistory[updatedXpHistory.lastIndex] += 1500

            val updatedUser = currentUser.copy(steps = newTotalSteps, level = newLevel, currentXp = newCurrentXp, maxXp = newMaxXp, stepHistory = updatedStepHistory, xpHistory = updatedXpHistory, workoutsCompleted = currentUser.workoutsCompleted + 1)


            val newlyUnlocked = getDynamicAchievements(updatedUser).filter { newAch -> newAch.isUnlocked && oldAchievements.none { it.title == newAch.title && it.isUnlocked } }
            if (newlyUnlocked.isNotEmpty()) {
                achievementToPopUp = newlyUnlocked.first() 
                if (hasNotificationPermission) sendAchievementNotification(context, achievementToPopUp!!.title, achievementToPopUp!!.description)
            }
        }

        // --- In-App Alert Dialog ---
        if (achievementToPopUp != null) {
            AlertDialog(
                onDismissRequest = { achievementToPopUp = null },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Trophy Unlocked!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                },
                text = {
                    Column {
                        Text(achievementToPopUp!!.title, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(6.dp))
                        Text(achievementToPopUp!!.description, color = Color.Gray, fontSize = 16.sp)
                    }
                },
                confirmButton = { TextButton({ achievementToPopUp = null }) { Text("Awesome!", fontWeight = FontWeight.SemiBold) } }
            )
        }

        // --- Router Scaffold ---
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home", fontSize = 10.sp) }, selected = currentScreen == "home", onClick = { currentScreen = "home" })
                    NavigationBarItem(icon = { Icon(Icons.Default.DateRange, null) }, label = { Text("Logs", fontSize = 10.sp) }, selected = currentScreen == "history", onClick = { currentScreen = "history" })
                    NavigationBarItem(icon = { Icon(Icons.Default.List, null) }, label = { Text("Rank", fontSize = 10.sp) }, selected = currentScreen == "leaderboard", onClick = { currentScreen = "leaderboard" })
                    NavigationBarItem(icon = { Icon(Icons.Default.Star, null) }, label = { Text("Awards", fontSize = 10.sp) }, selected = currentScreen == "achievements", onClick = { currentScreen = "achievements" })
                    NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile", fontSize = 10.sp) }, selected = currentScreen == "profile", onClick = { currentScreen = "profile" })
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).background(MaterialTheme.colorScheme.background)) {
                when (currentScreen) {
                    "home" -> HomeScreen(user = currentUser, onWorkout = simulateWorkout)
                    "history" -> HistoryScreen(stepData = currentUser.stepHistory, xpData = currentUser.xpHistory)
                    "leaderboard" -> LeaderboardScreen(currentUser = currentUser, allUsers = userDatabase.values.toList())
                    "achievements" -> AchievementsScreen(currentUser = currentUser)
                    "profile" -> ProfileScreen(
                        user = currentUser,
                        onLogout = { authVm.signOut() }
                    )
                }
            }
        }
    }
}