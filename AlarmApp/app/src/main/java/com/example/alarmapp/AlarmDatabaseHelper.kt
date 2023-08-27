package com.example.alarmapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson
import java.util.*


class AlarmDatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "alarms.db"
        private const val DATABASE_VERSION = 1

        // Define table name and column names
        private const val TABLE_ALARM = "alarm"
        private const val COLUMN_ID = "_id"
        private const val COLUMN_TIME = "time"
        private const val COLUMN_LABEL = "label"
        private const val COLUMN_TOGGLE = "toggle"
        private const val COLUMN_WEEKDAYS = "weekdays"
        private const val COLUMN_TONES = "tones"
        private const val COLUMN_SHUTOFFTIME = "shutofftime"
        // Add other columns as needed

        // Singleton instance
        @Volatile
        private var instance: AlarmDatabaseHelper? = null

        // Provide a global access method to get the instance
        fun getInstance(context: Context): AlarmDatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: AlarmDatabaseHelper(context.applicationContext).also { instance = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create the alarm table when the database is first created

        val createTableQuery = "CREATE TABLE IF NOT EXISTS $TABLE_ALARM " +
                "($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_TIME LONG, $COLUMN_TOGGLE INTEGER, " +
                "$COLUMN_WEEKDAYS STRING, $COLUMN_LABEL STRING, $COLUMN_TONES STRING, $COLUMN_SHUTOFFTIME LONG)" //$COLUMN_TONES STRING, $COLUMN_SHUTOFFTIME LONG
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Implement database upgrade if needed
        // For simplicity, we won't do anything for now
        if (oldVersion < 2) {
            // Code to handle the database upgrade from version 1 to version 2
            // For example, you might add a new column or modify the existing schema.
            db.execSQL("ALTER TABLE $TABLE_ALARM ADD COLUMN $COLUMN_TOGGLE INTEGER DEFAULT 0")
        }
    }


    // Add methods to interact with the database here (e.g., insertAlarm, updateAlarm, deleteAlarm, etc.)

    fun getAlarmCount(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_ALARM", null)
        cursor.moveToFirst()
        var count=0
        count = cursor.getInt(0)
        cursor.close()
        return count
    }

//    fun insertData(name: String): Long {
//        val db = this.writableDatabase
//        val values = ContentValues().apply {
//            put(COLUMN_NAME, name)
//        }
//
//        // The insert method returns the ID of the newly inserted row.
//        // You can use this ID if needed.
//        val newId = db.insert(TABLE_ALARM, null, values)
//        db.close()
//
//        return newId
//    }

    fun removeAlarm(alarmId: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_ALARM, "$COLUMN_ID=?", arrayOf(alarmId.toString()))
    }

    fun retrieveData(alarmId: Int): Pair<Int,Long> {
        val db = this.readableDatabase
        val cursor = db.query("alarm", null, null, null, null, null, "null")

        with(cursor) {
            while (moveToNext()) {
                val _id = getInt(getColumnIndexOrThrow("_id"))
                val toggle = getInt(getColumnIndexOrThrow("toggle"))
                val alarmTime=getLong(getColumnIndexOrThrow("time"))
                if(alarmId==_id){
                    return Pair(toggle,alarmTime)
                }

        }
        close()
    }
        return Pair(0,0);
    }
    fun retrieveWeekDaysLabel(alarmId: Int): Pair<String,String> {
        val db = this.readableDatabase
        val cursor = db.query("alarm", null, null, null, null, null, "null")

        with(cursor) {
            while (moveToNext()) {
                val _id = getInt(getColumnIndexOrThrow("_id"))
                val weekDays=getString(getColumnIndexOrThrow("weekdays"))
                val label=getString(getColumnIndexOrThrow("label"))
                if(alarmId==_id){
                    return Pair(weekDays,label)
                }

            }
            close()
        }
        return Pair("","");
    }

    fun retrieveTonesShutOffTime(alarmId: Int): Pair<String,Long> {
        val db = this.readableDatabase
        val cursor = db.query("alarm", null, null, null, null, null, "null")

        with(cursor) {
            while (moveToNext()) {
                val _id = getInt(getColumnIndexOrThrow("_id"))
                val tones=getString(getColumnIndexOrThrow("tones"))
                val shutOffTime=getLong(getColumnIndexOrThrow("shutofftime"))
                if(alarmId==_id){
                    return Pair(tones,shutOffTime)
                }

            }
            close()
        }
        return Pair("alarm_sound_1",-1L);
    }







    fun updateData(id: Int,time: Long, toggle: Int):Long{
        val newTime = isTimeBeforeCurrentTime(time)
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("_id",id )
            put("time", newTime)
            put("toggle",toggle)

        }
        // Define the selection criteria to identify the item you want to update
        val selection = "_id = ?"
        val selectionArgs = arrayOf(id.toString()) // Replace "1" with the ID of the item you want to update

        // Update the item in the database
        db.update("alarm", values, selection, selectionArgs)
        return newTime
    }

    fun updateDaysOfWeek(id: Int,weekDays: String,){
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("_id",id )
            put("weekdays",weekDays)

        }
        // Define the selection criteria to identify the item you want to update
        val selection = "_id = ?"
        val selectionArgs = arrayOf(id.toString()) // Replace "1" with the ID of the item you want to update

        // Update the item in the database
        db.update("alarm", values, selection, selectionArgs)

    }
    fun updateLabel(id: Int,label: String,){
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("_id",id )
            put("label",label)

        }
        // Define the selection criteria to identify the item you want to update
        val selection = "_id = ?"
        val selectionArgs = arrayOf(id.toString()) // Replace "1" with the ID of the item you want to update

        // Update the item in the database
        db.update("alarm", values, selection, selectionArgs)

    }

    fun updateTones(id: Int,tones: String){
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("_id",id )
            put("tones",tones)

        }
        // Define the selection criteria to identify the item you want to update
        val selection = "_id = ?"
        val selectionArgs = arrayOf(id.toString()) // Replace "1" with the ID of the item you want to update

        // Update the item in the database
        db.update("alarm", values, selection, selectionArgs)

    }

    fun updateShutOffTime(id: Int,shutOffTime: Long,){
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("_id",id )
            put("shutofftime",shutOffTime)

        }
        // Define the selection criteria to identify the item you want to update
        val selection = "_id = ?"
        val selectionArgs = arrayOf(id.toString()) // Replace "1" with the ID of the item you want to update

        // Update the item in the database
        db.update("alarm", values, selection, selectionArgs)

    }
/**
 * Used to check if there are more than one set alarms so
 * only one alarm can be set at a time.
 * */
    fun containsDuplicateSetAlarm(time: Long):Boolean{
        val timeInMillisList = ArrayList<Long>()
        val db = this.readableDatabase

        val projection = arrayOf(COLUMN_TIME) // The column you want to retrieve
        val cursor = db.query(
            TABLE_ALARM , // Replace with your table name
            projection,
            null,
            null,
            null,
            null,
            null
        )

// Iterate through the cursor to retrieve values and add them to the ArrayList
        while (cursor.moveToNext()) {
            if(timeInMillisList.contains(time)){
                return true
            }
            val timeInMillis = cursor.getLong(cursor.getColumnIndexOrThrow("time"))
            timeInMillisList.add(timeInMillis)
        }

// Close the cursor
        cursor.close()

        return false
    }


    /**
     *Checks if set time is before current time and adds
     * a day to bring it up to current time
     * Called in updateData*/

    fun isTimeBeforeCurrentTime(time: Long): Long{
        val calendar = Calendar.getInstance()
        var newTime=time
        val currentTime= Calendar.getInstance().timeInMillis
        // Check if the selected time is in the past
        while (newTime <= currentTime) {
            // Add one day to the selected time
            calendar.timeInMillis=newTime
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            newTime=calendar.timeInMillis
        }
        return newTime
    }

    /**Convert array to string (JSON format in this example)
     * Converts the boolean array of checked day of the week boxes
     * into string so it can be saved in the database as JSON
     * */
    fun convertArrayToString(array: Array<Boolean>): String {
        return Gson().toJson(array)
    }

    /**Convert JSON string of checked day of week boxes from database
     * back into Array of Booleans
     * */
    fun convertStringToArray(arrayString: String): Array<Boolean> {
        return Gson().fromJson(arrayString, Array<Boolean>::class.java)
    }


    fun clearDatabase(TABLE_NAME: String) {
        val db = this.writableDatabase
        val clearDBQuery = "DELETE FROM $TABLE_ALARM"
        db.execSQL(clearDBQuery)
    }

    fun convertHourMinSecToMilliseconds(hour: Int, min: Int, sec: Int): Long {

        val totalMilliseconds = (hour * 3600 + min * 60 + sec) * 1000L
        return totalMilliseconds
    }

    fun convertMillisecondsToHoursMinSecs(milliseconds: Long): Triple<Int,Int,Int> {
        val totalSeconds = milliseconds / 1000
        val hours = (totalSeconds / 3600).toInt()
        val minutes = ((totalSeconds % 3600) / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()

        return Triple(hours,minutes,seconds)
    }


    fun deleteTable() {

        val db = this.writableDatabase

        // Replace "my_table" with the name of the table you want to delete


        // Execute the SQL command to drop the table
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALARM")

        db.close()
    }
    fun recreateTable() {
        val db = writableDatabase
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALARM")
        onCreate(db)
        db.close()
    }

}
