package com.example.scheduleconnect

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.ArrayList

// --- Data Classes ---
data class UserDataModel(
    val username: String,
    val firstName: String,
    val middleName: String,
    val lastName: String,
    val gender: String,
    val dob: String,
    val email: String,
    val phone: String
)

data class Schedule(
    val id: Int,
    val creator: String,
    val groupId: Int,
    val title: String,
    val date: String,
    val location: String,
    val description: String,
    val type: String,
    val image: ByteArray?,
    val status: String = "ACTIVE"
)

data class GroupInfo(val id: Int, val name: String, val code: String)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ScheduleConnect.db"
        private const val DATABASE_VERSION = 14

        // Table Names
        private const val TABLE_USERS = "users"
        private const val TABLE_SCHEDULES = "schedules"
        private const val TABLE_GROUPS = "schedule_groups"
        private const val TABLE_MEMBERS = "group_members"
        private const val TABLE_RSVP = "rsvps"
        private const val TABLE_NOTIFICATIONS = "notifications"

        // Columns
        private const val COL_PROFILE_IMG = "profile_image"
        private const val COL_GROUP_IMG = "group_image"
        private const val COL_GROUP_CREATOR = "group_creator"

        // Schedule Columns
        const val SCH_ID = "schedule_id"
        const val SCH_USERNAME = "username"
        const val SCH_GROUP_ID = "group_id"
        const val SCH_TITLE = "title"
        const val SCH_DATE = "date"
        const val SCH_LOCATION = "location"
        const val SCH_DESC = "description"
        const val SCH_TYPE = "type"
        const val SCH_IMAGE = "schedule_image"
        const val SCH_STATUS = "status"

        // RSVP Columns
        const val RSVP_ID = "rsvp_id"
        const val RSVP_SCH_ID = "schedule_id"
        const val RSVP_USER = "username"
        const val RSVP_STATUS = "status"

        // Notification Columns
        private const val COL_NOTIF_ID = "notif_id"
        private const val COL_NOTIF_USER = "username"
        private const val COL_NOTIF_TITLE = "title"
        private const val COL_NOTIF_MSG = "message"
        private const val COL_NOTIF_DATE = "date"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE $TABLE_USERS (id INTEGER PRIMARY KEY AUTOINCREMENT, first_name TEXT, middle_name TEXT, last_name TEXT, gender TEXT, dob TEXT, email TEXT, phone TEXT, username TEXT, password TEXT, $COL_PROFILE_IMG BLOB)")
        db?.execSQL("CREATE TABLE $TABLE_SCHEDULES ($SCH_ID INTEGER PRIMARY KEY AUTOINCREMENT, $SCH_USERNAME TEXT, $SCH_GROUP_ID INTEGER DEFAULT -1, $SCH_TITLE TEXT, $SCH_DATE TEXT, $SCH_LOCATION TEXT, $SCH_DESC TEXT, $SCH_TYPE TEXT, $SCH_IMAGE BLOB, $SCH_STATUS TEXT DEFAULT 'ACTIVE')")
        db?.execSQL("CREATE TABLE $TABLE_GROUPS (group_id INTEGER PRIMARY KEY AUTOINCREMENT, group_name TEXT, group_code TEXT, $COL_GROUP_CREATOR TEXT, $COL_GROUP_IMG BLOB)")
        db?.execSQL("CREATE TABLE $TABLE_MEMBERS (member_id INTEGER PRIMARY KEY AUTOINCREMENT, group_id INTEGER, username TEXT)")
        db?.execSQL("CREATE TABLE $TABLE_RSVP ($RSVP_ID INTEGER PRIMARY KEY AUTOINCREMENT, $RSVP_SCH_ID INTEGER, $RSVP_USER TEXT, $RSVP_STATUS INTEGER)")
        db?.execSQL("CREATE TABLE $TABLE_NOTIFICATIONS ($COL_NOTIF_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_NOTIF_USER TEXT, $COL_NOTIF_TITLE TEXT, $COL_NOTIF_MSG TEXT, $COL_NOTIF_DATE TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // --- FIX: Drop old tables and recreate them to ensure schema matches ---
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_SCHEDULES")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_GROUPS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_MEMBERS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_RSVP")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NOTIFICATIONS")
        onCreate(db)
    }

    // --- USER METHODS ---
    fun checkUser(input: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE (username = ? OR email = ?) AND password = ?", arrayOf(input, input, password))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun getUsernameFromInput(input: String): String? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT username FROM $TABLE_USERS WHERE username = ? OR email = ?", arrayOf(input, input))
        var foundUsername: String? = null
        if (cursor.moveToFirst()) {
            foundUsername = cursor.getString(0)
        }
        cursor.close()
        return foundUsername
    }

    fun addUser(fName: String, mName: String, lName: String, gender: String, dob: String, email: String, phone: String, user: String, pass: String): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put("first_name", fName); cv.put("middle_name", mName); cv.put("last_name", lName)
        cv.put("gender", gender); cv.put("dob", dob); cv.put("email", email)
        cv.put("phone", phone); cv.put("username", user); cv.put("password", pass)
        return db.insert(TABLE_USERS, null, cv) != -1L
    }

    fun getUserDetails(username: String): UserDataModel? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE username = ?", arrayOf(username))
        var user: UserDataModel? = null
        if (cursor.moveToFirst()) {
            try {
                user = UserDataModel(
                    cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    cursor.getString(cursor.getColumnIndexOrThrow("first_name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("middle_name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("last_name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("gender")),
                    cursor.getString(cursor.getColumnIndexOrThrow("dob")),
                    cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    cursor.getString(cursor.getColumnIndexOrThrow("phone"))
                )
            } catch (e: Exception) {
                user = UserDataModel(username, "", "", "", "", "", "", "")
            }
        }
        cursor.close()
        return user
    }

    fun updateUserInfo(currentUsername: String, fName: String, mName: String, lName: String, gender: String, dob: String, phone: String, email: String): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put("first_name", fName)
        cv.put("middle_name", mName)
        cv.put("last_name", lName)
        cv.put("gender", gender)
        cv.put("dob", dob)
        cv.put("phone", phone)
        cv.put("email", email)
        val result = db.update(TABLE_USERS, cv, "username = ?", arrayOf(currentUsername))
        return result != -1
    }

    fun updatePassword(identifier: String, newPass: String, isEmail: Boolean): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put("password", newPass)
        val whereClause = if (isEmail) "email = ?" else "phone = ?"
        val result = db.update(TABLE_USERS, cv, whereClause, arrayOf(identifier))
        return result != -1
    }

    fun checkUsernameOrEmail(input: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE username = ? OR email = ?", arrayOf(input, input))
        val exists = cursor.count > 0; cursor.close(); return exists
    }

    fun checkEmail(email: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE email = ?", arrayOf(email))
        val exists = cursor.count > 0; cursor.close(); return exists
    }

    fun checkPhone(phone: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE phone = ?", arrayOf(phone))
        val exists = cursor.count > 0; cursor.close(); return exists
    }

    fun searchUsers(keyword: String, excludeUser: String): ArrayList<String> {
        val list = ArrayList<String>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT username FROM $TABLE_USERS WHERE username LIKE ? AND username != ?", arrayOf("%$keyword%", excludeUser))
        if (cursor.moveToFirst()) {
            do { list.add(cursor.getString(0)) } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // --- PROFILE IMAGE METHODS ---
    fun hasProfilePicture(username: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COL_PROFILE_IMG FROM $TABLE_USERS WHERE username = ?", arrayOf(username))
        var hasImage = false
        if (cursor.moveToFirst()) {
            val img = cursor.getBlob(0)
            hasImage = img != null && img.isNotEmpty()
        }
        cursor.close()
        return hasImage
    }

    fun getProfilePicture(username: String): Bitmap? {
        val db = this.readableDatabase
        var bitmap: Bitmap? = null
        try {
            val cursor = db.rawQuery("SELECT $COL_PROFILE_IMG FROM $TABLE_USERS WHERE username = ?", arrayOf(username))
            if (cursor.moveToFirst()) {
                val img = cursor.getBlob(0)
                if (img != null && img.isNotEmpty()) {
                    bitmap = BitmapFactory.decodeByteArray(img, 0, img.size)
                }
            }
            cursor.close()
        } catch (e: Exception) { e.printStackTrace() }
        return bitmap
    }

    fun updateProfilePicture(username: String, imageBytes: ByteArray): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(COL_PROFILE_IMG, imageBytes)
        val result = db.update(TABLE_USERS, cv, "username = ?", arrayOf(username))
        return result != -1
    }

    fun updateUsername(currentName: String, newName: String): Boolean {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            val checkCursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE username = ?", arrayOf(newName))
            if (checkCursor.count > 0) {
                checkCursor.close()
                return false
            }
            checkCursor.close()

            val cvUser = ContentValues(); cvUser.put("username", newName)
            db.update(TABLE_USERS, cvUser, "username = ?", arrayOf(currentName))

            val cvSch = ContentValues(); cvSch.put(SCH_USERNAME, newName)
            db.update(TABLE_SCHEDULES, cvSch, "$SCH_USERNAME = ?", arrayOf(currentName))

            val cvMem = ContentValues(); cvMem.put("username", newName)
            db.update(TABLE_MEMBERS, cvMem, "username = ?", arrayOf(currentName))

            val cvRsvp = ContentValues(); cvRsvp.put(RSVP_USER, newName)
            db.update(TABLE_RSVP, cvRsvp, "$RSVP_USER = ?", arrayOf(currentName))

            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) { return false } finally { db.endTransaction() }
    }

    // --- SCHEDULE METHODS ---
    fun addSchedule(user: String, groupId: Int, title: String, date: String, location: String, desc: String, type: String, image: ByteArray? = null): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(SCH_USERNAME, user); cv.put(SCH_GROUP_ID, groupId); cv.put(SCH_TITLE, title)
        cv.put(SCH_DATE, date); cv.put(SCH_LOCATION, location); cv.put(SCH_DESC, desc); cv.put(SCH_TYPE, type)
        cv.put(SCH_STATUS, "ACTIVE")
        if (image != null) cv.put(SCH_IMAGE, image)
        return db.insert(TABLE_SCHEDULES, null, cv) != -1L
    }

    fun getSchedules(user: String, type: String): ArrayList<Schedule> {
        val list = ArrayList<Schedule>()
        val db = this.readableDatabase
        val cursor: Cursor
        if (type == "shared") {
            val query = "SELECT s.* FROM $TABLE_SCHEDULES s INNER JOIN $TABLE_MEMBERS m ON s.$SCH_GROUP_ID = m.group_id WHERE s.$SCH_TYPE = ? AND m.username = ? AND s.$SCH_STATUS = 'ACTIVE'"
            cursor = db.rawQuery(query, arrayOf("shared", user))
        } else {
            cursor = db.rawQuery("SELECT * FROM $TABLE_SCHEDULES WHERE $SCH_TYPE = ? AND $SCH_USERNAME = ? AND $SCH_STATUS = 'ACTIVE'", arrayOf(type, user))
        }
        if (cursor.moveToFirst()) {
            do { list.add(cursorToSchedule(cursor, type)) } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getAllHistorySchedules(user: String): ArrayList<Schedule> {
        val list = ArrayList<Schedule>()
        val db = this.readableDatabase
        val query = "SELECT DISTINCT s.* FROM $TABLE_SCHEDULES s LEFT JOIN $TABLE_MEMBERS m ON s.$SCH_GROUP_ID = m.group_id WHERE (s.$SCH_USERNAME = ? OR m.username = ?)"
        val cursor = db.rawQuery(query, arrayOf(user, user))
        if (cursor.moveToFirst()) {
            do {
                val type = cursor.getString(cursor.getColumnIndexOrThrow(SCH_TYPE))
                list.add(cursorToSchedule(cursor, type))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    private fun cursorToSchedule(cursor: Cursor, type: String): Schedule {
        val imgIndex = cursor.getColumnIndex(SCH_IMAGE)
        val imgBytes = if (imgIndex != -1) cursor.getBlob(imgIndex) else null
        val statusIndex = cursor.getColumnIndex(SCH_STATUS)
        val status = if (statusIndex != -1) cursor.getString(statusIndex) else "ACTIVE"
        return Schedule(
            cursor.getInt(0), cursor.getString(1), cursor.getInt(2), cursor.getString(3),
            cursor.getString(4), cursor.getString(5), cursor.getString(6), type, imgBytes, status
        )
    }

    fun getSchedule(scheduleId: Int): Schedule? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SCHEDULES WHERE $SCH_ID = ?", arrayOf(scheduleId.toString()))
        var schedule: Schedule? = null
        if (cursor.moveToFirst()) {
            val type = cursor.getString(cursor.getColumnIndexOrThrow(SCH_TYPE))
            schedule = cursorToSchedule(cursor, type)
        }
        cursor.close()
        return schedule
    }

    fun updateScheduleStatus(scheduleId: Int, newStatus: String): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(SCH_STATUS, newStatus)
        return db.update(TABLE_SCHEDULES, cv, "$SCH_ID = ?", arrayOf(scheduleId.toString())) > 0
    }

    fun updateScheduleDetails(id: Int, title: String, date: String, location: String, desc: String, image: ByteArray?): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(SCH_TITLE, title); cv.put(SCH_DATE, date); cv.put(SCH_LOCATION, location); cv.put(SCH_DESC, desc)
        if (image != null) cv.put(SCH_IMAGE, image)
        return db.update(TABLE_SCHEDULES, cv, "$SCH_ID = ?", arrayOf(id.toString())) > 0
    }

    fun deleteSchedule(scheduleId: Int): Boolean {
        val db = this.writableDatabase
        db.delete(TABLE_RSVP, "$RSVP_SCH_ID = ?", arrayOf(scheduleId.toString()))
        return db.delete(TABLE_SCHEDULES, "$SCH_ID = ?", arrayOf(scheduleId.toString())) > 0
    }

    // --- GROUP METHODS ---
    fun createGroupGetId(name: String, code: String, creator: String, image: ByteArray? = null): Int {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put("group_name", name); cv.put("group_code", code); cv.put(COL_GROUP_CREATOR, creator)
        if (image != null) cv.put(COL_GROUP_IMG, image)
        val id = db.insert(TABLE_GROUPS, null, cv)
        if (id != -1L) {
            addMemberToGroup(id.toInt(), creator)
            return id.toInt()
        }
        return -1
    }

    fun addMemberToGroup(groupId: Int, username: String): Boolean {
        val db = this.writableDatabase
        val check = db.rawQuery("SELECT * FROM $TABLE_MEMBERS WHERE group_id = ? AND username = ?", arrayOf(groupId.toString(), username))
        if(check.count > 0) { check.close(); return false }
        check.close()

        val cv = ContentValues()
        cv.put("group_id", groupId); cv.put("username", username)
        return db.insert(TABLE_MEMBERS, null, cv) != -1L
    }

    fun getGroupIdByCode(code: String): Int {
        val db = this.readableDatabase
        var id = -1
        val cursor = db.rawQuery("SELECT group_id FROM $TABLE_GROUPS WHERE group_code = ?", arrayOf(code))
        if (cursor.moveToFirst()) id = cursor.getInt(0)
        cursor.close()
        return id
    }

    fun isUserInGroup(groupId: Int, username: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MEMBERS WHERE group_id = ? AND username = ?", arrayOf(groupId.toString(), username))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun getGroupName(groupId: Int): String {
        val db = this.readableDatabase
        var name = ""
        val cursor = db.rawQuery("SELECT group_name FROM $TABLE_GROUPS WHERE group_id = ?", arrayOf(groupId.toString()))
        if (cursor.moveToFirst()) name = cursor.getString(0)
        cursor.close()
        return name
    }

    fun getGroupCreator(groupId: Int): String {
        val db = this.readableDatabase
        var creator = ""
        try {
            val cursor = db.rawQuery("SELECT $COL_GROUP_CREATOR FROM $TABLE_GROUPS WHERE group_id = ?", arrayOf(groupId.toString()))
            if (cursor.moveToFirst()) creator = cursor.getString(0) ?: ""
            cursor.close()
        } catch (e: Exception) { e.printStackTrace() }
        return creator
    }

    fun getUserGroups(user: String): ArrayList<GroupInfo> {
        val list = ArrayList<GroupInfo>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT g.group_id, g.group_name, g.group_code FROM $TABLE_GROUPS g INNER JOIN $TABLE_MEMBERS m ON g.group_id = m.group_id WHERE m.username = ?", arrayOf(user))
        if (cursor.moveToFirst()) {
            do { list.add(GroupInfo(cursor.getInt(0), cursor.getString(1), cursor.getString(2))) } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getGroupMemberUsernames(groupId: Int, excludeUser: String): ArrayList<String> {
        val list = ArrayList<String>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT username FROM $TABLE_MEMBERS WHERE group_id = ? AND username != ?", arrayOf(groupId.toString(), excludeUser))
        if (cursor.moveToFirst()) {
            do { list.add(cursor.getString(0)) } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getGroupMemberEmails(groupId: Int, excludeUser: String): ArrayList<String> {
        val list = ArrayList<String>()
        val db = this.readableDatabase
        val query = "SELECT u.email FROM $TABLE_USERS u INNER JOIN $TABLE_MEMBERS m ON u.username = m.username WHERE m.group_id = ? AND u.username != ?"
        val cursor = db.rawQuery(query, arrayOf(groupId.toString(), excludeUser))
        if (cursor.moveToFirst()) {
            do { list.add(cursor.getString(0)) } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // --- RSVP & NOTIFICATIONS ---
    fun updateRSVP(scheduleId: Int, user: String, status: Int) {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT $RSVP_ID FROM $TABLE_RSVP WHERE $RSVP_SCH_ID = ? AND $RSVP_USER = ?", arrayOf(scheduleId.toString(), user))
        val cv = ContentValues()
        cv.put(RSVP_SCH_ID, scheduleId); cv.put(RSVP_USER, user); cv.put(RSVP_STATUS, status)
        if (cursor.moveToFirst()) db.update(TABLE_RSVP, cv, "$RSVP_SCH_ID = ? AND $RSVP_USER = ?", arrayOf(scheduleId.toString(), user))
        else db.insert(TABLE_RSVP, null, cv)
        cursor.close()
    }

    fun getUserRSVPStatus(scheduleId: Int, username: String): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $RSVP_STATUS FROM $TABLE_RSVP WHERE $RSVP_SCH_ID = ? AND $RSVP_USER = ?", arrayOf(scheduleId.toString(), username))
        var status = 0
        if (cursor.moveToFirst()) status = cursor.getInt(0)
        cursor.close()
        return status
    }

    fun getScheduleAttendees(scheduleId: Int): ArrayList<Map<String, String>> {
        val list = ArrayList<Map<String, String>>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $RSVP_USER, $RSVP_STATUS FROM $TABLE_RSVP WHERE $RSVP_SCH_ID = ?", arrayOf(scheduleId.toString()))
        if (cursor.moveToFirst()) {
            do {
                val user = cursor.getString(0)
                val statusInt = cursor.getInt(1)
                val statusStr = when (statusInt) { 1 -> "GOING"; 2 -> "UNSURE"; 3 -> "NOT GOING"; else -> "UNKNOWN" }
                val map = HashMap<String, String>()
                map["username"] = user; map["status"] = statusStr
                list.add(map)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun addNotification(user: String, title: String, message: String, date: String): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(COL_NOTIF_USER, user); cv.put(COL_NOTIF_TITLE, title); cv.put(COL_NOTIF_MSG, message); cv.put(COL_NOTIF_DATE, date)
        return db.insert(TABLE_NOTIFICATIONS, null, cv) != -1L
    }

    fun getUserNotifications(user: String): ArrayList<Map<String, String>> {
        val list = ArrayList<Map<String, String>>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NOTIFICATIONS WHERE $COL_NOTIF_USER = ? ORDER BY $COL_NOTIF_ID DESC", arrayOf(user))
        if (cursor.moveToFirst()) {
            do {
                val map = HashMap<String, String>()
                map["title"] = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_TITLE))
                map["message"] = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_MSG))
                map["date"] = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_DATE))
                list.add(map)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}