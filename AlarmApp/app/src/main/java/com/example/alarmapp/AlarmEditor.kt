package com.example.alarmapp

import AlarmManagerHelper
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.util.*


class AlarmEditor : AppCompatActivity() {

    private lateinit var saveButton : Button;
    private lateinit var cancelButton : Button;
    private lateinit var timePicker: TimePicker;
    private lateinit var alarmManager: AlarmManager
    private lateinit var checkBoxes: Array<CheckBox>
    private lateinit var checkedBoxes: Array<Boolean>
    private lateinit var labelText: EditText

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_editor)

        checkedBoxes = Array(7) { false }

      checkBoxes= arrayOf(
            findViewById<CheckBox>(R.id.monday),
            findViewById<CheckBox>(R.id.tuesday),
            findViewById<CheckBox>(R.id.wednsday),
            findViewById<CheckBox>(R.id.thursday),
            findViewById<CheckBox>(R.id.friday),
            findViewById<CheckBox>(R.id.saturday),
            findViewById<CheckBox>(R.id.sunday),
        )

        for((index,cb) in checkBoxes.withIndex()){
            cb.setOnCheckedChangeListener { buttonView, isChecked ->
                checkedBoxes[index]=isChecked
            }
        }

        labelText=findViewById(R.id.labelEditText)



        saveButton=findViewById(R.id.saveButton);
        cancelButton=findViewById(R.id.cancelButton);
        timePicker=findViewById(R.id.timePicker);
        onTimePicked();
        createNotificationChannel();

        //default time is 6:00
        timePicker.hour=6;
        timePicker.minute=0;

        //If clicked on an alarm object
        if(intent.hasExtra("key")){
            //timePicker.hour=intent.getIntExtra("key",6)
            val context: Context = this
            val alarmHelper = AlarmDatabaseHelper.getInstance(context);
            val id=intent.getIntExtra("key",6)
            val data=alarmHelper.retrieveData(id)
            val(toggle,time)=alarmHelper.retrieveData(id)
            val(weekDays,label)=alarmHelper.retrieveWeekDaysLabel(id)
            val cal=Calendar.getInstance()
            //Set time picker as the current alarm time that is stored in the db
            if (data != null) {
                cal.timeInMillis = time
                timePicker.hour=cal.get(Calendar.HOUR_OF_DAY)
                timePicker.minute=cal.get(Calendar.MINUTE)

            }

            if(weekDays!=null || weekDays!= ""){

                checkedBoxes=alarmHelper.convertStringToArray(weekDays)

                for((index,cb) in checkBoxes.withIndex()){
                    cb.isChecked=checkedBoxes[index]
                }
            }

            if(label!=null||label!=""){
                labelText.setText(label)
            }
            val maxLength = 20
            val filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))

            labelText.filters = filters
            if(labelText.text==null){
                labelText.setText("")
            }

        }




        cancelButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        saveButton.setOnClickListener {

            val am=AlarmManagerHelper.getInstance(this)

            val hour = timePicker.hour
            val minute = timePicker.minute


            val calendar = Calendar.getInstance()
            Calendar.HOUR_OF_DAY
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            //calendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)

            val currentTime = Calendar.getInstance().timeInMillis

            // Check if the selected time is in the past
            if (calendar.timeInMillis < currentTime) {
                // Add one day to the selected time
                calendar.add(Calendar.DATE, 1)

            }



            //If a box is checked
            val allDaysOff=am.allDaysOfWeekOff(checkedBoxes)
            //If a day of week check box checked
            if(!allDaysOff){
                calendar.timeInMillis= am.dayOfWeekInMillis(checkedBoxes,calendar.timeInMillis)
            }


            val t = am.convertTimeInMillisToDate(calendar.timeInMillis)
            Log.d("Time","$t ")
            for((index,cb) in checkedBoxes.withIndex()){
                Log.d("Time2","$cb + $index")
            }





            // Get database
            val context: Context = this
            val alarmHelper = AlarmDatabaseHelper.getInstance(context);

            val db = alarmHelper.writableDatabase

            //A new added alarm
            if(!intent.hasExtra("key")) {
                // Add to database

                val values = ContentValues().apply {
                    //put("_id", count + 1)
                    put("time", calendar.timeInMillis)
                    put("toggle",1)
                    put("weekdays",alarmHelper.convertArrayToString(checkedBoxes))
                    put("label",labelText.text.toString())
                }
                val id=db.insert("alarm", null, values)


                Toast.makeText(this, "ID is $id", Toast.LENGTH_SHORT).show()
                db.close()



                // Get the alarm manager
                alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

                // Create a pending intent for the alarm
                val intent = Intent(this, AlarmReceiver::class.java)
                intent.putExtra("key",id.toInt())
                intent.action = "ALARM_SET"
                val pendingIntent = PendingIntent.getBroadcast(this, id.toInt(), intent, FLAG_IMMUTABLE)
                // use count as requestCode so the pendingIntent can be retrieved later.


                // Set the alarm
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(calendar.timeInMillis,pendingIntent),pendingIntent)
                //alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                //alarmManager.set(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,pendingIntent)
                // Show a toast message to confirm the save
                Toast.makeText(this, "Alarm set!$hour$minute", Toast.LENGTH_SHORT).show()
                DataHolder.getInstance().data = hour.toString();



            }
            //Editing already existing alarm
            else{
                val id=intent.getIntExtra("key",alarmHelper.getAlarmCount())

                //Boxes are checked

                alarmHelper.updateDaysOfWeek(id,alarmHelper.convertArrayToString(checkedBoxes))
                alarmHelper.updateLabel(id,labelText.text.toString())
                if(!allDaysOff){
                    calendar.timeInMillis= am.dayOfWeekInMillis(checkedBoxes,calendar.timeInMillis)
                }
                val intent = Intent(this, AlarmReceiver::class.java)
                intent.putExtra("key",id)
                intent.action = "ALARM_SET"
                val pendingIntent = PendingIntent.getBroadcast(this, id, intent, FLAG_IMMUTABLE)
                val am=AlarmManagerHelper.getInstance(this).setAlarm(id,calendar.timeInMillis,pendingIntent,"")
            }

            val intent2 = Intent(this, MainActivity::class.java)
            intent2.putExtra("key", "$hour:$minute");
            startActivity(intent2)
        }

    }
    fun onTimePicked(){
        timePicker.setOnTimeChangedListener { view, hourOfDay, minute ->
            // Do something here when the time is changed
            //hour = hourOfDay;
           // min=minute;
        }

    }
    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create a notification channel
            val channel = NotificationChannel("alarm_channel", "Alarms", NotificationManager.IMPORTANCE_HIGH)
            //channel.enableVibration(true)
            //channel.vibration = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)

            // Get the NotificationManager
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

    }

}