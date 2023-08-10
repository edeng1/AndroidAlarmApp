package com.example.alarmapp

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_ALARM_COUNT
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Get the NotificationManager
        val notificationManager = NotificationManagerCompat.from(context)

        // Display the notification
        notificationManager.notify(0, builder.build())

        //Reschedule
        if(intent.hasExtra("key")){
            val requestCode=intent.getIntExtra("key",-1)
            val db=AlarmDatabaseHelper.getInstance(context)
            val am=AlarmManagerHelper.getInstance(context)
            val (toggle,time)=db.retrieveData(requestCode)
            if(toggle==1){
                intent.putExtra("key",requestCode)
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
            else{

            }
        }
        else{
            Toast.makeText(context,"No intent key, not rescheduling alarm",Toast.LENGTH_SHORT).show()
        }



    }


}

