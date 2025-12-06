package com.example.scheduleconnect

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.ArrayList

// --- Data Classes ---
data class Schedule(
    val id: Int, val creator: String, val groupId: Int, val title: String,
    val date: String, val location: String, val description: String, val type: String
)

data class GroupInfo(val id: Int, val name: String, val code: String)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ScheduleConnect.db"
        private const val DATABASE_VERSION = 8
        // ... (Keep your existing table constants here) ...
        private const val TABLE_USERS = "users"
        private const val TABLE_SCHEDULES = "schedules"
        private const val TABLE_GROUPS = "schedule_groups"
        private const val TABLE_MEMBERS = "group_members"
        private const val TABLE_RSVP = "rsvps"
        const val SCH_ID = "schedule_id"
        const val SCH_USERNAME = "username"
        const val SCH_GROUP_ID = "group_id"
        const val SCH_TITLE = "title"
        const val SCH_DATE = "date"
        const val SCH_LOCATION = "location"
        const val SCH_DESC = "description"
        const val SCH_TYPE = "type"
        const val RSVP_ID = "rsvp_id"
        const val RSVP_SCH_ID = "schedule_id"
        const val RSVP_USER = "username"
        const val RSVP_STATUS = "status"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE $TABLE_USERS (id INTEGER PRIMARY KEY AUTOINCREMENT, first_name TEXT, middle_name TEXT, last_name TEXT, gender TEXT, dob TEXT, email TEXT, phone TEXT, username TEXT, password TEXT)")
        db?.execSQL("CREATE TABLE $TABLE_SCHEDULES ($SCH_ID INTEGER PRIMARY KEY AUTOINCREMENT, $SCH_USERNAME TEXT, $SCH_GROUP_ID INTEGER DEFAULT -1, $SCH_TITLE TEXT, $SCH_DATE TEXT, $SCH_LOCATION TEXT, $SCH_DESC TEXT, $SCH_TYPE TEXT)")
        db?.execSQL("CREATE TABLE $TABLE_GROUPS (group_id INTEGER PRIMARY KEY AUTOINCREMENT, group_name TEXT, group_code TEXT)")
        db?.execSQL("CREATE TABLE $TABLE_MEMBERS (member_id INTEGER PRIMARY KEY AUTOINCREMENT, group_id INTEGER, username TEXT)")
        db?.execSQL("CREATE TABLE $TABLE_RSVP ($RSVP_ID INTEGER PRIMARY KEY AUTOINCREMENT, $RSVP_SCH_ID INTEGER, $RSVP_USER TEXT, $RSVP_STATUS INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS"); db?.execSQL("DROP TABLE IF EXISTS $TABLE_SCHEDULES")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_GROUPS"); db?.execSQL("DROP TABLE IF EXISTS $TABLE_MEMBERS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_RSVP"); onCreate(db)
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
        val exists = cursor.count > 0; cursor.close(); return exists
    }

    fun checkUser(input: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE (username = ? OR email = ?) AND password = ?", arrayOf(input, input, password))
        val exists = cursor.count > 0; cursor.close(); return exists
    }

    fun getUser(username: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE username = ?", arrayOf(username))
        val exists = cursor.count > 0; cursor.close(); return exists
    }

    // NEW: Search for users (partial match)
    fun searchUsers(keyword: String, excludeUser: String): ArrayList<String> {
        val list = ArrayList<String>()
        val db = this.readableDatabase
        // Search for usernames LIKE %keyword%, exclude current user
        val cursor = db.rawQuery("SELECT username FROM $TABLE_USERS WHERE username LIKE ? AND username != ?", arrayOf("%$keyword%", excludeUser))

        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // --- SCHEDULE METHODS (Keep existing) ---
    fun addSchedule(user: String, groupId: Int, title: String, date: String, location: String, desc: String, type: String): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(SCH_USERNAME, user); cv.put(SCH_GROUP_ID, groupId); cv.put(SCH_TITLE, title)
        cv.put(SCH_DATE, date); cv.put(SCH_LOCATION, location); cv.put(SCH_DESC, desc); cv.put(SCH_TYPE, type)
        return db.insert(TABLE_SCHEDULES, null, cv) != -1L
    }

    fun getSchedules(user: String, type: String): ArrayList<Schedule> {
        val list = ArrayList<Schedule>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SCHEDULES WHERE $SCH_TYPE = ? AND $SCH_USERNAME = ?", arrayOf(type, user))
        if (cursor.moveToFirst()) {
            do {
                list.add(Schedule(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), type))
            } while (cursor.moveToNext())
        }
        cursor.close(); return list
    }

    fun getGroupSchedules(groupId: Int): ArrayList<Schedule> {
        val list = ArrayList<Schedule>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SCHEDULES WHERE $SCH_GROUP_ID = ?", arrayOf(groupId.toString()))
        if (cursor.moveToFirst()) {
            do {
                val type = cursor.getString(cursor.getColumnIndexOrThrow(SCH_TYPE))
                list.add(Schedule(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), type))
            } while (cursor.moveToNext())
        }
        cursor.close(); return list
    }

    // --- GROUP METHODS (Keep existing) ---
    fun createGroupGetId(name: String, code: String, creator: String): Int {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put("group_name", name); cv.put("group_code", code)
        val groupId = db.insert(TABLE_GROUPS, null, cv)
        if (groupId != -1L) {
            val memberCv = ContentValues()
            memberCv.put("group_id", groupId); memberCv.put("username", creator)
            db.insert(TABLE_MEMBERS, null, memberCv)
            return groupId.toInt()
        }
        return -1
    }

    fun createGroup(name: String, code: String, creator: String): Boolean {
        return createGroupGetId(name, code, creator) != -1
    }

    fun joinGroup(user: String, code: String): Boolean {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT group_id FROM $TABLE_GROUPS WHERE group_code = ?", arrayOf(code))
        if (cursor.moveToFirst()) {
            val groupId = cursor.getInt(0); cursor.close()
            val check = db.rawQuery("SELECT * FROM $TABLE_MEMBERS WHERE group_id = ? AND username = ?", arrayOf(groupId.toString(), user))
            if (check.count > 0) { check.close(); return false }
            check.close()
            val cv = ContentValues(); cv.put("group_id", groupId); cv.put("username", user)
            return db.insert(TABLE_MEMBERS, null, cv) != -1L
        }
        cursor.close(); return false
    }

    fun addMemberToGroup(groupId: Int, username: String): Boolean {
        val db = this.writableDatabase
        val check = db.rawQuery("SELECT * FROM $TABLE_MEMBERS WHERE group_id = ? AND username = ?", arrayOf(groupId.toString(), username))
        if (check.count > 0) { check.close(); return false }
        check.close()
        val cv = ContentValues(); cv.put("group_id", groupId); cv.put("username", username)
        return db.insert(TABLE_MEMBERS, null, cv) != -1L
    }

    fun getUserGroups(user: String): ArrayList<GroupInfo> {
        val list = ArrayList<GroupInfo>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT g.group_id, g.group_name, g.group_code FROM $TABLE_GROUPS g INNER JOIN $TABLE_MEMBERS m ON g.group_id = m.group_id WHERE m.username = ?", arrayOf(user))
        if (cursor.moveToFirst()) {
            do { list.add(GroupInfo(cursor.getInt(0), cursor.getString(1), cursor.getString(2))) } while (cursor.moveToNext())
        }
        cursor.close(); return list
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

    fun updateRSVP(scheduleId: Int, user: String, status: Int) {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT $RSVP_ID FROM $TABLE_RSVP WHERE $RSVP_SCH_ID = ? AND $RSVP_USER = ?", arrayOf(scheduleId.toString(), user))
        val cv = ContentValues()
        cv.put(RSVP_SCH_ID, scheduleId); cv.put(RSVP_USER, user); cv.put(RSVP_STATUS, status)
        if (cursor.moveToFirst()) { db.update(TABLE_RSVP, cv, "$RSVP_SCH_ID = ? AND $RSVP_USER = ?", arrayOf(scheduleId.toString(), user)) }
        else { db.insert(TABLE_RSVP, null, cv) }
        cursor.close()
    }
}