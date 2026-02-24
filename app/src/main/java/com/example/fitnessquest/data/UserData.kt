package com.example.fitnessquest.data

import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.pow

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
data class FirestoreUserStats(
    val displayName: String = "",
    val totalSteps: Long = 0,
    val level: Int = 1,
    val currentXp: Int = 0,
    val workoutsCompleted: Int = 0
)
data class Achievement(val title: String, val description: String, val isUnlocked: Boolean, val icon: ImageVector)
data class LeaderboardEntry(val rank: Int, val displayName: String, val level: Int, val isCurrentUser: Boolean = false)
data class LeaderboardRow(
    val displayName: String = "",
    val totalSteps: Long = 0
)
fun getLevelData(totalSteps: Int): Triple<Int, Int, Int> {
    val baseXp = 1500.0
    val exponent = 1.8

    var currentLevel = 1
    var xpForCurrentLevel = 0
    
    // Calculate total XP required to hit level 2
    var xpForNextLevel = (baseXp * currentLevel.toDouble().pow(exponent)).toInt()

    // Iteratively scale the requirements until we find the player's true level
    while (totalSteps >= xpForNextLevel) {
        currentLevel++
        xpForCurrentLevel = xpForNextLevel
        xpForNextLevel = (baseXp * currentLevel.toDouble().pow(exponent)).toInt()
    }

    // currentXpProgress: How far they are into the CURRENT level
    val currentXpProgress = totalSteps - xpForCurrentLevel
    
    // xpNeededForNextLevel: The gap between the current level floor and the next level ceiling
    val xpNeededForNextLevel = xpForNextLevel - xpForCurrentLevel

    return Triple(currentLevel, currentXpProgress, xpNeededForNextLevel)
}

fun getDynamicAchievements(user: UserStats): List<Achievement> {
    return listOf(
        Achievement("First Steps", "Walk 1,500 steps", user.steps >= 1500, Icons.Default.Person),
        Achievement("Warrior", "Complete 10 workouts", user.workoutsCompleted >= 10, Icons.Default.Star),
        Achievement("Marathoner", "Walk 42,000 steps", user.steps >= 42000, Icons.Default.Home),
        Achievement("Dedicated", "Reach Level 10", user.level >= 10, Icons.Default.Star),
        Achievement("Grandmaster", "Reach Level 50", user.level >= 50, Icons.Default.Star)
    )
}

fun UserStats.toCsv() = "$username;$level;$currentXp;$maxXp;$steps;$stepGoal;${stepHistory.joinToString(",")};${xpHistory.joinToString(",")};$workoutsCompleted"

fun userStatsFromCsv(csv: String): UserStats? {
    return try {
        val p = csv.split(";")
        UserStats(p[0], p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5].toInt(), p[6].split(",").map{it.toInt()}, p[7].split(",").map{it.toInt()}, if(p.size>=9) p[8].toInt() else 0)
    } catch (e: Exception) { null }
}

class StorageManager(private val prefs: SharedPreferences) {
    fun saveUser(user: UserStats) {
        prefs.edit().putString("user_${user.username}", user.toCsv()).apply()
        val users = getUsersList().toMutableSet().apply { add(user.username) }
        prefs.edit().putStringSet("all_users", users).apply()
    }
    fun getUser(username: String): UserStats? = prefs.getString("user_$username", null)?.let { userStatsFromCsv(it) }
    fun getAllUsers() = getUsersList().mapNotNull { getUser(it) }
    private fun getUsersList() = prefs.getStringSet("all_users", emptySet()) ?: emptySet()
    fun saveLoggedInUser(username: String?) = if (username == null) prefs.edit().remove("logged_in_user").apply() else prefs.edit().putString("logged_in_user", username).apply()
    fun getLoggedInUser() = prefs.getString("logged_in_user", null)
}