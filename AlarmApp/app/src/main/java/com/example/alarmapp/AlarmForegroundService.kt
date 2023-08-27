package com.example.alarmapp
import AlarmManagerHelper
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*


class AlarmForegroundService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var countdownTimer: CountDownTimer
    private lateinit var db: AlarmDatabaseHelper
    private lateinit var am: AlarmManagerHelper
    var toneString: String=""
    override fun onCreate() {
        super.onCreate()
        // Initialize and prepare MediaPlayer for audio playback
        db= AlarmDatabaseHelper.getInstance(this)
        am=AlarmManagerHelper.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("Media",intent?.action.toString())

        when (intent?.action) {
            "START_ALARM" -> {

                val NOTIFICATION_ID=intent.getIntExtra("key",1)

                //isAlarmPlaying needed so multiple alarms dont play at once
                DataHolder.getInstance().isAlarmPlaying=true
                val shutOffTime= intent.getLongExtra("shutoff",5000L)
                startForeground(NOTIFICATION_ID, createNotification(NOTIFICATION_ID,shutOffTime))
                startMediaPlayer(NOTIFICATION_ID)
            }
            "STOP_ALARM" -> {
                stopMediaPlayer()
                // Optionally, stop the service if no other ongoing tasks
                DataHolder.getInstance().isAlarmPlaying=false
                stopSelf()
            }
            // Other actions...
            "SNOOZE_ALARM" -> {
                snoozeMediaPlayer()
                val NOTIFICATION_ID=intent.getIntExtra("key",1)
                val notificationManager = NotificationManagerCompat.from(this)
                    //DataHolder.getInstance().isAlarmPlaying=false
                notificationManager.cancel(1)
                startForeground(NOTIFICATION_ID, createNotificationSnooze(NOTIFICATION_ID))
                // Optionally, stop the service if no other ongoing tasks

            }

        }

        return START_STICKY
    }

    private fun createNotification(id: Int,shutOffTime: Long): Notification {
            val contentView = RemoteViews(packageName, R.layout.notification_layout)


            val dismissIntent = Intent(this, DismissReceiver::class.java)
            val snoozeIntent = Intent(this, SnoozeReceiver::class.java)
            Log.d("Not","createNotification $id  ")
            snoozeIntent.putExtra("key",id)

            val dismissPendingIntent = PendingIntent.getBroadcast(this, 0, dismissIntent, PendingIntent.FLAG_IMMUTABLE )
            val snoozePendingIntent = PendingIntent.getBroadcast(this, 0, snoozeIntent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)


            val notificationBuilder = NotificationCompat.Builder(this, "alarm_channel4")
                .setSmallIcon(com.google.android.material.R.drawable.ic_keyboard_black_24dp)
                .setCustomContentView(contentView) // Set the updated custom layout here
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setSound(null)
                .setContentTitle("Alarm")
                .setContentText("Alarm went off!")



            notificationBuilder.setSilent(false)

            notificationBuilder.addAction(com.google.android.material.R.drawable.mtrl_ic_cancel, "Dismiss", dismissPendingIntent)
            notificationBuilder.addAction(com.google.android.material.R.drawable.mtrl_switch_thumb, "Snooze", snoozePendingIntent)
            val notificationManager = NotificationManagerCompat.from(this)

        Log.d("imp",notificationManager.importance.toString())
            notificationManager.notify(id, notificationBuilder.build())
           val(tones,shutofftime) = db.retrieveTonesShutOffTime(id)
            toneString=tones
            val totalTimeMillis: Long = 5000
            val intervalMillis: Long = 1000

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "MyApp::MyWakelockTag")
        wakeLock.acquire(shutofftime)

        // Start the intent here

        // -1L means forever
        if(shutofftime!=-1L){
            countdownTimer = object : CountDownTimer(shutofftime, intervalMillis) {
                override fun onTick(millisUntilFinished: Long) {

                    val formattedTime=formatMillisecondsToTime(millisUntilFinished)
                    val remainingTimeSeconds = millisUntilFinished / 1000

                    //contentView.setTextViewText(R.id.notificationTextView, "Time remaining: $remainingTimeSeconds s")
                    notificationBuilder.setContentText("Time remaining: $formattedTime s")
                    notificationManager.notify(id, notificationBuilder.build())
                }

                override fun onFinish() {
                    DataHolder.getInstance().isAlarmPlaying=false
                    mediaPlayer.stop()
                    mediaPlayer.release()
                    wakeLock.release() // Release the wakelock after the intent is started
                    contentView.setTextViewText(R.id.notificationTextView, "Time's up!")
                    notificationManager.notify(id, notificationBuilder.build())

                }
            }
            countdownTimer.start()

        }

            return notificationBuilder.build()
    }

    private fun createNotificationSnooze(id: Int): Notification {
        val contentView = RemoteViews(packageName, R.layout.notification_layout)


        val dismissIntent = Intent(this, DismissReceiver::class.java)

        val dismissPendingIntent = PendingIntent.getBroadcast(this, 0, dismissIntent, PendingIntent.FLAG_IMMUTABLE )



        val notificationBuilder = NotificationCompat.Builder(this, "alarm_channel4")
            .setSmallIcon(com.google.android.material.R.drawable.ic_keyboard_black_24dp)
            .setCustomContentView(contentView) // Set the updated custom layout here
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setSound(null)
            .setContentTitle("Snooze")
            .setContentText("Snoozing")



        notificationBuilder.setSilent(false)

        notificationBuilder.addAction(com.google.android.material.R.drawable.mtrl_ic_cancel, "Dismiss", dismissPendingIntent)
        val notificationManager = NotificationManagerCompat.from(this)

        Log.d("imp",notificationManager.importance.toString())
        notificationManager.notify(id, notificationBuilder.build())

        val snoozeTimeMillis: Long = 10000
        val intervalMillis: Long = 1000
        val context: Context = this
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "MyApp::MyWakelockTag")
        wakeLock.acquire(snoozeTimeMillis)

        // Start the intent here

            countdownTimer = object : CountDownTimer(snoozeTimeMillis, intervalMillis) {
                override fun onTick(millisUntilFinished: Long) {

                    val formattedTime=formatMillisecondsToTime(millisUntilFinished)
                    val remainingTimeSeconds = millisUntilFinished / 1000

                    //contentView.setTextViewText(R.id.notificationTextView, "Time remaining: $remainingTimeSeconds s")
                    notificationBuilder.setContentText("Snooze time: $formattedTime s")
                    notificationManager.notify(id, notificationBuilder.build())
                }

                override fun onFinish() {
                    val snoozeIntent = Intent(context, AlarmForegroundService::class.java)
                    snoozeIntent.putExtra("key",id)
                    snoozeIntent.action = "START_ALARM"

                    context.startService(snoozeIntent)

                }
            }
            countdownTimer.start()



        return notificationBuilder.build()
    }


    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized)
            mediaPlayer.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    fun startMediaPlayer(id: Int) {

        if(toneString==""){
            toneString="alarm_sound_1"
        }
        val sound=getRawResourceByName(this,toneString)

        mediaPlayer = MediaPlayer.create(this, sound)
        mediaPlayer.setVolume(1f,1f)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    fun stopMediaPlayer() {
        if (::mediaPlayer.isInitialized) {

            if(mediaPlayer.isPlaying){
                mediaPlayer.stop()
                mediaPlayer.release()
            }

        }
        if(::countdownTimer.isInitialized)
            countdownTimer.cancel()
    }
    fun snoozeMediaPlayer() {
        if (::mediaPlayer.isInitialized) {

            if(mediaPlayer.isPlaying){
                mediaPlayer.stop()
            }

        }
        if(::countdownTimer.isInitialized)
            countdownTimer.cancel()
    }




    fun formatMillisecondsToTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun getRawResourceByName(context: Context, resourceName: String): Int {
        val resources: Resources = context.resources
        return resources.getIdentifier(resourceName, "raw", context.packageName)

    }


}
