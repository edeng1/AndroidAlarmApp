package com.example.alarmapp
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat


class AlarmForegroundService : Service() {
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate() {
        super.onCreate()
        // Initialize and prepare MediaPlayer for audio playback

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("Media",intent?.action.toString())

        when (intent?.action) {
            "START_ALARM" -> {

                startMediaPlayer()
                val NOTIFICATION_ID=intent.getIntExtra("key",1)
                startForeground(NOTIFICATION_ID, createNotification())
            }
            "STOP_ALARM" -> {
                stopMediaPlayer()
                // Optionally, stop the service if no other ongoing tasks
                stopSelf()
            }
            // Other actions...
        }

        return START_STICKY
    }

    private fun createNotification(): Notification {
        //val contentView = RemoteViews(packageName, R.layout.notification_layout)
        //contentView.setOnClickPendingIntent(R.id.dismissButton, dismissPendingIntent)
        //contentView.setOnClickPendingIntent(R.id.snoozeButton, snoozePendingIntent)

        val dismissIntent = Intent(this, DismissReceiver::class.java)
        val snoozeIntent = Intent(this, SnoozeReceiver::class.java)

        val dismissPendingIntent = PendingIntent.getBroadcast(this, 0, dismissIntent, PendingIntent.FLAG_IMMUTABLE )
        val snoozePendingIntent = PendingIntent.getBroadcast(this, 0, snoozeIntent, PendingIntent.FLAG_IMMUTABLE )



        val notificationBuilder = NotificationCompat.Builder(this, "alarm_channel")
            .setSmallIcon(com.google.android.material.R.drawable.ic_keyboard_black_24dp)
            .setContentTitle("Alarm ringing")
            .setContentText("Alarm has gone off")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)

        notificationBuilder.addAction(com.google.android.material.R.drawable.mtrl_ic_cancel, "Dismiss", dismissPendingIntent)
        notificationBuilder.addAction(com.google.android.material.R.drawable.mtrl_switch_thumb, "Snooze", snoozePendingIntent)


        return notificationBuilder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    fun startMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound_1)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    fun stopMediaPlayer() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
    }

}
