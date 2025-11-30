package com.example.scheduleconnect

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.ArrayList

// Data class to hold schedule info
data class Schedule(
    val id: Int,
    val title: String,
    val date: String,
    val location: String,
    val description: String,
    val type: String
)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ScheduleConnect.db"
        // UPDATED: Incremented version to 3 to trigger the update
        private const val DATABASE_VERSION = 3

        // --- Users Table ---
        private const val TABLE_USERS = "users"
        const val COL_ID = "id"
        const val COL_FIRST_NAME = "first_name"
        const val COL_MIDDLE_NAME = "middle_name"
        const val COL_LAST_NAME = "last_name"
        const val COL_GENDER = "gender"
        const val COL_DOB = "dob"
        const val COL_EMAIL = "email"
        const val COL_PHONE = "phone"
        const val COL_USERNAME = "username"
        const val COL_PASSWORD = "password"

        // --- Schedules Table ---
        private const val TABLE_SCHEDULES = "schedules"
        const val SCH_ID = "schedule_id"
        // UPDATED: Added username column constant
        const val SCH_USERNAME = "username"
        const val SCH_TITLE = "title"
        const val SCH_DATE = "date"
        const val SCH_LOCATION = "location"
        const val SCH_DESC = "description"
        const val SCH_TYPE = "type" // "personal" or "shared"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Create Users Table
        val createUsers = ("CREATE TABLE " + TABLE_USERS + "("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_FIRST_NAME + " TEXT,"
                + COL_MIDDLE_NAME + " TEXT,"
                + COL_LAST_NAME + " TEXT,"
                + COL_GENDER + " TEXT,"
                + COL_DOB + " TEXT,"
                + COL_EMAIL + " TEXT,"
                + COL_PHONE + " TEXT,"
                + COL_USERNAME + " TEXT,"
                + COL_PASSWORD + " TEXT" + ")")
        db?.execSQL(createUsers)

        // Create Schedules Table (UPDATED with username column)
        val createSchedules = ("CREATE TABLE " + TABLE_SCHEDULES + "("
                + SCH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SCH_USERNAME + " TEXT,"
                + SCH_TITLE + " TEXT,"
                + SCH_DATE + " TEXT,"
                + SCH_LOCATION + " TEXT,"
                + SCH_DESC + " TEXT,"
                + SCH_TYPE + " TEXT" + ")")
        db?.execSQL(createSchedules)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val createSchedules = ("CREATE TABLE " + TABLE_SCHEDULES + "("
                    + SCH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + SCH_TITLE + " TEXT,"
                    + SCH_DATE + " TEXT,"
                    + SCH_LOCATION + " TEXT,"
                    + SCH_DESC + " TEXT,"
                    + SCH_TYPE + " TEXT" + ")")
            db?.execSQL(createSchedules)
        }
        // UPDATED: Upgrade logic for version 3 (adds username column to existing tables)
        if (oldVersion < 3) {
            db?.execSQL("ALTER TABLE $TABLE_SCHEDULES ADD COLUMN $SCH_USERNAME TEXT DEFAULT 'unknown'")
        }
    }

    // --- USER METHODS ---

    fun addUser(fName: String, mName: String, lName: String, gender: String, dob: String, email: String, phone: String, user: String, pass: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_FIRST_NAME, fName)
        contentValues.put(COL_MIDDLE_NAME, mName)
        contentValues.put(COL_LAST_NAME, lName)
        contentValues.put(COL_GENDER, gender)
        contentValues.put(COL_DOB, dob)
        contentValues.put(COL_EMAIL, email)
        contentValues.put(COL_PHONE, phone)
        contentValues.put(COL_USERNAME, user)
        contentValues.put(COL_PASSWORD, pass)

        val result = db.insert(TABLE_USERS, null, contentValues)
        return result != -1L
    }

    fun checkUsernameOrEmail(input: String): Boolean {
        val db = this.readableDatabase
        val selection = "$COL_USERNAME = ? OR $COL_EMAIL = ?"
        val selectionArgs = arrayOf(input, input)
        val cursor = db.query(TABLE_USERS, arrayOf(COL_ID), selection, selectionArgs, null, null, null)
        val count = cursor.count
        cursor.close()
        return count > 0
    }

    fun checkUser(input: String, password: String): Boolean {
        val db = this.readableDatabase
        val selection = "($COL_USERNAME = ? OR $COL_EMAIL = ?) AND $COL_PASSWORD = ?"
        val selectionArgs = arrayOf(input, input, password)
        val cursor = db.query(TABLE_USERS, arrayOf(COL_ID), selection, selectionArgs, null, null, null)
        val count = cursor.count
        cursor.close()
        return count > 0
    }

    // --- SCHEDULE METHODS (UPDATED) ---

    // UPDATED: Now accepts 'user' string
    fun addSchedule(user: String, title: String, date: String, location: String, desc: String, type: String): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(SCH_USERNAME, user) // Store the username
        cv.put(SCH_TITLE, title)
        cv.put(SCH_DATE, date)
        cv.put(SCH_LOCATION, location)
        cv.put(SCH_DESC, desc)
        cv.put(SCH_TYPE, type)
        val result = db.insert(TABLE_SCHEDULES, null, cv)
        return result != -1L
    }

    // UPDATED: Now accepts 'user' string to filter results
    fun getSchedules(user: String, type: String): ArrayList<Schedule> {
        val list = ArrayList<Schedule>()
        val db = this.readableDatabase
        // Filter by type AND username
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_SCHEDULES WHERE $SCH_TYPE = ? AND $SCH_USERNAME = ?", arrayOf(type, user))

        if (cursor.moveToFirst()) {
            do {
                // We use getColumnIndex to be safe since column order might change
                val idIndex = cursor.getColumnIndex(SCH_ID)
                val titleIndex = cursor.getColumnIndex(SCH_TITLE)
                val dateIndex = cursor.getColumnIndex(SCH_DATE)
                val locIndex = cursor.getColumnIndex(SCH_LOCATION)
                val descIndex = cursor.getColumnIndex(SCH_DESC)

                if(idIndex != -1 && titleIndex != -1) {
                    val id = cursor.getInt(idIndex)
                    val title = cursor.getString(titleIndex)
                    val date = cursor.getString(dateIndex)
                    val loc = cursor.getString(locIndex)
                    val desc = cursor.getString(descIndex)

                    list.add(Schedule(id, title, date, loc, desc, type))
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}