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
import com.google.firebase.firestore.SetOptions

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

    // --- Leaderboard Listener ---
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
                if (!safeEmail.isNullOrBlank()) {
                    authVm.signUpWithEmail(
                        email = safeEmail,
                        password = password,
                        username = identifier
                    )
                }
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

    // --- User Stats Listener ---
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
                    loadingStats = false
                    return@addSnapshotListener
                }

                if (snap != null && snap.exists()) {
                    val fs = snap.toObject(FirestoreUserStats::class.java)
                    currentStats = fs?.let {
                        // Calculate maxXp locally based on totalSteps so the UI bar is correct
                        val (_, _, calculatedMaxXp) = getLevelData(it.totalSteps.toInt())

                        UserStats(
                            username = it.displayName.ifBlank { "Unknown" },
                            level = it.level,
                            currentXp = it.currentXp,
                            maxXp = calculatedMaxXp,
                            steps = it.totalSteps.toInt(),
                            workoutsCompleted = it.workoutsCompleted
                        )
                    }
                } else {
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
            onAuthenticate = handleAuth
        )
    } else {
        val currentUser = currentStats
        if (loadingStats || currentUser == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        // --- Live Hardware Sensor Processor (REAL-TIME XP CONVERSION) ---
        LaunchedEffect(uid, realSessionSteps) {
            if (uid == null) return@LaunchedEffect
            if (!hasActivityPermission) return@LaunchedEffect

            val sessionRef = db.collection("StepSessions").document(uid)
            val statsRef = db.collection("UserData").document(uid)

            // Only run when steps increase
            if (realSessionSteps <= 0) return@LaunchedEffect

            // Use a transaction to safely read totalSteps, calculate new Level/XP, and update
            db.runTransaction { txn ->
                val sessionSnap = txn.get(sessionRef)
                val last = (sessionSnap.getLong("LastSessionSteps") ?: 0L).toInt()

                val delta = realSessionSteps - last

                // If delta is 0 or negative (sensor reset), just update session tracking
                if (delta <= 0) {
                    txn.set(
                        sessionRef,
                        mapOf("LastSessionSteps" to realSessionSteps, "UpdatedAt" to FieldValue.serverTimestamp()),
                        SetOptions.merge()
                    )
                    return@runTransaction null
                }

                // 1. Get current total steps from Firestore
                val statsSnap = txn.get(statsRef)
                val currentTotalSteps = (statsSnap.getLong("totalSteps") ?: 0L).toInt()
                val newTotalSteps = currentTotalSteps + delta

                // 2. Calculate new Level and XP using your algorithm
                val (newLevel, newCurrentXp, _) = getLevelData(newTotalSteps)

                // 3. Update UserData with ALL new stats
                txn.set(
                    statsRef,
                    mapOf(
                        "totalSteps" to newTotalSteps.toLong(),
                        "level" to newLevel,
                        "currentXp" to newCurrentXp,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )

                // 4. Update StepSession
                txn.set(
                    sessionRef,
                    mapOf(
                        "LastSessionSteps" to realSessionSteps,
                        "UpdatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )

                null
            }
        }

        // --- Mock Workout Simulator (UPDATED FOR XP) ---
        val simulateWorkout: () -> Unit = simulateWorkout@{
            val uidLocal = uid ?: return@simulateWorkout
            val statsRef = db.collection("UserData").document(uidLocal)

            db.runTransaction { txn ->
                val snapshot = txn.get(statsRef)
                val currentTotal = (snapshot.getLong("totalSteps") ?: 0L).toInt()
                val currentWorkouts = (snapshot.getLong("workoutsCompleted") ?: 0L).toInt()

                val newTotal = currentTotal + 1500
                // Calculate new Level/XP for the simulator too!
                val (newLevel, newXp, _) = getLevelData(newTotal)

                txn.set(
                    statsRef,
                    mapOf(
                        "totalSteps" to newTotal.toLong(),
                        "level" to newLevel,
                        "currentXp" to newXp,
                        "workoutsCompleted" to currentWorkouts + 1,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
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
                    "history" -> HistoryScreen(user = currentUser)
                    "leaderboard" -> LeaderboardScreen(currentUser = currentUser, leaderboard = leaderboard, isLoading = leaderboardLoading)
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