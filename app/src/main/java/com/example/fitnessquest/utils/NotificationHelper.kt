package com.example.fitnessquest.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

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