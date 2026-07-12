package com.example.smsforwarder

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TIME + " TEXT,"
                + COLUMN_MSG + " TEXT,"
                + COLUMN_STATUS + " TEXT" + ")")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
        onCreate(db)
    }

    fun addLog(msg: String, status: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        values.put(COLUMN_TIME, timeFormat.format(Date()))
        values.put(COLUMN_MSG, msg)
        values.put(COLUMN_STATUS, status)
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun getAllLogs(): List<String> {
        val logList = ArrayList<String>()
        val selectQuery = "SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC LIMIT 100"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor.moveToFirst()) {
            do {
                val time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME))
                val msg = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MSG))
                val status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS))
                logList.add("[$time] $status\n$msg")
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return logList
    }

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "ForwarderLogs"
        private const val TABLE_NAME = "logs"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TIME = "time"
        private const val COLUMN_MSG = "message"
        private const val COLUMN_STATUS = "status"
    }
}
