package com.example.alarmapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar


class MainActivity : AppCompatActivity() {
    private lateinit var addAlarm: ImageButton
    private lateinit var removeAlarm: ImageButton
    private lateinit var alarmList: ArrayList<AlarmItemModel>;
    private lateinit var recyclerView: RecyclerView;
    private lateinit var toolbar: Toolbar;
    private lateinit var _adapter: MyAdapter;
    private var toggleVisible=View.VISIBLE
    private var removeVisible=View.INVISIBLE
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //addAlarm=findViewById(R.id.addAlarm);
        //removeAlarm=findViewById(R.id.removeAlarm);
        toolbar=findViewById(R.id.toolbar)
        //createNotificationChannel();

        setSupportActionBar(toolbar)

//        addAlarm.setOnClickListener {
//
//
//        }



        recyclerView= findViewById(R.id.alarmRecycler)
        recyclerView.setBackgroundColor(Color.parseColor("#F0F0F0"))
        recyclerView.layoutManager=LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        initRecyclerData();
        //Gets AlarmEditor data
        val data = intent.getStringExtra("key")
        if (data != null) {

            // Now you can use the 'data' variable as needed
            // For example, you can update the UI with this data or perform some action based on it.
            //addRecyclerAlarm(data);
        }


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
       val alarmHelper = AlarmDatabaseHelper.getInstance(context);

       val db = alarmHelper.readableDatabase
       val cursor = db.query("alarm", null, null, null, null, null, "time ASC" )

       with(cursor) {
           while (moveToNext()) {
               val alarmId = getInt(getColumnIndexOrThrow("_id"))
               val alarmTime=getLong(getColumnIndexOrThrow("time"))
               val alarmToggle=getInt(getColumnIndexOrThrow("toggle"))
               // Create an Alarm object and add it to the list
               val dateTime=convertTimeInMillisToDate(alarmTime)
               Log.d("date", "$dateTime")
               val parts = dateTime.split(" ")
               var date=""
               var time=""
               var meridian= ""

               if (parts.size >= 2) {
                   date = parts[0]
                   if(parts[1].get(0)=='0'){
                       time = parts[1].drop(1)
                   }
                   else{
                       time=parts[1]
                   }
                   meridian = parts[2]
               }
               alarmList.add(AlarmItemModel(time,meridian,date,alarmId,alarmToggle ))
               //alarmList.add(AlarmItemModel(time,meridian,date,alarmId ))
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




}