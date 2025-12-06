package com.example.scheduleconnect

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.ArrayList

data class Schedule(
    val id: Int,
    val creator: String,
    val groupId: Int,
    val title: String,
    val date: String,
    val location: String,
    val description: String,
    val type: String
)

data class GroupInfo(val id: Int, val name: String, val code: String)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ScheduleConnect.db"
        private const val DATABASE_VERSION = 8

        // Table Names
        private const val TABLE_USERS = "users"
        private const val TABLE_SCHEDULES = "schedules"
        private const val TABLE_GROUPS = "schedule_groups"
        private const val TABLE_MEMBERS = "group_members"
        private const val TABLE_RSVP = "rsvps"

        // Schedule Columns
        const val SCH_ID = "schedule_id"
        const val SCH_USERNAME = "username"
        const val SCH_GROUP_ID = "group_id" // New: Links schedule to a group
        const val SCH_TITLE = "title"
        const val SCH_DATE = "date"
        const val SCH_LOCATION = "location"
        const val SCH_DESC = "description"
        const val SCH_TYPE = "type"

        // RSVP Columns
        const val RSVP_ID = "rsvp_id"
        const val RSVP_SCH_ID = "schedule_id"
        const val RSVP_USER = "username"
        const val RSVP_STATUS = "status" // 1=Attend, 2=Unsure, 3=Not Attend
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Users Table
        db?.execSQL("CREATE TABLE $TABLE_USERS (id INTEGER PRIMARY KEY AUTOINCREMENT, first_name TEXT, middle_name TEXT, last_name TEXT, gender TEXT, dob TEXT, email TEXT, phone TEXT, username TEXT, password TEXT)")

        // Schedules Table (with group_id)
        db?.execSQL("CREATE TABLE $TABLE_SCHEDULES ($SCH_ID INTEGER PRIMARY KEY AUTOINCREMENT, $SCH_USERNAME TEXT, $SCH_GROUP_ID INTEGER DEFAULT -1, $SCH_TITLE TEXT, $SCH_DATE TEXT, $SCH_LOCATION TEXT, $SCH_DESC TEXT, $SCH_TYPE TEXT)")

        // Groups Table
        db?.execSQL("CREATE TABLE $TABLE_GROUPS (group_id INTEGER PRIMARY KEY AUTOINCREMENT, group_name TEXT, group_code TEXT)")

        // Members Table
        db?.execSQL("CREATE TABLE $TABLE_MEMBERS (member_id INTEGER PRIMARY KEY AUTOINCREMENT, group_id INTEGER, username TEXT)")

        // RSVP Table
        db?.execSQL("CREATE TABLE $TABLE_RSVP ($RSVP_ID INTEGER PRIMARY KEY AUTOINCREMENT, $RSVP_SCH_ID INTEGER, $RSVP_USER TEXT, $RSVP_STATUS INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Reset database on upgrade to avoid conflicts during development
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_SCHEDULES")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_GROUPS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_MEMBERS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_RSVP")
        onCreate(db)
    }

    // --- USER METHODS ---
    fun addUser(fName: String, mName: String, lName: String, gender: String, dob: String, email: String, phone: String, user: String, pass: String): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put("first_name", fName); cv.put("middle_name", mName); cv.put("last_name", lName)
        cv.put("gender", gender); cv.put("dob", dob); cv.put("email", email)
        cv.put("phone", phone); cv.put("username", user); cv.put("password", pass)
        return db.insert(TABLE_USERS, null, cv) != -1L
    }

    fun checkUsernameOrEmail(input: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE username = ? OR email = ?", arrayOf(input, input))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun checkUser(input: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE (username = ? OR email = ?) AND password = ?", arrayOf(input, input, password))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    // --- SCHEDULE METHODS ---
    // Added groupId parameter
    fun addSchedule(user: String, groupId: Int, title: String, date: String, location: String, desc: String, type: String): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(SCH_USERNAME, user)
        cv.put(SCH_GROUP_ID, groupId)
        cv.put(SCH_TITLE, title)
        cv.put(SCH_DATE, date)
        cv.put(SCH_LOCATION, location)
        cv.put(SCH_DESC, desc)
        cv.put(SCH_TYPE, type)
        return db.insert(TABLE_SCHEDULES, null, cv) != -1L
    }

    // Fetch Personal Schedules
    fun getSchedules(user: String, type: String): ArrayList<Schedule> {
        // Only returns personal schedules for HomeFragment
        val list = ArrayList<Schedule>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SCHEDULES WHERE $SCH_TYPE = ? AND $SCH_USERNAME = ?", arrayOf(type, user))

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(SCH_ID))
                val creator = cursor.getString(cursor.getColumnIndexOrThrow(SCH_USERNAME))
                val gid = cursor.getInt(cursor.getColumnIndexOrThrow(SCH_GROUP_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(SCH_TITLE))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(SCH_DATE))
                val loc = cursor.getString(cursor.getColumnIndexOrThrow(SCH_LOCATION))
                val desc = cursor.getString(cursor.getColumnIndexOrThrow(SCH_DESC))
                list.add(Schedule(id, creator, gid, title, date, loc, desc, type))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // Fetch Schedules for a specific Group
    fun getGroupSchedules(groupId: Int): ArrayList<Schedule> {
        val list = ArrayList<Schedule>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SCHEDULES WHERE $SCH_GROUP_ID = ?", arrayOf(groupId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(SCH_ID))
                val creator = cursor.getString(cursor.getColumnIndexOrThrow(SCH_USERNAME))
                val gid = cursor.getInt(cursor.getColumnIndexOrThrow(SCH_GROUP_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(SCH_TITLE))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(SCH_DATE))
                val loc = cursor.getString(cursor.getColumnIndexOrThrow(SCH_LOCATION))
                val desc = cursor.getString(cursor.getColumnIndexOrThrow(SCH_DESC))
                val type = cursor.getString(cursor.getColumnIndexOrThrow(SCH_TYPE))
                list.add(Schedule(id, creator, gid, title, date, loc, desc, type))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // --- GROUP METHODS ---
    fun createGroup(name: String, code: String, creator: String): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put("group_name", name)
        cv.put("group_code", code)
        val groupId = db.insert(TABLE_GROUPS, null, cv)

        if (groupId != -1L) {
            val memberCv = ContentValues()
            memberCv.put("group_id", groupId)
            memberCv.put("username", creator)
            db.insert(TABLE_MEMBERS, null, memberCv)
            return true
        }
        return false
    }

    fun joinGroup(user: String, code: String): Boolean {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT group_id FROM $TABLE_GROUPS WHERE group_code = ?", arrayOf(code))
        if (cursor.moveToFirst()) {
            val groupId = cursor.getInt(0)
            cursor.close()

            val checkCursor = db.rawQuery("SELECT * FROM $TABLE_MEMBERS WHERE group_id = ? AND username = ?", arrayOf(groupId.toString(), user))
            if (checkCursor.count > 0) {
                checkCursor.close()
                return false
            }
            checkCursor.close()

            val cv = ContentValues()
            cv.put("group_id", groupId)
            cv.put("username", user)
            return db.insert(TABLE_MEMBERS, null, cv) != -1L
        }
        cursor.close()
        return false
    }

    fun getUserGroups(user: String): ArrayList<GroupInfo> {
        val list = ArrayList<GroupInfo>()
        val db = this.readableDatabase
        val query = "SELECT g.group_id, g.group_name, g.group_code FROM $TABLE_GROUPS g INNER JOIN $TABLE_MEMBERS m ON g.group_id = m.group_id WHERE m.username = ?"
        val cursor = db.rawQuery(query, arrayOf(user))
        if (cursor.moveToFirst()) {
            do {
                list.add(GroupInfo(cursor.getInt(0), cursor.getString(1), cursor.getString(2)))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getGroupMembers(groupId: Int): ArrayList<String> {
        val list = ArrayList<String>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT username FROM $TABLE_MEMBERS WHERE group_id = ?", arrayOf(groupId.toString()))
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // --- RSVP METHODS ---
    fun updateRSVP(scheduleId: Int, user: String, status: Int) {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT $RSVP_ID FROM $TABLE_RSVP WHERE $RSVP_SCH_ID = ? AND $RSVP_USER = ?", arrayOf(scheduleId.toString(), user))

        val cv = ContentValues()
        cv.put(RSVP_SCH_ID, scheduleId)
        cv.put(RSVP_USER, user)
        cv.put(RSVP_STATUS, status)

        if (cursor.moveToFirst()) {
            db.update(TABLE_RSVP, cv, "$RSVP_SCH_ID = ? AND $RSVP_USER = ?", arrayOf(scheduleId.toString(), user))
        } else {
            db.insert(TABLE_RSVP, null, cv)
        }
        cursor.close()
    }
}