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
                "$COLUMN_WEEKDAYS STRING, $COLUMN_LABEL STRING)" //$COLUMN_TONES STRING, $COLUMN_SHUTOFFTIME LONG
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

    // Convert array to string (JSON format in this example)
    fun convertArrayToString(array: Array<Boolean>): String {
        return Gson().toJson(array)
    }

    // Convert string back to array
    fun convertStringToArray(arrayString: String): Array<Boolean> {
        return Gson().fromJson(arrayString, Array<Boolean>::class.java)
    }


    fun clearDatabase(TABLE_NAME: String) {
        val db = this.writableDatabase
        val clearDBQuery = "DELETE FROM $TABLE_ALARM"
        db.execSQL(clearDBQuery)
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
