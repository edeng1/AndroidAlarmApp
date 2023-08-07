package com.example.alarmapp

import AlarmManagerHelper
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.CollapsingToolbarLayout
import java.lang.Long.MAX_VALUE
import java.util.*
import kotlin.collections.ArrayList
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var addAlarm: ImageButton
    private lateinit var removeAlarm: ImageButton
    private lateinit var nextAlarmText: TextView
    private lateinit var alarmList: ArrayList<AlarmItemModel>;
    private lateinit var recyclerView: RecyclerView;
    private lateinit var toolbar: Toolbar;
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var _adapter: MyAdapter;
    private var toggleVisible=View.VISIBLE
    private var removeVisible=View.INVISIBLE
    private var timer: Timer? = null
    private lateinit var alarmHelper: AlarmDatabaseHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //addAlarm=findViewById(R.id.addAlarm);
        //removeAlarm=findViewById(R.id.removeAlarm);
        nextAlarmText=findViewById(R.id.nextAlarmTime)
        toolbar=findViewById(R.id.toolbar)
        //createNotificationChannel();
        collapsingToolbar=findViewById(R.id.collapsingBarLayout)
        collapsingToolbar.setExpandedTitleTextAppearance(R.style.AppBarExpanded)
        collapsingToolbar.setCollapsedTitleTextAppearance(R.style.AppBarCollapsed)


        setSupportActionBar(toolbar)

//        addAlarm.setOnClickListener {
//
//
//        }



        recyclerView= findViewById(R.id.alarmRecycler)
        recyclerView.setBackgroundColor(resources.getColor(R.color.darkish_color))
        recyclerView.layoutManager=LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        val context: Context = this
        alarmHelper=AlarmDatabaseHelper.getInstance(context);
        initRecyclerData();
        //Gets AlarmEditor data
        val data = intent.getStringExtra("key")
        if (data != null) {

            // Now you can use the 'data' variable as needed
            // For example, you can update the UI with this data or perform some action based on it.
            //addRecyclerAlarm(data);
        }

        startTimer()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val adapter=initRecyclerData()
        // Handle item selection

        return when (item.itemId) {
            R.id.addButton -> {
                val intent = Intent(this, AlarmEditor::class.java)
                startActivity(intent)
                true
            }
            R.id.removeButton -> {

                if(toggleVisible==View.VISIBLE){ //Toggle is visible
                    toggleVisible=View.INVISIBLE  //Set toggle invisible and remove visible
                    removeVisible=View.VISIBLE
                }else{ //Toggle invisible
                    toggleVisible= View.VISIBLE //Set toggle visible
                    removeVisible=View.INVISIBLE
                }

                _adapter.switchToggleAndRemove(toggleVisible,removeVisible) // switches toggle and remove visibility

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

   private fun initRecyclerData(): MyAdapter {
       alarmList=arrayListOf<AlarmItemModel>()

       // Retrieve all alarms from SQLite database

       val context: Context = this

       val am= AlarmManagerHelper.getInstance(context)
       val db = alarmHelper.readableDatabase
       val cursor = db.query("alarm", null, null, null, null, null, "time ASC" )

       with(cursor) {
           while (moveToNext()) {
               val alarmId = getInt(getColumnIndexOrThrow("_id"))
               var alarmTime=getLong(getColumnIndexOrThrow("time"))
               val alarmToggle=getInt(getColumnIndexOrThrow("toggle"))

               alarmList.add(AlarmItemModel(alarmTime,alarmId,alarmToggle))
           }
           close()
       }



       val adapter= MyAdapter(alarmList,context)
       recyclerView.adapter = adapter
       _adapter=adapter
       return adapter

   }
    private fun addRecyclerAlarm(data: String){
        alarmList=arrayListOf<AlarmItemModel>()
        //alarmList.add(AlarmItemModel(data,"AM","2022"));
        //val adapter= MyAdapter(alarmList)
        //recyclerView.adapter = adapter
    }
    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create a notification channel
            val channel = NotificationChannel("alarm_channel", "Alarms", NotificationManager.IMPORTANCE_HIGH)
            //channel.enableVibration(true)
            //channel.vibration = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)

            // Get the NotificationManager
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

    }
    fun convertTimeInMillisToDate(timeInMillis: Long): String {
        // Choose the desired date/time format
        val dateFormat = "yyyy-MM-dd hh:mm a" // For example: "yyyy-MM-dd hh:mm a" or "dd MMM yyyy, hh:mm a"


        // Create a SimpleDateFormat instance with the desired format and locale
        val simpleDateFormat = SimpleDateFormat(dateFormat, Locale.getDefault())

        // Convert timeInMillis to Date object
        val date = Date(timeInMillis)

        // Format the Date object to the desired date/time format
        return simpleDateFormat.format(date)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the timer when the activity is destroyed
        stopTimer()
    }

    private fun startTimer() {
        // Use Handler to update the UI on the main thread
        val handler = Handler(mainLooper)

        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // Update the time difference for each item in the list
                var lowestTimeDiff=Long.MAX_VALUE

                for (item in alarmList) {
                    val(toggle,time)=alarmHelper.retrieveData(item.alarmId)
                    val timeInMillis = item.alarmTimeInMillis
                    // Call updateTimeDifference in the UI thread
                    val timeDifference=checkTimeDifference(timeInMillis)
                    if(toggle==1){

                        if (timeDifference<lowestTimeDiff){
                            lowestTimeDiff=timeDifference
                        }

                    }
                    if(timeDifference<0){
                        handler.post {
                            initRecyclerData()
                            _adapter.notifyDataSetChanged()
                        }
                    }
                    handler.post {

                        if(lowestTimeDiff>0&&lowestTimeDiff!= Long.MAX_VALUE){

                            val timeInHMS= String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(lowestTimeDiff),
                                TimeUnit.MILLISECONDS.toMinutes(lowestTimeDiff) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(lowestTimeDiff)),
                                TimeUnit.MILLISECONDS.toSeconds(lowestTimeDiff) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(lowestTimeDiff)));


                            nextAlarmText.text="Next alarm in $timeInHMS"
                        }
                        else{
                            nextAlarmText.text="All alarms off"
                        }
                    }
                }



            }
        }, 0, 1000) // Update every 1 second
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    fun checkTimeDifference(timeInMillis: Long) : Long{
        val currentTimeMillis = System.currentTimeMillis()
        val timeDifference = timeInMillis - currentTimeMillis

        if (timeDifference > 0) {

        } else {

        }
        return timeDifference
    }



}