package com.example.alarmapp

import AlarmManagerHelper
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ads.nativetemplates.NativeTemplateStyle
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private lateinit var adLoader: AdLoader
    private lateinit var nativeAdTemplate: TemplateView
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
    private var adsLoaded: Boolean = false
    private lateinit var alarmHelper: AlarmDatabaseHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadNativeAds()
        //addAlarm=findViewById(R.id.addAlarm);
        //removeAlarm=findViewById(R.id.removeAlarm);
        nextAlarmText=findViewById(R.id.nextAlarmTime)
        toolbar=findViewById(R.id.toolbar)
        //createNotificationChannel();
        collapsingToolbar=findViewById(R.id.collapsingBarLayout)
        collapsingToolbar.setExpandedTitleTextAppearance(R.style.AppBarExpanded)
        collapsingToolbar.setCollapsedTitleTextAppearance(R.style.AppBarCollapsed)


        setSupportActionBar(toolbar)





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
       val dd=AlarmDatabaseHelper.getInstance(context)

       val am= AlarmManagerHelper.getInstance(context)
       val db = alarmHelper.readableDatabase
       val cursor = db.query("alarm", null, null, null, null, null, "time ASC" )

       with(cursor) {
           while (moveToNext()) {
               val alarmId = getInt(getColumnIndexOrThrow("_id"))
               var alarmTime=getLong(getColumnIndexOrThrow("time"))
               val alarmToggle=getInt(getColumnIndexOrThrow("toggle"))
               var alarmLabel=getString(getColumnIndexOrThrow("label"))

               if(alarmLabel==null){alarmLabel=""}
               alarmList.add(AlarmItemModel(alarmTime,alarmId,alarmToggle,alarmLabel))
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
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun run() {
                // Update the time difference for each item in the list
                var lowestTimeDiff=Long.MAX_VALUE
                handler.post{
                    if(alarmList.isEmpty()){
                        nextAlarmText.text="No alarms Set"
                    }
                }

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


                            nextAlarmText.text="Next Alarm in $timeInHMS"
                        }
                        else{
                            nextAlarmText.text="All Alarms off"
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

 fun loadNativeAds(){



     MobileAds.initialize(this) {}
        //Test ca-app-pub-3940256099942544/2247696110
        //"ca-app-pub-1844708831767128/8013719447" appid
        //Real adUnitId ca-app-pub-3940256099942544/2247696110
     adLoader = AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110")
         .forNativeAd { ad : NativeAd ->
             // Show the ad.
             if (!adLoader.isLoading) {
                 Log.d("ad", "Ad loaded")
             }

             if (isDestroyed) {
                 ad.destroy()
                 return@forNativeAd
             }
             val styles = NativeTemplateStyle.Builder().withMainBackgroundColor(ColorDrawable(Color.BLACK)).build()
             nativeAdTemplate = findViewById<TemplateView>(R.id.nativeAdTemplate)
             nativeAdTemplate.setStyles(styles)
             nativeAdTemplate.setNativeAd(ad)
             adsLoaded=true

         }
         .withAdListener(object : AdListener() {
             override fun onAdFailedToLoad(adError: LoadAdError) {
                 // Handle the failure by logging, altering the UI, and so on.
                 Log.d("ad", adError.toString())
             }
         })
         .withNativeAdOptions(
             NativeAdOptions.Builder()
             // Methods in the NativeAdOptions.Builder class can be
             // used here to specify individual options settings.
             .build())
         .build()

// Load the ad
     adLoader.loadAd(AdRequest.Builder().build())
 }
    fun collapsedToolbar(){
        val fadeInAnimation = AlphaAnimation(0f, 1f)
        fadeInAnimation.duration = 1000 // Adjust the duration as needed
        val appBarLayout = findViewById<AppBarLayout>(R.id.appBarLayout)
        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            // Calculate the percentage of collapse (0 when fully expanded, 1 when fully collapsed)
            val percentage = Math.abs(verticalOffset.toFloat()) / appBarLayout.totalScrollRange.toFloat()

            // Adjust the alpha value of the ImageView based on the collapse percentage
            if(adsLoaded)
                nativeAdTemplate.alpha = percentage
        })
    }

}