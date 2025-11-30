package com.example.scheduleconnect

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ScheduleConnect.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_USERS = "users"

        // Column Names
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
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = ("CREATE TABLE " + TABLE_USERS + "("
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
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // Function to insert a new user
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
        db.close()
        return result != -1L
    }

    // --- THIS IS THE MISSING FUNCTION ---
    fun checkUser(input: String, password: String): Boolean {
        val db = this.readableDatabase
        val columns = arrayOf(COL_ID)

        // Checks if the entered text matches EITHER the Username OR the Email
        val selection = "($COL_USERNAME = ? OR $COL_EMAIL = ?) AND $COL_PASSWORD = ?"
        val selectionArgs = arrayOf(input, input, password)

        val cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null)
        val count = cursor.count
        cursor.close()
        db.close()
        return count > 0
    }
}