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
import android.content.res.Resources
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.iterator
import androidx.core.view.size
import java.util.*


class AlarmEditor : AppCompatActivity() {

    private lateinit var saveButton : Button;
    private lateinit var cancelButton : Button;
    private lateinit var timePicker: TimePicker;
    private lateinit var alarmManager: AlarmManager
    private lateinit var checkBoxes: Array<CheckBox>
    private lateinit var checkedBoxes: Array<Boolean>
    private lateinit var labelText: EditText
    private lateinit var shutOffSpinner: Spinner
    private lateinit var tonesSpinner: Spinner
    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var secondPicker: NumberPicker
    private var selectedSpinnerItem: String = ""
    private var shutOffisForever: Boolean=true
    private var shutOffTimeLong: Long=-1
    var hasSpinnerOpened = false
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

        shutOffSpinner=findViewById(R.id.shutOffSpinner)
        hourPicker=findViewById(R.id.hourPicker)
        minutePicker=findViewById(R.id.minutePicker)
        secondPicker=findViewById(R.id.secondPicker)
        tonesSpinner=findViewById(R.id.tonesSpinner)


        setItemLayouts()
        setUpShutoffPickers()
        handleShutOffTime()
        createNotificationChannel();

        //default time is 6:00
        timePicker.hour=6;
        timePicker.minute=0;

        //label cant be over 20 chars
        val maxLength = 20
        val filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))
        labelText.filters = filters
        if(labelText.text==null){
            labelText.setText("")
        }

        //If clicked on an alarm object, set data from database
        if(intent.hasExtra("key")){
            //timePicker.hour=intent.getIntExtra("key",6)
            val context: Context = this
            val alarmHelper = AlarmDatabaseHelper.getInstance(context);
            val id=intent.getIntExtra("key",6)
            val data=alarmHelper.retrieveData(id)
            val(toggle,time)=alarmHelper.retrieveData(id)
            val(weekDays,label)=alarmHelper.retrieveWeekDaysLabel(id)
            val(tones,shutofftime)=alarmHelper.retrieveTonesShutOffTime(id)
            val cal=Calendar.getInstance()
            //Set time picker as the current alarm time that is stored in the db
            if (data != null) {
                cal.timeInMillis = time
                timePicker.hour=cal.get(Calendar.HOUR_OF_DAY)
                timePicker.minute=cal.get(Calendar.MINUTE)

            }
            //Set check boxes as checked boxes that are stored in the db
            if(weekDays!=null || weekDays!= ""){


                checkedBoxes=alarmHelper.convertStringToArray(weekDays)

                for((index,cb) in checkBoxes.withIndex()){
                    cb.isChecked=checkedBoxes[index]
                }
            }
            //Set label as label from db
            if(label!=null||label!=""){
                labelText.setText(label)
            }



            //Set selected tone as tones from db
            val adapter=tonesSpinner.adapter

            for (position in 0 until adapter.count) {
                if (adapter.getItem(position) == tones) {
                    tonesSpinner.setSelection(position)
                    break
                }
            }

            //Set shutOffTimePickers from db
            if(shutofftime==-1L){
                shutOffSpinner.setSelection(0)
                shutOffisForever=true
            }
            else{
                //Set selection automatically calls setOnItemSelectedListener
                shutOffSpinner.setSelection(1)
                shutOffisForever=false

                val(h,m,s)=alarmHelper.convertMillisecondsToHoursMinSecs(shutofftime)
                hourPicker.value=h
                minutePicker.value=m
                secondPicker.value=s

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
            //If a day of week check box checked choose time as the nearest day of week from current time
            if(!allDaysOff){
                calendar.timeInMillis= am.dayOfWeekInMillis(checkedBoxes,calendar.timeInMillis)
            }

            //Log checked boxes and date alarm will go off
            val t = am.convertTimeInMillisToDate(calendar.timeInMillis)
            Log.d("Time","$t ")
            for((index,cb) in checkedBoxes.withIndex()){
                Log.d("Time2","$cb + $index")
            }

            //shut off spinner is on setTime not forever



            // Get database
            val context: Context = this
            val alarmHelper = AlarmDatabaseHelper.getInstance(context);

            if(!shutOffisForever){
                shutOffTimeLong=alarmHelper.convertHourMinSecToMilliseconds(hourPicker.value,minutePicker.value,secondPicker.value)
            }

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
                    put("tones",tonesSpinner.selectedItem.toString())
                    put("shutofftime",shutOffTimeLong)
                }
                val id=db.insert("alarm", null, values)


                Toast.makeText(this, "ID is $id", Toast.LENGTH_SHORT).show()
                db.close()



                // Get the alarm manager
                alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

                // Create a pending intent for the alarm
                val intent = Intent(this, AlarmReceiver::class.java)
                intent.putExtra("key",id.toInt())
                intent.putExtra("shutoff",shutOffTimeLong)
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
                //
                //update tones and shutofftime
                alarmHelper.updateTones(id,tonesSpinner.selectedItem.toString())
                alarmHelper.updateShutOffTime(id,shutOffTimeLong)

                if(!allDaysOff){
                    calendar.timeInMillis= am.dayOfWeekInMillis(checkedBoxes,calendar.timeInMillis)
                }
                val intent = Intent(this, AlarmReceiver::class.java)
                intent.putExtra("key",id)
                intent.putExtra("shutoff",shutOffTimeLong)
                intent.action = "ALARM_SET"
                val pendingIntent = PendingIntent.getBroadcast(this, id, intent, FLAG_IMMUTABLE)
                val am=AlarmManagerHelper.getInstance(this)
                am.setAlarm(id,calendar.timeInMillis,pendingIntent,"")
            }

            val intent2 = Intent(this, MainActivity::class.java)
            intent2.putExtra("key", "$hour:$minute");
            startActivity(intent2)
        }

    }

    private fun setItemLayouts() {
        val adap = tonesSpinner.adapter as ArrayAdapter<String?>
        val itemList = ArrayList<String?>()
        for (i in 0 until adap.count) {
            val item = adap.getItem(i)
            itemList.add(item)
        }
        tonesSpinner.adapter
        val adapter=ArrayAdapter<String>(this, R.layout.custom_spinner_item,itemList)
        tonesSpinner.adapter = adapter
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)



        val adap2 = shutOffSpinner.adapter as ArrayAdapter<String?>
        val itemList2 = ArrayList<String?>()
        for (i in 0 until adap2.count) {
            val item = adap2.getItem(i)
            itemList2.add(item)
        }
        shutOffSpinner.adapter
        val adapter2=ArrayAdapter<String>(this, R.layout.custom_spinner_item,itemList2)
        shutOffSpinner.adapter = adapter2
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)


        //val numberPickerEditText = hourPicker.findViewById<View>(Resources.getSystem().getIdentifier("numberpicker_input", "id", "android")) as EditText

        // Set the text color of the EditText
//        numberPickerEditText.setTextColor(ContextCompat.getColor(this, R.color.white))
//        numberPickerEditText.setHighlightColor(ContextCompat.getColor(this, R.color.white))

        val customDrawable = ContextCompat.getDrawable(this, R.drawable.custom_numberpicker_background)

// Set the custom drawable as the background of the NumberPicker

        val parentLayout = findViewById<LinearLayout>(R.id.numberPickerLayout)
        parentLayout.background=customDrawable


    }

    fun setUpShutoffPickers(){

        hourPicker.minValue=0
        hourPicker.maxValue=99
        hourPicker.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })//2 digit values onlyjkl/
        minutePicker.minValue=0
        minutePicker.maxValue=59
        minutePicker.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })//2 digit values onlyjkl/

        secondPicker.minValue=0
        secondPicker.maxValue=59

        minutePicker.value=3
        secondPicker.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })//2 digit values onlyjkl/

    }

    fun handleShutOffTime(){
        val parentLayout = findViewById<LinearLayout>(R.id.numberPickerLayout)
        val fadeInAnimation = AnimationUtils.loadAnimation(parentLayout.context, R.anim.fade_in)
        val fadeOutAnimation = AnimationUtils.loadAnimation(parentLayout.context, R.anim.fade_out)
        var pos=0;
        shutOffSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                pos=position
                selectedSpinnerItem = parent.getItemAtPosition(position).toString()
                if(position==0){
                    //Makes it so animation doesnt happen when alarm editor is first opened, only when a spinner is touched
                    if(hasSpinnerOpened())
                        parentLayout.startAnimation(fadeOutAnimation)
                    parentLayout.visibility=View.INVISIBLE
                    hourPicker.visibility=View.INVISIBLE
                    secondPicker.visibility=View.INVISIBLE
                    minutePicker.visibility=View.INVISIBLE
                    shutOffTimeLong=-1L
                    shutOffisForever=true
                }
                else{
                    if(hasSpinnerOpened())
                        parentLayout.startAnimation(fadeInAnimation)
                    parentLayout.visibility=View.VISIBLE
                    hourPicker.visibility=View.VISIBLE
                    secondPicker.visibility=View.VISIBLE
                    minutePicker.visibility=View.VISIBLE
                    shutOffisForever=false
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing here

            }

        })

    }

    fun hasSpinnerOpened(): Boolean{
        if(!hasSpinnerOpened){
            shutOffSpinner.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    // Spinner was clicked or opened

                    hasSpinnerOpened = true
                }
                false // Return false to allow other touch events to be processed
            }
        }


        return hasSpinnerOpened

    }





    /*There is no way to set importance level of notification channel again to IMPORTANCE_HIGH programmatically after user turns it OFF.
    Only user can change it via settings. As per docs.
     "Sets the level of interruption of this notification channel.
     Only modifiable before the channel is submitted to NotificationManager.createNotificationChannel(NotificationChannel)."*/
    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create a notification channel
            val channel = NotificationChannel("alarm_channel4", "Alarms", NotificationManager.IMPORTANCE_HIGH)
            //channel.enableVibration(true)
            //channel.vibration = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            channel.importance=NotificationManager.IMPORTANCE_HIGH
            // Get the NotificationManager
            val notificationManager = getSystemService(NotificationManager::class.java)
            channel.setSound(null, null)

            notificationManager.createNotificationChannel(channel)


        }

    }

}