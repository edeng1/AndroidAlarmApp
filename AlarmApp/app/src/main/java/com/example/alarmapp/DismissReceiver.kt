package com.example.alarmapp

import android.content.*
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat

class DismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Handle dismiss action here

        val stopIntent = Intent(context, AlarmForegroundService::class.java)
        stopIntent.action = "STOP_ALARM"
        context.startService(stopIntent)

        val notificationManager = NotificationManagerCompat.from(context)

        notificationManager.cancel(1)
    }
}

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Handle snooze action here
        // Implement your snooze logic
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(1)
    }
}
