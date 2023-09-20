package com.example.alarmapp

import android.content.*
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat

class DismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Handle dismiss action here

        val stopIntent = Intent(context, AlarmForegroundService::class.java)
        stopIntent.action = "STOP_ALARM"
        context.startService(stopIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        val id =intent.getIntExtra("key",2)
        notificationManager.cancel(id)
    }
}

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Handle snooze action here
        val snoozeIntent = Intent(context, AlarmForegroundService::class.java)
        snoozeIntent.action = "SNOOZE_ALARM"
        val id =intent.getIntExtra("key",2)
        snoozeIntent.putExtra("key",id)
        Log.d("Not","SnoozeReceiver $id  ")
        context.startService(snoozeIntent)
        // Implement your snooze logic
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(1)
    }
}
