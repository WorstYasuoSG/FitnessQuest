package com.example.fitnessquest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fitnessquest.data.Achievement
import com.example.fitnessquest.data.UserStats
import com.example.fitnessquest.data.getDynamicAchievements

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
        colors = CardDefaults.cardColors(containerColor = containerColor), shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).background(iconBg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(achievement.icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(achievement.title, style = MaterialTheme.typography.titleMedium, color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray)
                Text(achievement.description, style = MaterialTheme.typography.bodyMedium, color = if (isUnlocked) Color.Gray else Color.LightGray)
            }
            if (!isUnlocked) Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.LightGray)
        }
    }
}