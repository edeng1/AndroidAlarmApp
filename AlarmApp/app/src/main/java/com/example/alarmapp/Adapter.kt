package com.example.alarmapp

import android.content.ClipData
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class Adapter(private val items: ArrayList<AlarmItemModel>): RecyclerView.Adapter<Adapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_alarm_editor, parent, false)
        return ViewHolder(view)

    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val alarmTime: TextView = itemView.findViewById(R.id.timeText);
        val alarmMeridian: TextView = itemView.findViewById(R.id.meridianText);
        val alarmDate: TextView = itemView.findViewById(R.id.dateText);
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        // Bind the data for the item to the view holder
        holder.alarmTime.setText(item.alarmTime);
        holder.alarmMeridian.text = item.meridian;
        holder.alarmMeridian.setText(item.alarmDate);

    }
    override fun getItemCount(): Int {
        return items.size
    }



}