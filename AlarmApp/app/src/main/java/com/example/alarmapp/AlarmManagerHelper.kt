import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.alarmapp.AlarmDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class AlarmManagerHelper private constructor(private val context: Context) {

    private var alarmManager: AlarmManager? = null

    companion object {
        @Volatile
        private var INSTANCE: AlarmManagerHelper? = null

        fun getInstance(context: Context): AlarmManagerHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AlarmManagerHelper(context).also { INSTANCE = it }
            }
    }

    fun setAlarm(alarmId: Int, triggerTime: Long, pendingIntent: PendingIntent, timeString: String) {


        val db=AlarmDatabaseHelper.getInstance(context)
        //Updates db but also checks if alarm is before current time, if so adds days to alarm
        //until its over the current time, then returns new alarm time
        var newTime = db.updateData(alarmId,triggerTime,1)

        //If there are checked boxes for days of week
        val (weekdays,label)=db.retrieveWeekDaysLabel(alarmId)
        var checkedBoxes:Array<Boolean>
        if(weekdays!="" || weekdays!=null){
           checkedBoxes = db.convertStringToArray(weekdays)
            if(!allDaysOfWeekOff(checkedBoxes)){
                newTime=dayOfWeekInMillis(checkedBoxes,newTime)
            }
        }

        getAlarmManager()?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, newTime, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, newTime, pendingIntent)
            } else {
                am.set(AlarmManager.RTC_WAKEUP, newTime, pendingIntent)
            }
            val calendar = Calendar.getInstance()
            calendar.timeInMillis=newTime
            val hour=Calendar.HOUR_OF_DAY
            val min=Calendar.MINUTE
            Toast.makeText(context, "Alarm set! $timeString", Toast.LENGTH_SHORT).show()
        }
    }



    fun cancelAlarm(pendingIntent: PendingIntent, timeString: String="") {
        getAlarmManager()?.cancel(pendingIntent)
        if(timeString!="")
            Toast.makeText(context, "Alarm off! $timeString", Toast.LENGTH_SHORT).show()
    }

    private fun getAlarmManager(): AlarmManager? {
        if (alarmManager == null) {
            alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        }
        return alarmManager
    }


    /**Check if all days of the week are off
     * **/
    fun allDaysOfWeekOff(checkedBoxes: Array<Boolean>):Boolean{
        var allDaysOff=true
        for(cb in checkedBoxes){
            if(cb){
                allDaysOff=false
            }
        }

        return allDaysOff
    }

    /**Check the upcoming day of the week until we get a day that is checked, so we can set the alarm
     * on that day..
     * returns the time in millis
     * **/
    fun dayOfWeekInMillis(checkedBoxes: Array<Boolean>, timeInMillis: Long): Long{
        val calendar= Calendar.getInstance()
        calendar.timeInMillis=timeInMillis
        var dayOfWeek = calendar[Calendar.DAY_OF_WEEK]
        var dayOfWeekIndex: Int
        dayOfWeekIndex = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> -1
        }

        while(!checkedBoxes[dayOfWeekIndex]){
            calendar.add(Calendar.DATE,1)
            dayOfWeek = calendar[Calendar.DAY_OF_WEEK]
            dayOfWeekIndex = when (dayOfWeek) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> -1
            }
        }
        return calendar.timeInMillis
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
