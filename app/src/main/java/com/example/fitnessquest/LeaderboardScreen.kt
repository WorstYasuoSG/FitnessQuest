package com.example.fitnessquest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitnessquest.data.LeaderboardEntry
import com.example.fitnessquest.data.UserStats

@Composable
fun LeaderboardScreen(currentUser: UserStats, allUsers: List<UserStats>) {
    val mockUsers = listOf(LeaderboardEntry(0, "DragonSlayer99", 15), LeaderboardEntry(0, "MarathonMike", 12), LeaderboardEntry(0, "IronLifter", 10), LeaderboardEntry(0, "SpeedyGonzales", 4), LeaderboardEntry(0, "NoobWarrior", 2))
    val realUsers = allUsers.map { LeaderboardEntry(0, it.username, it.level, it.username == currentUser.username) }
    val combinedLeaderboard = (mockUsers + realUsers).sortedByDescending { it.level }.mapIndexed { index, entry -> entry.copy(rank = index + 1) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Global Rankings", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text("Top Adventurers by Level", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(combinedLeaderboard) { entry -> LeaderboardItem(entry) }
        }
    }
}

@Composable
fun LeaderboardItem(entry: LeaderboardEntry) {
    val isTop3 = entry.rank <= 3
    val containerColor = if (entry.isCurrentUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    val textColor = if (entry.isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor), shape = RoundedCornerShape(16.dp),
        border = if (entry.isCurrentUser) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(if (isTop3) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                Text("#${entry.rank}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = if (isTop3) Color.White else Color.DarkGray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = textColor)
                if (entry.isCurrentUser) Text("That's you!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Text("Lvl ${entry.level}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        }
    }
}