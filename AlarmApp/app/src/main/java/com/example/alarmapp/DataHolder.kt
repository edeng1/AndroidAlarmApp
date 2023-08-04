package com.example.alarmapp

class DataHolder private constructor() {
    var data: String = ""
    var hashMap= HashMap<Int,Int>()
    companion object {
        @Volatile
        private var instance: DataHolder? = null

        fun getInstance(): DataHolder =
            instance ?: synchronized(this) {
                instance ?: DataHolder().also { instance = it }
            }
    }
}