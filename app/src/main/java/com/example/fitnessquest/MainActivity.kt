package com.example.fitnessquest

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.fitnessquest.ui.theme.FitnessQuestTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log

//
import com.google.firebase.firestore.FirebaseFirestore

class StepViewModel : ViewModel() {
    private val _steps = mutableStateOf(0)
    val steps: State<Int> = _steps

    fun updateSteps(newSteps: Int) {
        _steps.value = newSteps
    }
}

class StepSensorManager(
    context: Context,
    private val onStepChanged: (Int) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val stepSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var initialStepCount: Int? = null

    fun start() {
        stepSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val totalSteps = event.values[0].toInt()

        if (initialStepCount == null) {
            initialStepCount = totalSteps
        }

        val currentSessionSteps = totalSteps - (initialStepCount ?: 0)

        onStepChanged(currentSessionSteps)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

// --- Data Models ---
data class UserStats(
    val username: String,
    val level: Int = 1,
    val currentXp: Int = 0,
    val maxXp: Int = 1000,
    val steps: Int = 0,
    val stepGoal: Int = 10000,
    val stepHistory: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val xpHistory: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val workoutsCompleted: Int = 0
)

data class Achievement(
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val icon: ImageVector
)

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val level: Int,
    val isCurrentUser: Boolean = false
)

// --- Custom Logo Component ---
@Composable
fun AnimatedFitnessQuestLogo(modifier: Modifier = Modifier) {
    // Creates a smooth, infinite pulsing animation
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f, // Grows by 5%
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "LogoPulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier.scale(scale)) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 1. Background Gradient Circle
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(primaryColor, secondaryColor),
                start = Offset(0f, 0f),
                end = Offset(canvasWidth, canvasHeight)
            ),
            radius = canvasWidth / 2
        )

        // 2. The Sun / Star
        drawCircle(
            color = tertiaryColor,
            radius = canvasWidth * 0.15f,
            center = Offset(canvasWidth * 0.7f, canvasHeight * 0.35f)
        )

        // 3. The Mountain / Heartbeat Path
        val path = Path().apply {
            moveTo(canvasWidth * 0.15f, canvasHeight * 0.6f)
            lineTo(canvasWidth * 0.35f, canvasHeight * 0.6f)
            lineTo(canvasWidth * 0.5f, canvasHeight * 0.25f) // The peak
            lineTo(canvasWidth * 0.65f, canvasHeight * 0.8f) // The valley
            lineTo(canvasWidth * 0.75f, canvasHeight * 0.55f)
            lineTo(canvasWidth * 0.85f, canvasHeight * 0.55f)
        }

        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(
                width = canvasWidth * 0.08f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

// --- Dynamic Achievement Logic ---
fun getDynamicAchievements(user: UserStats): List<Achievement> {
    return listOf(
        Achievement("First Steps", "Walk 1,000 steps", user.steps >= 1000, Icons.Default.Person),
        Achievement("Warrior", "Complete 5 workouts", user.workoutsCompleted >= 5, Icons.Default.Star),
        Achievement("Marathoner", "Walk 42,000 steps", user.steps >= 42000, Icons.Default.Home),
        Achievement("Dedicated", "Reach Level 5", user.level >= 5, Icons.Default.Star)
    )
}

// --- System Notification Helper ---
fun sendAchievementNotification(context: Context, title: String, description: String) {
    val channelId = "achievement_channel"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Achievements", NotificationManager.IMPORTANCE_HIGH).apply {
            this.description = "Notifications for unlocked achievements"
        }
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.star_on)
        .setContentTitle("🏆 Trophy Unlocked: $title!")
        .setContentText(description)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(title.hashCode(), notification)
}

// --- Local Storage Logic ---
fun UserStats.toCsv(): String {
    val sHist = stepHistory.joinToString(",")
    val xHist = xpHistory.joinToString(",")
    return "$username;$level;$currentXp;$maxXp;$steps;$stepGoal;$sHist;$xHist;$workoutsCompleted"
}

fun userStatsFromCsv(csv: String): UserStats? {
    try {
        val p = csv.split(";")
        if (p.size < 8) return null
        val workouts = if (p.size >= 9) p[8].toInt() else 0
        return UserStats(
            username = p[0],
            level = p[1].toInt(),
            currentXp = p[2].toInt(),
            maxXp = p[3].toInt(),
            steps = p[4].toInt(),
            stepGoal = p[5].toInt(),
            stepHistory = p[6].split(",").map { it.toInt() },
            xpHistory = p[7].split(",").map { it.toInt() },
            workoutsCompleted = workouts
        )
    } catch (e: Exception) { return null }
}

class StorageManager(private val prefs: SharedPreferences) {
    fun saveUser(user: UserStats) {
        prefs.edit().putString("user_${user.username}", user.toCsv()).apply()
        val users = getUsersList().toMutableSet()
        users.add(user.username)
        prefs.edit().putStringSet("all_users", users).apply()
    }
    fun getUser(username: String): UserStats? {
        val csv = prefs.getString("user_$username", null) ?: return null
        return userStatsFromCsv(csv)
    }
    fun getAllUsers(): List<UserStats> {
        return getUsersList().mapNotNull { getUser(it) }
    }
    private fun getUsersList(): Set<String> {
        return prefs.getStringSet("all_users", emptySet()) ?: emptySet()
    }
    fun saveLoggedInUser(username: String?) {
        if (username == null) prefs.edit().remove("logged_in_user").apply()
        else prefs.edit().putString("logged_in_user", username).apply()
    }
    fun getLoggedInUser(): String? = prefs.getString("logged_in_user", null)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val TAG = "FQ_FIREBASE"

        val db = FirebaseFirestore.getInstance()
        db.collection("test")
            .add(mapOf("connected" to true, "ts" to System.currentTimeMillis()))
            .addOnSuccessListener { doc ->
                Log.d(TAG, "Firestore connected. Wrote docId=${doc.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore error", e)
            }

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
    val stepViewModel: StepViewModel = viewModel()
    val realSteps by stepViewModel.steps
    val context = LocalContext.current
    val storageManager = remember {
        StorageManager(context.getSharedPreferences("FitnessQuestDB", Context.MODE_PRIVATE))
    }

    val userDatabase = remember { mutableStateMapOf<String, UserStats>() }
    var loggedInUsername by remember { mutableStateOf<String?>(null) }
    var currentScreen by remember { mutableStateOf("home") }
    var isInitialized by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        } else {
            mutableStateOf(true)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        userDatabase.putAll(storageManager.getAllUsers().associateBy { it.username })
        loggedInUsername = storageManager.getLoggedInUser()
        if (loggedInUsername != null) currentScreen = "home"
        isInitialized = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var hasActivityPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val activityPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            hasActivityPermission = it
        }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasActivityPermission) {
            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    var sensorManager by remember { mutableStateOf<StepSensorManager?>(null) }

    LaunchedEffect(hasActivityPermission) {
        if (hasActivityPermission) {
            sensorManager = StepSensorManager(context) {
                stepViewModel.updateSteps(it)
            }
            sensorManager?.start()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sensorManager?.stop()
        }
    }
    if (!isInitialized) return 

    var achievementToPopUp by remember { mutableStateOf<Achievement?>(null) }

    if (loggedInUsername == null) {
        LoginScreen(
            onAuthenticate = { username, isSignUp ->
                if (isSignUp) {
                    if (userDatabase.containsKey(username)) {
                        "Username already taken. Please choose another."
                    } else {
                        val newUser = UserStats(username = username)
                        userDatabase[username] = newUser
                        storageManager.saveUser(newUser) 
                        storageManager.saveLoggedInUser(username) 
                        loggedInUsername = username
                        currentScreen = "home"
                        null 
                    }
                } else {
                    if (userDatabase.containsKey(username)) {
                        storageManager.saveLoggedInUser(username) 
                        loggedInUsername = username
                        currentScreen = "home"
                        null 
                    } else {
                        "Account not found. Please sign up first." 
                    }
                }
            }
        )
    } else {
        val currentUser = userDatabase[loggedInUsername]!!
        LaunchedEffect(realSteps) {
            if (realSteps != currentUser.steps) {

                val updatedStepHistory = currentUser.stepHistory.toMutableList()
                updatedStepHistory[updatedStepHistory.lastIndex] = realSteps

                val updatedUser = currentUser.copy(
                    steps = realSteps,
                    stepHistory = updatedStepHistory
                )

                userDatabase[loggedInUsername!!] = updatedUser
                storageManager.saveUser(updatedUser)
            }
        }

        val simulateWorkout = {
            val oldAchievements = getDynamicAchievements(currentUser)
            var newXp = currentUser.currentXp + 200
            var newLevel = currentUser.level
            var newMax = currentUser.maxXp

            if (newXp >= newMax) {
                newLevel++
                newXp -= newMax
                newMax = (newMax * 1.2).toInt()
            }

            val updatedStepHistory = currentUser.stepHistory.toMutableList()
            updatedStepHistory[updatedStepHistory.lastIndex] += 1500

            val updatedXpHistory = currentUser.xpHistory.toMutableList()
            updatedXpHistory[updatedXpHistory.lastIndex] += 200

            val updatedUser = currentUser.copy(
                level = newLevel, 
                currentXp = newXp, 
                maxXp = newMax,
                steps = realSteps,
                stepHistory = updatedStepHistory,
                xpHistory = updatedXpHistory,
                workoutsCompleted = currentUser.workoutsCompleted + 1 
            )
            
            userDatabase[loggedInUsername!!] = updatedUser
            storageManager.saveUser(updatedUser) 

            val newAchievements = getDynamicAchievements(updatedUser)
            val newlyUnlocked = newAchievements.filter { newAch ->
                newAch.isUnlocked && oldAchievements.none { it.title == newAch.title && it.isUnlocked }
            }

            if (newlyUnlocked.isNotEmpty()) {
                val latestAchievement = newlyUnlocked.first() 
                achievementToPopUp = latestAchievement 
                if (hasNotificationPermission) {
                    sendAchievementNotification(context, latestAchievement.title, latestAchievement.description)
                }
            }
        }

        if (achievementToPopUp != null) {
            AlertDialog(
                onDismissRequest = { achievementToPopUp = null },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Trophy Unlocked!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                },
                text = {
                    Column {
                        Text(achievementToPopUp!!.title, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(achievementToPopUp!!.description, color = Color.Gray, fontSize = 16.sp)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { achievementToPopUp = null }
                    ) {
                        Text("Awesome!", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp 
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 10.sp, maxLines = 1) },
                        selected = currentScreen == "home",
                        onClick = { currentScreen = "home" }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "History") },
                        label = { Text("Logs", fontSize = 10.sp, maxLines = 1) },
                        selected = currentScreen == "history",
                        onClick = { currentScreen = "history" }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "Rank") },
                        label = { Text("Rank", fontSize = 10.sp, maxLines = 1) },
                        selected = currentScreen == "leaderboard",
                        onClick = { currentScreen = "leaderboard" }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Star, contentDescription = "Awards") },
                        label = { Text("Awards", fontSize = 10.sp, maxLines = 1) },
                        selected = currentScreen == "achievements",
                        onClick = { currentScreen = "achievements" }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile", fontSize = 10.sp, maxLines = 1) },
                        selected = currentScreen == "profile",
                        onClick = { currentScreen = "profile" }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)) {
                when (currentScreen) {
                    "home" -> HomeScreen(user = currentUser, onWorkout = simulateWorkout)
                    "history" -> HistoryScreen(stepData = currentUser.stepHistory, xpData = currentUser.xpHistory)
                    "leaderboard" -> LeaderboardScreen(currentUser = currentUser, allUsers = userDatabase.values.toList())
                    "achievements" -> AchievementsScreen(currentUser = currentUser)
                    "profile" -> ProfileScreen(
                        user = currentUser, 
                        onLogout = { 
                            storageManager.saveLoggedInUser(null) 
                            loggedInUsername = null 
                        }
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// LOGIN / SIGN UP SCREEN
// ------------------------------------------------------------------------
@Composable
fun LoginScreen(onAuthenticate: (username: String, isSignUp: Boolean) -> String?) {
    var isLoginMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        
        // --- THE NEW ANIMATED CANVAS LOGO ---
        AnimatedFitnessQuestLogo(modifier = Modifier.size(120.dp))
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("FitnessQuest", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text(
            text = if (isLoginMode) "Welcome back, Adventurer!" else "Begin your journey today", 
            style = MaterialTheme.typography.bodyLarge, 
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { 
                username = it
                errorMessage = null 
            },
            label = { Text("Adventurer Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it
                errorMessage = null 
            },
            label = { Text("Secret Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp)
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isLoginMode) "Don't have an account? Sign up here" else "Already have an account? Log in here",
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clickable {
                    isLoginMode = !isLoginMode 
                    errorMessage = null 
                }
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (username.isBlank()) {
                    errorMessage = "Please enter an Adventurer Name."
                } else {
                    val returnedError = onAuthenticate(username.trim(), !isLoginMode)
                    if (returnedError != null) {
                        errorMessage = returnedError
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp) 
        ) {
            Text(if (isLoginMode) "Login" else "Create Account", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ------------------------------------------------------------------------
// HOME SCREEN
// ------------------------------------------------------------------------
@Composable
fun HomeScreen(user: UserStats, onWorkout: () -> Unit) {
    val animatedXpProgress by animateFloatAsState(
        targetValue = user.currentXp.toFloat() / user.maxXp,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "xpAnimation"
    )
    
    val animatedStepProgress by animateFloatAsState(
        targetValue = user.steps.toFloat() / user.stepGoal,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "stepAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Level ${user.level} Ranger",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("XP: ${user.currentXp} / ${user.maxXp}", fontWeight = FontWeight.Medium, color = Color.Gray)
                    Text("${(animatedXpProgress * 100).toInt()}%", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedXpProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp) 
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text("Daily Quest", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(24.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 10.dp, 
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            CircularProgressIndicator(
                progress = { animatedStepProgress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 10.dp, 
                strokeCap = StrokeCap.Round,
                color = MaterialTheme.colorScheme.primary
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${user.steps}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "/ ${user.stepGoal} Steps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onWorkout,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp) 
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Adventure (Workout)", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ------------------------------------------------------------------------
// HISTORY SCREEN
// ------------------------------------------------------------------------
@Composable
fun HistoryScreen(stepData: List<Int>, xpData: List<Int>) {
    val scrollState = rememberScrollState()
    
    val daysOfWeek = remember {
        val tz = TimeZone.getTimeZone("Asia/Singapore")
        val format = SimpleDateFormat("EEE", Locale.getDefault())
        format.timeZone = tz
        val cal = Calendar.getInstance(tz)
        val daysList = mutableListOf<String>()
        for (i in 0..6) {
            daysList.add(0, format.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        daysList
    }

    val currentMonthYear = remember {
        val tz = TimeZone.getTimeZone("Asia/Singapore")
        val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        format.timeZone = tz
        format.format(Calendar.getInstance(tz).time)
    }
    
    val totalSteps = stepData.sum()
    val totalXp = xpData.sum()
    val stepDisplay = if (totalSteps >= 1000) "${totalSteps / 1000}.${(totalSteps % 1000) / 100}k" else "$totalSteps"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("Quest Logs", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text(currentMonthYear, style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard(title = "Total Steps", value = stepDisplay, icon = Icons.Default.Person, modifier = Modifier.weight(1f))
            SummaryCard(title = "XP Gained", value = "$totalXp", icon = Icons.Default.Star, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Weekly Steps", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                StepBarChart(data = stepData, labels = daysOfWeek)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("XP History", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                XpLineChart(data = xpData, labels = daysOfWeek)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SummaryCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

@Composable
fun StepBarChart(data: List<Int>, labels: List<String>) {
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) 
    val maxStep = data.maxOrNull()?.coerceAtLeast(1) ?: 1
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.animateTo(1f, tween(durationMillis = 1000, easing = FastOutSlowInEasing))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 8.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barSpacing = canvasWidth / data.size
            val barWidth = barSpacing * 0.5f 

            data.forEachIndexed { index, steps ->
                val normalizedHeight = (steps.toFloat() / maxStep) * canvasHeight * animationProgress.value
                val xPos = (index * barSpacing) + (barSpacing / 2) - (barWidth / 2)
                val yPos = canvasHeight - normalizedHeight

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x = xPos, y = yPos),
                    size = Size(width = barWidth, height = normalizedHeight),
                    cornerRadius = CornerRadius(8f, 8f) 
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            labels.forEach { label ->
                Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun XpLineChart(data: List<Int>, labels: List<String>) {
    val lineColor = MaterialTheme.colorScheme.secondary
    val maxXP = data.maxOrNull()?.coerceAtLeast(1) ?: 1
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.animateTo(1f, tween(durationMillis = 1000, easing = FastOutSlowInEasing))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 12.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val xSpacing = canvasWidth / (data.size - 1)
            val path = Path()
            val points = mutableListOf<Offset>()

            data.forEachIndexed { index, xp ->
                val xPos = index * xSpacing
                val animatedYPosition = canvasHeight - ((xp.toFloat() / maxXP) * canvasHeight * animationProgress.value)
                val point = Offset(x = xPos, y = animatedYPosition)
                points.add(point)
                
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }

            val fillPath = Path().apply {
                addPath(path)
                lineTo(points.last().x, canvasHeight)
                lineTo(points.first().x, canvasHeight)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.2f), Color.Transparent), 
                    startY = 0f,
                    endY = canvasHeight
                )
            )

            drawPath(path = path, color = lineColor, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)) 
            points.forEach { point ->
                drawCircle(color = Color.White, radius = 8f, center = point)
                drawCircle(color = lineColor, radius = 6f, center = point)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { label -> Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
        }
    }
}

// ------------------------------------------------------------------------
// LEADERBOARD SCREEN
// ------------------------------------------------------------------------
@Composable
fun LeaderboardScreen(currentUser: UserStats, allUsers: List<UserStats>) {
    val mockUsers = listOf(
        LeaderboardEntry(0, "DragonSlayer99", 15),
        LeaderboardEntry(0, "MarathonMike", 12),
        LeaderboardEntry(0, "IronLifter", 10),
        LeaderboardEntry(0, "SpeedyGonzales", 4),
        LeaderboardEntry(0, "NoobWarrior", 2)
    )

    val realUsers = allUsers.map { user ->
        LeaderboardEntry(
            rank = 0, 
            name = user.username,
            level = user.level,
            isCurrentUser = user.username == currentUser.username 
        )
    }

    val combinedLeaderboard = (mockUsers + realUsers)
        .sortedByDescending { it.level }
        .mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Global Rankings", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text("Top Adventurers by Level", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(combinedLeaderboard) { entry ->
                LeaderboardItem(entry)
            }
        }
    }
}

@Composable
fun LeaderboardItem(entry: LeaderboardEntry) {
    val isTop3 = entry.rank <= 3
    val containerColor = if (entry.isCurrentUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    val textColor = if (entry.isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (entry.isCurrentUser) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isTop3) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isTop3) Color.White else Color.DarkGray
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = textColor)
                if (entry.isCurrentUser) {
                    Text("That's you!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                text = "Lvl ${entry.level}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ------------------------------------------------------------------------
// ACHIEVEMENTS SCREEN
// ------------------------------------------------------------------------
@Composable
fun AchievementsScreen(currentUser: UserStats) {
    val achievements = getDynamicAchievements(currentUser)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your Trophies", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text("Unlock them all!", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(achievements) { achievement -> AchievementItem(achievement) }
        }
    }
}

@Composable
fun AchievementItem(achievement: Achievement) {
    val isUnlocked = achievement.isUnlocked
    val containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val iconTint = if (isUnlocked) MaterialTheme.colorScheme.primary else Color.LightGray
    val iconBg = if (isUnlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor), 
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, if (isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).background(iconBg, CircleShape), 
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = achievement.icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = achievement.title, style = MaterialTheme.typography.titleMedium, color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray)
                Text(text = achievement.description, style = MaterialTheme.typography.bodyMedium, color = if (isUnlocked) Color.Gray else Color.LightGray)
            }
            if (!isUnlocked) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.LightGray)
            }
        }
    }
}

// ------------------------------------------------------------------------
// PROFILE SCREEN
// ------------------------------------------------------------------------
@Composable
fun ProfileScreen(user: UserStats, onLogout: () -> Unit) {
    val unlockedAwardsCount = getDynamicAchievements(user).count { it.isUnlocked }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile Avatar",
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(user.username, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text("Member since Today", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatNode(value = "${user.level}", label = "Level")
                ProfileStatNode(value = "${user.steps}", label = "Total Steps")
                ProfileStatNode(value = "$unlockedAwardsCount", label = "Awards")
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            ProfileMenuItem(icon = Icons.Default.Settings, title = "Account Settings")
            ProfileMenuItem(icon = Icons.Default.Notifications, title = "Notifications")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Logout", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ProfileStatNode(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { /* TODO */ },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}