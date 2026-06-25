package com.example.ui.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object PushNotificationManager {
    private const val CHANNEL_ID = "invexx_push_notifications"
    
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Invexx Alerts & Updates"
            val descriptionText = "Important account transactions, updates and alerts from Invexx"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, title: String, text: String, id: Int = (100..9999).random()) {
        createChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_notification)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.resources, com.example.R.drawable.app_icon))
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(id, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted or SecurityException
        }
    }
}
