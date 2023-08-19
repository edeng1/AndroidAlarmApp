package com.example.alarmapp

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_ALARM_COUNT
import android.media.MediaPlayer
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Perform the desired action when the alarm goes off
        Toast.makeText(context, "Alarm went off!", Toast.LENGTH_SHORT).show()
        val i=Intent(context,MainActivity::class.java)
        intent!!.flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent= PendingIntent.getActivity(context,0,i,FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context!!, "alarm_channel")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Alarm go boom boom")
            .setContentText("Your alarm has gone off.")
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Get the NotificationManager
        val notificationManager = NotificationManagerCompat.from(context)

        // Display the notification
        //notificationManager.notify(0, builder.build())

        if (intent.action == "ALARM_SET") {
            val serviceIntent = Intent(context, AlarmForegroundService::class.java)
            serviceIntent.action = "START_ALARM"
            val id=intent.getIntExtra("key",-1)
            serviceIntent.putExtra("key",id)
            ContextCompat.startForegroundService(context, serviceIntent)
        }



        if (context != null) {
            //val mediaPlayer = MediaPlayer.create(context, R.raw.alarm_sound_1)
            //mediaPlayer.isLooping=true
            //mediaPlayer.start()

        }



        //Reschedule the alarm right after it goes off.
        if(intent.hasExtra("key")){
            val requestCode=intent.getIntExtra("key",-1)
            val db=AlarmDatabaseHelper.getInstance(context)
            val am=AlarmManagerHelper.getInstance(context)
            val (toggle,time)=db.retrieveData(requestCode)
            val(tones,shutoff)=db.retrieveTonesShutOffTime(requestCode)
            if(toggle==1){
                intent.putExtra("key",requestCode)
                intent.putExtra("shutoff",shutoff)
                intent.action = "ALARM_SET"
                val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, FLAG_IMMUTABLE)
                val calendar = Calendar.getInstance()
                calendar.timeInMillis=time
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                var newTime=calendar.timeInMillis

                //CHeck if week day boxes are checked
                val (week,label)= db.retrieveWeekDaysLabel(requestCode)
                val checkedBoxes=db.convertStringToArray((week))
                if(!am.allDaysOfWeekOff(checkedBoxes)){
                    newTime=am.dayOfWeekInMillis(checkedBoxes,calendar.timeInMillis)
                }


                am.setAlarm(requestCode,newTime,pendingIntent,"")
            }

        }
        else{
            Toast.makeText(context,"No intent key, not rescheduling alarm",Toast.LENGTH_SHORT).show()
        }



    }


}
/*
// Perform the desired action when the alarm goes off
        Toast.makeText(context, "Alarm went off!", Toast.LENGTH_SHORT).show()
        val i=Intent(context,MainActivity::class.java)
        intent!!.flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent= PendingIntent.getActivity(context,0,i,FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context!!, "alarm_channel")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Alarm go boom boom")
            .setContentText("Your alarm has gone off.")
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Get the NotificationManager
        val notificationManager = NotificationManagerCompat.from(context)

        // Display the notification
        notificationManager.notify(0, builder.build())
 */

