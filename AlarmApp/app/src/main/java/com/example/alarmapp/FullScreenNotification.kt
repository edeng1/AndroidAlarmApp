package com.example.alarmapp

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*

class FullScreenNotification : AppCompatActivity() {
    private lateinit var countdownTimer: CountDownTimer
    private lateinit var timeRemainingTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var clockTextView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        setContentView(R.layout.notification_layout)

        dateTextView=findViewById(R.id.dateTextView1)
        clockTextView=findViewById(R.id.clockTextView)
        timeRemainingTextView=findViewById(R.id.notificationTextView)
        timeTillDismiss()
        val buttonDismiss = findViewById<Button>(R.id.dismissButton)
        val buttonSnooze = findViewById<Button>(R.id.snoozeButton)

        buttonDismiss.setOnClickListener(View.OnClickListener {
            val dismissIntent = Intent(this, DismissReceiver::class.java)


            val dismissPendingIntent = PendingIntent.getBroadcast(this, 0, dismissIntent, PendingIntent.FLAG_IMMUTABLE )
            try {
                // Start the PendingIntent when the button is clicked
                dismissPendingIntent.send()
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }
            finish() // Close the activity
        })

        buttonSnooze.setOnClickListener(View.OnClickListener {

            val snoozeIntent = Intent(this, SnoozeReceiver::class.java)
            val id = intent.getIntExtra("key",0)
            snoozeIntent.putExtra("key",id)


            val snoozePendingIntent = PendingIntent.getBroadcast(this, 0, snoozeIntent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            try {
                // Start the PendingIntent when the button is clicked
                snoozePendingIntent.send()
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }

            val snoozeTimeMillis: Long = 10000
            val intervalMillis: Long = 1000

            countdownTimer.cancel()
            countdownTimer = object : CountDownTimer(snoozeTimeMillis, intervalMillis) {
                override fun onTick(millisUntilFinished: Long) {

                    val formattedTime = formatMillisecondsToTime(millisUntilFinished)
                    val remainingTimeSeconds = millisUntilFinished / 1000

                    val currentTime = Calendar.getInstance().time

                    val sdfT = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val sdfD = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

                    val clockTime = sdfT.format(currentTime)
                    val dateTime=sdfD.format(currentTime)

                    dateTextView.text=dateTime
                    clockTextView.text=clockTime


                    timeRemainingTextView.text="Snoozing: "+formattedTime.toString()

                }

                override fun onFinish() {

                    timeTillDismiss()

                }

            }
            countdownTimer.start()



             // Close the activity
        })
    }

    private fun timeTillDismiss() {
        var shutofftime=intent.getLongExtra("shutofftime",-1L)
        val intervalMillis: Long = 1000
        var isShutOffTime:Boolean=true
        Log.d("shut",shutofftime.toString())
        if (shutofftime == -1L) {
            shutofftime=Long.MAX_VALUE
            isShutOffTime=false
        }

            countdownTimer = object : CountDownTimer(shutofftime, intervalMillis) {
                override fun onTick(millisUntilFinished: Long) {

                    val formattedTime = formatMillisecondsToTime(millisUntilFinished)
                    val remainingTimeSeconds = millisUntilFinished / 1000

                    val currentTime = Calendar.getInstance().time

                    val sdfT = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val sdfD = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

                    val clockTime = sdfT.format(currentTime)
                    val dateTime=sdfD.format(currentTime)

                    dateTextView.text=dateTime
                    clockTextView.text=clockTime

                    if(isShutOffTime)
                        timeRemainingTextView.text="Dismissing in "+formattedTime.toString()

                }

                override fun onFinish() {
                    timeRemainingTextView.text="Dismissed!"
                    finish()

                }

            }
            countdownTimer.start()
        }



    fun formatMillisecondsToTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }
}
