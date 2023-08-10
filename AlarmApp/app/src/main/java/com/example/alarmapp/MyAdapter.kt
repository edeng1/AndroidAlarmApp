package com.example.alarmapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.opengl.Visibility
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MyAdapter(private val items: ArrayList<AlarmItemModel>,private val context: Context): RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
    private var toggleVisibility = View.VISIBLE
    private var removeVisibility= View.INVISIBLE


    interface OnRemoveClickListener {
        fun onRemoveClick(position: Int)
    }

    fun switchToggleAndRemove(toggleVisibility: Int,removeVisibility: Int){
        this.toggleVisibility=toggleVisibility
        this.removeVisibility=removeVisibility
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.alarm_item_design, parent, false)
        return MyViewHolder(view)

    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val alarmTime: TextView = itemView.findViewById(R.id.timeText);
        val alarmMeridian: TextView = itemView.findViewById(R.id.meridianText);
        val alarmDate: TextView = itemView.findViewById(R.id.dateText);
        val alarmSwitch: Switch = itemView.findViewById(R.id.alarmSwitch)
        val alarmRemove: ImageView=itemView.findViewById(R.id.deleteTime)
        val dayOfWeek: TextView= itemView.findViewById(R.id.daysOfWeekText)
        init {
            itemView.setOnClickListener(this)

        }




        override fun onClick(view: View) { //click on alarm
            // Handle the click event for the individual item here
            val position = adapterPosition
            // Perform actions based on the clicked item's position or data
            // Start a new activity when an item is clicked
            val context = view.context
            val intent = Intent(context, AlarmEditor::class.java)
            // Pass any data you want to the new activity using putExtra()
            // For example:
            // intent.putExtra("itemPosition", position)

            intent.putExtra("key", DataHolder.getInstance().hashMap[adapterPosition])
            context.startActivity(intent)
        }

        fun bind(item: AlarmItemModel, onRemoveClickListener: OnRemoveClickListener, context: Context) {
            // Bind your data to the view elements

            // Set OnClickListener for the removeButton
            alarmRemove.setOnClickListener {
                val db=AlarmDatabaseHelper.getInstance(context)
                val am=AlarmManagerHelper.getInstance(context)
                db.removeAlarm(item.alarmId)
                val intent = Intent(context, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(context, item.alarmId, intent, PendingIntent.FLAG_IMMUTABLE)
                am.cancelAlarm(pendingIntent)
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemoveClickListener.onRemoveClick(position)
                }
            }

        }

        fun bindItem(item:AlarmItemModel,context: Context){
            updateTimeDifference(item.alarmId,item.alarmToggle,item.alarmTimeInMillis,context)
        }
        //If current time passes alarm, it must update.
        fun updateTimeDifference(id: Int,toggle: Int,timeInMillis: Long, context: Context  ) {
            val db=AlarmDatabaseHelper.getInstance(context)
            val currentTimeMillis = System.currentTimeMillis()
            val timeDifference = timeInMillis - currentTimeMillis

            if (timeDifference > 0) {

            } else {
                db.updateData(id,timeInMillis,toggle)
            }
        }

    }
    //Adapter class
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = items[position]
        // Bind the data for the item to the view holder

        //updates time if over currenttime
        holder.bindItem(item,context)

        val (timeString,meridian,date)=convertTimeInMillisToDate(item.alarmTimeInMillis)

        val am=AlarmManagerHelper.getInstance(context)
        val db=AlarmDatabaseHelper.getInstance(context)

        holder.alarmTime.setText(timeString);
        holder.alarmMeridian.text = meridian;
        holder.alarmDate.setText(date);

        val dayOfWeekText=holder.dayOfWeek
        val switch=holder.alarmSwitch
        val remove= holder.alarmRemove
        val fadeInAnimationA = AnimationUtils.loadAnimation(switch.context, R.anim.fade_in)
        //val fadeOutAnimationA = AnimationUtils.loadAnimation(switch.context, R.anim.fade_out)
        val fadeInAnimationB = AnimationUtils.loadAnimation(remove.context, R.anim.fade_in)
        //val fadeOutAnimationB = AnimationUtils.loadAnimation(remove.context, R.anim.fade_out)
        if(switch.visibility==View.VISIBLE&&toggleVisibility==View.INVISIBLE){//Switch on to off
            //switch.startAnimation(fadeOutAnimationA)
            switch.visibility = toggleVisibility
            remove.startAnimation(fadeInAnimationB)
            remove.visibility=removeVisibility
        }
        else if(switch.visibility==View.INVISIBLE&&toggleVisibility==View.VISIBLE) {
            //switch.startAnimation(fadeOutAnimationB)
            switch.visibility = toggleVisibility
            switch.startAnimation(fadeInAnimationA)
            remove.visibility=removeVisibility
        }

        val (week,label)=db.retrieveWeekDaysLabel(item.alarmId)
        val checkedBoxes=db.convertStringToArray(week)

        //Sets MWTFSS visibility and bolds them if checkboxes are checked
        if(!am.allDaysOfWeekOff(checkedBoxes)){
            val spannableString = SpannableStringBuilder("M T W T F S S ")
            for((index,cb)in checkedBoxes.withIndex()){
                if(cb){
                    spannableString.setSpan(ForegroundColorSpan(Color.RED),index*2,(index*2)+1,SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE)
                }

            }

            dayOfWeekText.visibility=View.VISIBLE
            dayOfWeekText.text=spannableString
        }



        DataHolder.getInstance().hashMap[position]=item.alarmId

        //AlarmDatabaseHelper.getInstance(context)
        if(item.alarmToggle==1){
            holder.alarmSwitch.toggle()
        }



        //Toggle alarm
       switch.setOnCheckedChangeListener { buttonView, isChecked ->
            // Get the alarm manager


            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra("key",item.alarmId)
            val pendingIntent = PendingIntent.getBroadcast(context, item.alarmId, intent, PendingIntent.FLAG_IMMUTABLE)

            val id=item.alarmId
            val (toggle,time)= db.retrieveData(id)
            if(isChecked){

                db.updateData(id,time,1)
                am.setAlarm(id,time,pendingIntent,timeString)

            }else{
                db.updateData(id,time,0)
                am.cancelAlarm(pendingIntent, timeString)
            }

        }

        //Remove
        holder.bind(item, object : OnRemoveClickListener {
            override fun onRemoveClick(position: Int) {
                removeItem(position)
            }

        }, context)


    }
    override fun getItemCount(): Int {
        return items.size
    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun convertTimeInMillisToDate(timeInMillis: Long) : Triple<out String, out String, out String>{


        // Choose the desired date/time format
        val dateFormat = "yyyy-MM-dd hh:mm a" // For example: "yyyy-MM-dd hh:mm a" or "dd MMM yyyy, hh:mm a"


        // Create a SimpleDateFormat instance with the desired format and locale
        val simpleDateFormat = SimpleDateFormat(dateFormat, Locale.getDefault())

        // Convert timeInMillis to Date object
        val date = Date(timeInMillis)

        val dateTime=simpleDateFormat.format(date)

        val parts = dateTime.split(" ")
        var dateString=""
        var time=""
        var meridian= ""

        if (parts.size >= 2) {
            dateString = parts[0]
            if(parts[1].get(0)=='0'){
                time = parts[1].drop(1)
            }
            else{
                time=parts[1]
            }
            meridian = parts[2]
        }
        return Triple(time,meridian,dateString)
    }
    fun resetViewHolder(){
        notifyDataSetChanged()
    }




}