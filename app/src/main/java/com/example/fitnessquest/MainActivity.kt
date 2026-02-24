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
import com.google.firebase.firestore.Query
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

    // 2. Global State Variables
    var currentScreen by remember { mutableStateOf("home") }
    var isInitialized by remember { mutableStateOf(false) }
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

    val db = remember { FirebaseFirestore.getInstance() }
    val uid = authState.uid

    var leaderboard by remember { mutableStateOf<List<LeaderboardRow>>(emptyList()) }
    var leaderboardLoading by remember { mutableStateOf(false) }

    DisposableEffect(uid) {
        if (uid == null) {
            leaderboard = emptyList()
            leaderboardLoading = false
            return@DisposableEffect onDispose { }
        }

        leaderboardLoading = true

        val reg = db.collection("UserData")
            .orderBy("totalSteps", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snaps, err ->
                if (err != null || snaps == null) {
                    leaderboardLoading = false
                    return@addSnapshotListener
                }

                leaderboard = snaps.documents.mapNotNull { d ->
                    d.toObject(LeaderboardRow::class.java)
                }

                leaderboardLoading = false
            }

        onDispose { reg.remove() }
    }

    var currentStats by remember { mutableStateOf<UserStats?>(null) }
    var loadingStats by remember { mutableStateOf(true) }
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

// React to login and route to home
    LaunchedEffect(uid) {
        if (uid != null) currentScreen = "home"
    }

// Firestore listener: userStats/{uid}
    DisposableEffect(uid) {
        if (uid == null) {
            loadingStats = false
            currentStats = null
            return@DisposableEffect onDispose { }
        }

        loadingStats = true

        val reg = db.collection("UserData")
            .document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    // optional: store error somewhere
                    loadingStats = false
                    return@addSnapshotListener
                }

                if (snap != null && snap.exists()) {
                    val fs = snap.toObject(FirestoreUserStats::class.java)
                    currentStats = fs?.let {
                        UserStats(
                            username = it.displayName.ifBlank { "Unknown" },
                            level = it.level,
                            currentXp = it.currentXp,
                            steps = it.totalSteps.toInt(),
                            workoutsCompleted = it.workoutsCompleted
                            // stepHistory/xpHistory keep defaults from UserStats
                        )
                    }                } else {
                    currentStats = null
                }

                loadingStats = false
            }

        onDispose {
            reg.remove()
        }
    }
    DisposableEffect(Unit) { onDispose { sensorManager?.stop() } }

    // 5. Load Initial Data
    LaunchedEffect(Unit) {
        isInitialized = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission)
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasActivityPermission)
            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
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
        val currentUser = currentStats
        if (loadingStats || currentUser == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }        // --- Live Hardware Sensor Processor ---
        LaunchedEffect(uid, realSessionSteps) {
            if (uid == null) return@LaunchedEffect
            if (!hasActivityPermission) return@LaunchedEffect

            val sessionRef = db.collection("StepSessions").document(uid)
            val statsRef = db.collection("UserData").document(uid)

            // Only run when steps increase
            if (realSessionSteps <= 0) return@LaunchedEffect

            // Use a transaction so delta + lastSessionSteps update is atomic
            db.runTransaction { txn ->
                val sessionSnap = txn.get(sessionRef)
                val last = (sessionSnap.getLong("LastSessionSteps") ?: 0L).toInt()

                val delta = realSessionSteps - last
                if (delta <= 0) {
                    // Nothing new, or sensor reset to smaller value
                    // In case of reset, you can decide to set lastSessionSteps = realSessionSteps
                    txn.set(sessionRef, mapOf("LastSessionSteps" to realSessionSteps, "UpdatedAt" to FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge())
                    return@runTransaction null
                }

                // increment totalSteps by delta
                txn.set(
                    statsRef,
                    mapOf(
                        "totalSteps" to FieldValue.increment(delta.toLong()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )

                // store new lastSessionSteps
                txn.set(
                    sessionRef,
                    mapOf(
                        "LastSessionSteps" to realSessionSteps,
                        "UpdatedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )

                null
            }
        }

        // --- Mock Workout Simulator (for testing on emulator) ---
        val simulateWorkout: () -> Unit = simulateWorkout@{
            val uidLocal = uid ?: return@simulateWorkout

            val statsRef = db.collection("UserData").document(uidLocal)
            statsRef.set(
                mapOf(
                    "totalSteps" to FieldValue.increment(1500),
                    "workoutsCompleted" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
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
                    "leaderboard" -> LeaderboardScreen(currentUser = currentUser,leaderboard = leaderboard,isLoading = leaderboardLoading)
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