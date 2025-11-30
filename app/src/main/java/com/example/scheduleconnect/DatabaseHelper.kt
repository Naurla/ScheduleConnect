package com.example.scheduleconnect

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.ArrayList
import java.util.UUID

// Data class to hold schedule info
data class Schedule(
    val id: Int,
    val title: String,
    val date: String,
    val location: String,
    val description: String,
    val type: String,
    val status: String, // "DONE" or "DID NOT ATTEND"
    val creator: String // Field to store creator's name
)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ScheduleConnect.db"
        private const val DATABASE_VERSION = 6

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
        const val COL_GROUP_CODE = "group_code"

        // --- Schedules Table ---
        private const val TABLE_SCHEDULES = "schedules"
        const val SCH_ID = "schedule_id"
        const val SCH_USERNAME = "username"
        const val SCH_TITLE = "title"
        const val SCH_DATE = "date"
        const val SCH_LOCATION = "location"
        const val SCH_DESC = "description"
        const val SCH_TYPE = "type" // "personal" or "shared"
        const val SCH_STATUS = "status"
        const val SCH_CREATOR = "creator"

        // --- Groups Table ---
        private const val TABLE_GROUPS = "groups"
        const val GRP_ID = "group_id"
        const val GRP_NAME = "group_name"
        const val GRP_CODE = "group_code"
        const val GRP_CREATOR = "creator_username"
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
                + COL_PASSWORD + " TEXT,"
                + COL_GROUP_CODE + " TEXT" + ")")
        db?.execSQL(createUsers)

        // Create Schedules Table
        val createSchedules = ("CREATE TABLE " + TABLE_SCHEDULES + "("
                + SCH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SCH_USERNAME + " TEXT,"
                + SCH_TITLE + " TEXT,"
                + SCH_DATE + " TEXT,"
                + SCH_LOCATION + " TEXT,"
                + SCH_DESC + " TEXT,"
                + SCH_TYPE + " TEXT,"
                + SCH_STATUS + " TEXT DEFAULT 'DONE',"
                + SCH_CREATOR + " TEXT" + ")")
        db?.execSQL(createSchedules)

        // Create Groups Table
        val createGroups = ("CREATE TABLE " + TABLE_GROUPS + "("
                + GRP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + GRP_NAME + " TEXT,"
                + GRP_CODE + " TEXT UNIQUE,"
                + GRP_CREATOR + " TEXT" + ")")
        db?.execSQL(createGroups)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db?.execSQL("ALTER TABLE $TABLE_SCHEDULES ADD COLUMN $SCH_USERNAME TEXT DEFAULT 'unknown'")
        }
        if (oldVersion < 4) {
            db?.execSQL("CREATE TABLE $TABLE_GROUPS ($GRP_ID INTEGER PRIMARY KEY AUTOINCREMENT, $GRP_NAME TEXT, $GRP_CODE TEXT UNIQUE, $GRP_CREATOR TEXT)")
            try { db?.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_GROUP_CODE TEXT") } catch (e: Exception) { }
        }
        if (oldVersion < 5) {
            try { db?.execSQL("ALTER TABLE $TABLE_SCHEDULES ADD COLUMN $SCH_STATUS TEXT DEFAULT 'DONE'") } catch (e: Exception) { }
        }
        if (oldVersion < 6) {
            try { db?.execSQL("ALTER TABLE $TABLE_SCHEDULES ADD COLUMN $SCH_CREATOR TEXT") } catch (e: Exception) { }
        }
    }

    // ===========================
    //      USER AUTH METHODS
    // ===========================

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

    // NEW: Get User's First Name
    fun getUserName(username: String): String? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COL_FIRST_NAME FROM $TABLE_USERS WHERE $COL_USERNAME = ?", arrayOf(username))
        if (cursor.moveToFirst()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COL_FIRST_NAME))
            cursor.close()
            return name
        }
        cursor.close()
        return null
    }

    // ===========================
    //      GROUP METHODS
    // ===========================

    fun getUserGroupCode(username: String): String? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COL_GROUP_CODE FROM $TABLE_USERS WHERE $COL_USERNAME = ?", arrayOf(username))
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(COL_GROUP_CODE)
            if (idx != -1) {
                val code = cursor.getString(idx)
                cursor.close()
                return code
            }
        }
        cursor.close()
        return null
    }

    fun getGroupInfo(groupCode: String): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_GROUPS WHERE $GRP_CODE = ?", arrayOf(groupCode))
    }

    fun createGroup(groupName: String, creatorUser: String): String? {
        val db = this.writableDatabase
        val groupCode = UUID.randomUUID().toString().substring(0, 6).uppercase()
        val cv = ContentValues()
        cv.put(GRP_NAME, groupName)
        cv.put(GRP_CODE, groupCode)
        cv.put(GRP_CREATOR, creatorUser)
        val result = db.insert(TABLE_GROUPS, null, cv)
        if (result != -1L) {
            updateUserGroup(creatorUser, groupCode)
            return groupCode
        }
        return null
    }

    fun joinGroup(groupCode: String, username: String): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_GROUPS WHERE $GRP_CODE = ?", arrayOf(groupCode))
        val groupExists = cursor.count > 0
        cursor.close()

        if (!groupExists) return 1

        val dbWrite = this.writableDatabase
        val cv = ContentValues()
        cv.put(COL_GROUP_CODE, groupCode)
        val rows = dbWrite.update(TABLE_USERS, cv, "$COL_USERNAME = ?", arrayOf(username))
        return if (rows > 0) 0 else 2
    }

    private fun updateUserGroup(username: String, groupCode: String): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(COL_GROUP_CODE, groupCode)
        return db.update(TABLE_USERS, cv, "$COL_USERNAME = ?", arrayOf(username)) > 0
    }

    // ===========================
    //      SCHEDULE METHODS
    // ===========================

    fun addSchedule(user: String, title: String, date: String, loc: String, desc: String, type: String, creator: String = ""): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(SCH_USERNAME, user)
        cv.put(SCH_TITLE, title)
        cv.put(SCH_DATE, date)
        cv.put(SCH_LOCATION, loc)
        cv.put(SCH_DESC, desc)
        cv.put(SCH_TYPE, type)
        cv.put(SCH_STATUS, "DONE")
        cv.put(SCH_CREATOR, creator)
        return db.insert(TABLE_SCHEDULES, null, cv) != -1L
    }

    // UPDATED: Fixed NullPointerException by handling null values safely
    fun getSchedules(user: String, type: String? = null): ArrayList<Schedule> {
        val list = ArrayList<Schedule>()
        val db = this.readableDatabase
        val query = if (type == null) "SELECT * FROM $TABLE_SCHEDULES WHERE $SCH_USERNAME = ?"
        else "SELECT * FROM $TABLE_SCHEDULES WHERE $SCH_TYPE = ? AND $SCH_USERNAME = ?"
        val args = if (type == null) arrayOf(user) else arrayOf(type, user)
        val cursor = db.rawQuery(query, args)

        if (cursor.moveToFirst()) {
            do {
                // Safely retrieve strings, defaulting to empty string or "DONE" if null
                val statusIdx = cursor.getColumnIndex(SCH_STATUS)
                val status = if (statusIdx != -1) cursor.getString(statusIdx) ?: "DONE" else "DONE"

                val creatorIdx = cursor.getColumnIndex(SCH_CREATOR)
                val creator = if (creatorIdx != -1) cursor.getString(creatorIdx) ?: "" else ""

                list.add(Schedule(
                    cursor.getInt(cursor.getColumnIndexOrThrow(SCH_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(SCH_TITLE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(SCH_DATE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(SCH_LOCATION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(SCH_DESC)),
                    cursor.getString(cursor.getColumnIndexOrThrow(SCH_TYPE)),
                    status,
                    creator
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}