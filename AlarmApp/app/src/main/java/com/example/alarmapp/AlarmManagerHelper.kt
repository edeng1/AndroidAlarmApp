import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.alarmapp.AlarmDatabaseHelper
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
        val newTime = db.updateData(alarmId,triggerTime,1)

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
}
