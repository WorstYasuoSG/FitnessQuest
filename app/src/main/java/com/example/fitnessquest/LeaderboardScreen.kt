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
import com.example.fitnessquest.data.LeaderboardEntry   // UI model (rank, displayName, level, isCurrentUser)
import com.example.fitnessquest.data.LeaderboardRow     // Firestore model (displayName, totalSteps)
import com.example.fitnessquest.data.UserStats
import com.example.fitnessquest.data.getLevelData
@Composable
fun LeaderboardScreen(
    currentUser: UserStats,
    leaderboard: List<LeaderboardRow>,
    isLoading: Boolean
) {
    // Optional mock rows (still based on steps -> converted to level)
    val mockUsers = listOf(
        LeaderboardRow(displayName = "DragonSlayer99", totalSteps = 15000),
        LeaderboardRow(displayName = "MarathonMike", totalSteps = 12000),
        LeaderboardRow(displayName = "IronLifter", totalSteps = 10000),
        LeaderboardRow(displayName = "SpeedyGonzales", totalSteps = 4000),
        LeaderboardRow(displayName = "NoobWarrior", totalSteps = 2000)
    )

    // Convert Firestore rows -> UI entries (rank computed later)
    val realEntries: List<LeaderboardEntry> = leaderboard.map { row ->
        val name = row.displayName.ifBlank { "Unknown" }
        val level = getLevelData(row.totalSteps.toInt()).first
        LeaderboardEntry(
            rank = 0,
            displayName = name,
            level = level,
            isCurrentUser = (name == currentUser.username) // you can also compare against displayName if you add it to UserStats
        )
    }

    val mockEntries: List<LeaderboardEntry> = mockUsers.map { row ->
        val name = row.displayName.ifBlank { "Unknown" }
        val level = getLevelData(row.totalSteps.toInt()).first
        LeaderboardEntry(
            rank = 0,
            displayName = name,
            level = level,
            isCurrentUser = (name == currentUser.username)
        )
    }

    // Same behavior as your previous UI: sort by level desc, assign ranks
    val combinedLeaderboard = (
            if (leaderboard.isEmpty()) mockEntries else realEntries
            )
        .sortedByDescending { it.level }
        .mapIndexed { index, entry -> entry.copy(rank = index + 1) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Global Rankings", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text("Top Adventurers by Level", style = MaterialTheme.typography.titleMedium, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Loading leaderboard…", color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

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
    val containerColor =
        if (entry.isCurrentUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surface
    val textColor =
        if (entry.isCurrentUser) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = if (entry.isCurrentUser)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isTop3) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "#${entry.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isTop3) Color.White else Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(entry.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = textColor)
                if (entry.isCurrentUser) {
                    Text("That's you!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Keep the exact same field shown as before
            Text(
                "Lvl ${entry.level}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}