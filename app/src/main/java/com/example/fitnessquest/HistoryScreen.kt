package com.example.fitnessquest

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fitnessquest.data.UserStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun HistoryScreen(user: UserStats) {
    val scrollState = rememberScrollState()

    // Use data directly from the UserStats object
    val stepData = user.stepHistory
    val xpData = user.xpHistory

    val daysOfWeek = remember {
        val tz = TimeZone.getTimeZone("Asia/Singapore")
        val format = SimpleDateFormat("EEE", Locale.getDefault()).apply { timeZone = tz }
        val cal = Calendar.getInstance(tz)
        val daysList = mutableListOf<String>()
        for (i in 0..6) { daysList.add(0, format.format(cal.time)); cal.add(Calendar.DAY_OF_YEAR, -1) }
        daysList
    }

    val currentMonthYear = remember {
        val tz = TimeZone.getTimeZone("Asia/Singapore")
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).apply { timeZone = tz }.format(Calendar.getInstance(tz).time)
    }

    // Use Lifetime stats for the summary cards
    val totalSteps = user.steps
    val currentLevel = user.level

    val stepDisplay = if (totalSteps >= 1000) "${totalSteps / 1000}.${(totalSteps % 1000) / 100}k" else "$totalSteps"

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp)) {
        Text("Quest Logs", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text(currentMonthYear, style = MaterialTheme.typography.titleMedium, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        // Updated Summary Cards to show Lifetime Steps and Current Level
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SummaryCard("Lifetime Steps", stepDisplay, Icons.Default.Person, Modifier.weight(1f))
            SummaryCard("Current Level", "$currentLevel", Icons.Default.Star, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Weekly Steps", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                StepBarChart(stepData, daysOfWeek)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("XP History", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                // Note: In our current logic, XP history roughly mirrors steps, but we display it here for consistency
                XpLineChart(xpData, daysOfWeek)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SummaryCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

@Composable
fun StepBarChart(data: List<Int>, labels: List<String>) {
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    val maxStep = data.maxOrNull()?.coerceAtLeast(1) ?: 1
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) { animationProgress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 8.dp)) {
            val w = size.width; val h = size.height
            val barSpacing = w / data.size
            val barWidth = barSpacing * 0.5f
            data.forEachIndexed { index, steps ->
                val nHeight = (steps.toFloat() / maxStep) * h * animationProgress.value
                val xPos = (index * barSpacing) + (barSpacing / 2) - (barWidth / 2)
                drawRoundRect(barColor, Offset(xPos, h - nHeight), Size(barWidth, nHeight), CornerRadius(8f, 8f))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            labels.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
fun XpLineChart(data: List<Int>, labels: List<String>) {
    val lineColor = MaterialTheme.colorScheme.secondary
    val maxXP = data.maxOrNull()?.coerceAtLeast(1) ?: 1
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) { animationProgress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 12.dp)) {
            val w = size.width; val h = size.height
            val xSpacing = w / (data.size - 1)
            val path = Path()
            val points = mutableListOf<Offset>()

            data.forEachIndexed { index, xp ->
                val xPos = index * xSpacing
                val yPos = h - ((xp.toFloat() / maxXP) * h * animationProgress.value)
                val point = Offset(xPos, yPos)
                points.add(point)
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            val fillPath = Path().apply { addPath(path); lineTo(points.last().x, h); lineTo(points.first().x, h); close() }
            drawPath(fillPath, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.2f), Color.Transparent), 0f, h))
            drawPath(path, lineColor, style = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            points.forEach { p ->
                drawCircle(Color.White, 8f, p)
                drawCircle(lineColor, 6f, p)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
        }
    }
}