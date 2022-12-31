package com.example.alarmapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var addAlarm: ImageButton
    private lateinit var removeAlarm: ImageButton
    private lateinit var alarmList: ArrayList<AlarmItemModel>;
    private lateinit var recyclerView: RecyclerView;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addAlarm=findViewById(R.id.addAlarm);
        removeAlarm=findViewById(R.id.removeAlarm);

        addAlarm.setOnClickListener {
            val intent = Intent(this, AlarmEditor::class.java)
            startActivity(intent)
        }


        recyclerView= findViewById<RecyclerView>(R.id.alarmRecycler)
        alarmList=arrayListOf<AlarmItemModel>()
        initRecyclerData();

        recyclerView.adapter = Adapter(alarmList)




    }

   private fun initRecyclerData(){
       for(n in 0..5){
           alarmList.add(AlarmItemModel(9+n,"PM",2020+n));
       }



    }


}