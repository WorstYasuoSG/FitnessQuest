package com.example.fitnessquest

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnessquest.data.UserStats

@Composable
fun HomeScreen(user: UserStats, onWorkout: () -> Unit) {
    val animatedXpProgress by animateFloatAsState(targetValue = user.currentXp.toFloat() / user.maxXp, animationSpec = tween(800, easing = FastOutSlowInEasing))
    val animatedStepProgress by animateFloatAsState(targetValue = user.steps.toFloat() / user.stepGoal, animationSpec = tween(800, easing = FastOutSlowInEasing))

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Level ${user.level} Ranger", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("XP: ${user.currentXp} / ${user.maxXp}", fontWeight = FontWeight.Medium, color = Color.Gray)
                    Text("${(animatedXpProgress * 100).toInt()}%", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { animatedXpProgress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        Text("Daily Quest", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(24.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), strokeWidth = 10.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            CircularProgressIndicator(progress = { animatedStepProgress }, modifier = Modifier.fillMaxSize(), strokeWidth = 10.dp, strokeCap = StrokeCap.Round, color = MaterialTheme.colorScheme.primary)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${user.steps}", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Text("/ ${user.stepGoal} Steps", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        
        // This button simulates a workout for testing purposes without needing to walk around
        Button(
            onClick = onWorkout, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp), elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simulate Steps (Testing)", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}