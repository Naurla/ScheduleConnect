package com.example.scheduleconnect

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

// --- Data Classes ---

data class UserDataModel(
    val username: String = "",
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val gender: String = "",
    val dob: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImageUrl: String = ""
)

data class Schedule(
    val id: Int = 0,
    val creator: String = "",
    val groupId: Int = -1,
    val title: String = "",
    val date: String = "",
    val location: String = "",
    val description: String = "",
    val type: String = "",
    val imageUrl: String = "",
    val status: String = "ACTIVE"
)

data class GroupInfo(
    val id: Int = 0,
    val name: String = "",
    val code: String = "",
    val imageUrl: String = ""
)

data class NotificationItem(
    val id: Int = 0,
    val username: String = "",
    val title: String = "",
    val message: String = "",
    val date: String = "",
    val isRead: Boolean = false,
    val relatedId: Int = -1,
    val type: String = ""
)

class DatabaseHelper(context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // ==========================================
    // USER METHODS
    // ==========================================

    fun checkUser(input: String, password: String, callback: (Boolean) -> Unit) {
        db.collection("users")
            .whereEqualTo("username", input)
            .whereEqualTo("password", password)
            .get()
            .addOnSuccessListener {
                if (!it.isEmpty) callback(true)
                else checkEmailUser(input, password, callback)
            }
            .addOnFailureListener { callback(false) }
    }

    private fun checkEmailUser(email: String, password: String, callback: (Boolean) -> Unit) {
        db.collection("users")
            .whereEqualTo("email", email)
            .whereEqualTo("password", password)
            .get()
            .addOnSuccessListener { callback(!it.isEmpty) }
            .addOnFailureListener { callback(false) }
    }

    fun checkUsernameOrEmail(input: String, callback: (Boolean) -> Unit) {
        db.collection("users").whereEqualTo("username", input).get().addOnSuccessListener {
            if (!it.isEmpty) callback(true)
            else db.collection("users").whereEqualTo("email", input).get().addOnSuccessListener { d -> callback(!d.isEmpty) }
        }
    }

    fun addUser(fName: String, mName: String, lName: String, gender: String, dob: String, email: String, phone: String, user: String, pass: String, callback: (Boolean) -> Unit) {
        val userData = hashMapOf(
            "first_name" to fName, "middle_name" to mName, "last_name" to lName,
            "gender" to gender, "dob" to dob, "email" to email, "phone" to phone,
            "username" to user, "password" to pass, "profile_image_url" to ""
        )
        db.collection("users").document(user).set(userData)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun getUsernameFromInput(input: String, callback: (String?) -> Unit) {
        db.collection("users").document(input).get().addOnSuccessListener {
            if (it.exists()) callback(input)
            else db.collection("users").whereEqualTo("email", input).get().addOnSuccessListener { d ->
                if (!d.isEmpty) callback(d.documents[0].id) else callback(null)
            }
        }
    }

    fun getUserDetails(username: String, callback: (UserDataModel?) -> Unit) {
        db.collection("users").document(username).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) callback(doc.toObject(UserDataModel::class.java))
                else callback(null)
            }
            .addOnFailureListener { callback(null) }
    }

    fun updateUserInfo(username: String, fName: String, mName: String, lName: String, gender: String, dob: String, phone: String, email: String, callback: (Boolean) -> Unit) {
        val updates = hashMapOf<String, Any>(
            "first_name" to fName, "middle_name" to mName, "last_name" to lName,
            "gender" to gender, "dob" to dob, "phone" to phone, "email" to email
        )
        db.collection("users").document(username).update(updates)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun updatePassword(identifier: String, newPass: String, isEmail: Boolean, callback: (Boolean) -> Unit) {
        val field = if (isEmail) "email" else "phone"
        db.collection("users").whereEqualTo(field, identifier).get().addOnSuccessListener {
            if (!it.isEmpty) {
                val id = it.documents[0].id
                db.collection("users").document(id).update("password", newPass)
                    .addOnSuccessListener { callback(true) }
                    .addOnFailureListener { callback(false) }
            } else callback(false)
        }
    }

    fun checkEmail(email: String, callback: (Boolean) -> Unit) {
        db.collection("users").whereEqualTo("email", email).get().addOnSuccessListener { callback(!it.isEmpty) }
    }

    fun checkPhone(phone: String, callback: (Boolean) -> Unit) {
        db.collection("users").whereEqualTo("phone", phone).get().addOnSuccessListener { callback(!it.isEmpty) }
    }

    fun searchUsers(keyword: String, excludeUser: String, callback: (ArrayList<String>) -> Unit) {
        db.collection("users").get().addOnSuccessListener { result ->
            val list = ArrayList<String>()
            for (doc in result) {
                val user = doc.id
                if (user.contains(keyword, ignoreCase = true) && user != excludeUser) {
                    list.add(user)
                }
            }
            callback(list)
        }
    }

    // ==========================================
    // IMAGE METHODS
    // ==========================================

    private fun uploadImage(path: String, data: ByteArray, onComplete: (String) -> Unit) {
        val ref = storage.reference.child(path)
        ref.putBytes(data).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { uri -> onComplete(uri.toString()) }
                .addOnFailureListener { onComplete("") }
        }.addOnFailureListener { onComplete("") }
    }

    fun updateProfilePicture(username: String, imageBytes: ByteArray, callback: (Boolean) -> Unit) {
        uploadImage("profiles/$username.png", imageBytes) { url ->
            if (url.isNotEmpty()) {
                db.collection("users").document(username).update("profile_image_url", url)
                    .addOnSuccessListener { callback(true) }
                    .addOnFailureListener { callback(false) }
            } else callback(false)
        }
    }

    fun getProfilePictureUrl(username: String, callback: (String) -> Unit) {
        db.collection("users").document(username).get()
            .addOnSuccessListener { callback(it.getString("profile_image_url") ?: "") }
            .addOnFailureListener { callback("") }
    }

    // ==========================================
    // SCHEDULE METHODS
    // ==========================================

    fun addSchedule(user: String, groupId: Int, title: String, date: String, loc: String, desc: String, type: String, imageBytes: ByteArray?, callback: (Boolean) -> Unit) {
        val id = System.currentTimeMillis().toInt()

        if (imageBytes != null) {
            uploadImage("schedules/$id.png", imageBytes) { url ->
                saveScheduleData(id, user, groupId, title, date, loc, desc, type, url, callback)
            }
        } else {
            saveScheduleData(id, user, groupId, title, date, loc, desc, type, "", callback)
        }
    }

    private fun saveScheduleData(id: Int, user: String, groupId: Int, title: String, date: String, loc: String, desc: String, type: String, imageUrl: String, callback: (Boolean) -> Unit) {
        val sch = Schedule(id, user, groupId, title, date, loc, desc, type, imageUrl, "ACTIVE")
        db.collection("schedules").document(id.toString()).set(sch)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun getSchedules(user: String, type: String, callback: (ArrayList<Schedule>) -> Unit) {
        val list = ArrayList<Schedule>()
        if (type == "personal") {
            db.collection("schedules")
                .whereEqualTo("username", user)
                .whereEqualTo("type", "personal")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener { res ->
                    for (d in res) list.add(d.toObject(Schedule::class.java))
                    callback(list)
                }
                .addOnFailureListener { callback(list) }
        } else {
            getUserGroups(user) { groups ->
                val groupIds = groups.map { it.id }
                if (groupIds.isEmpty()) { callback(list); return@getUserGroups }

                // Note: Firestore 'whereIn' supports max 10 items.
                db.collection("schedules")
                    .whereEqualTo("type", "shared")
                    .whereEqualTo("status", "ACTIVE")
                    .whereIn("group_id", groupIds)
                    .get()
                    .addOnSuccessListener { res ->
                        for (d in res) list.add(d.toObject(Schedule::class.java))
                        callback(list)
                    }
                    .addOnFailureListener { callback(list) }
            }
        }
    }

    fun getSchedule(id: Int, callback: (Schedule?) -> Unit) {
        db.collection("schedules").document(id.toString()).get()
            .addOnSuccessListener { callback(it.toObject(Schedule::class.java)) }
            .addOnFailureListener { callback(null) }
    }

    fun updateScheduleDetails(id: Int, title: String, date: String, loc: String, desc: String, imageBytes: ByteArray?, callback: (Boolean) -> Unit) {
        val updates = hashMapOf<String, Any>("title" to title, "date" to date, "location" to loc, "description" to desc)

        if (imageBytes != null) {
            uploadImage("schedules/$id.png", imageBytes) { url ->
                updates["imageUrl"] = url
                performUpdate(id, updates, callback)
            }
        } else {
            performUpdate(id, updates, callback)
        }
    }

    private fun performUpdate(id: Int, updates: Map<String, Any>, callback: (Boolean) -> Unit) {
        db.collection("schedules").document(id.toString()).update(updates)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun updateScheduleStatus(id: Int, status: String, callback: (Boolean) -> Unit) {
        db.collection("schedules").document(id.toString()).update("status", status)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun deleteSchedule(id: Int, callback: (Boolean) -> Unit) {
        db.collection("schedules").document(id.toString()).delete()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun getHistorySchedules(user: String, callback: (ArrayList<Schedule>) -> Unit) {
        db.collection("schedules")
            .whereEqualTo("username", user)
            .whereIn("status", listOf("FINISHED", "CANCELLED"))
            .get()
            .addOnSuccessListener { res ->
                val list = ArrayList<Schedule>()
                for (d in res) list.add(d.toObject(Schedule::class.java))
                callback(list)
            }
            .addOnFailureListener { callback(ArrayList()) }
    }

    // ==========================================
    // GROUP METHODS
    // ==========================================

    fun createGroup(name: String, code: String, creator: String, imageBytes: ByteArray?, callback: (Int) -> Unit) {
        val id = System.currentTimeMillis().toInt()

        if (imageBytes != null) {
            uploadImage("groups/$id.png", imageBytes) { url ->
                saveGroupData(id, name, code, url, creator, callback)
            }
        } else {
            saveGroupData(id, name, code, "", creator, callback)
        }
    }

    private fun saveGroupData(id: Int, name: String, code: String, imageUrl: String, creator: String, callback: (Int) -> Unit) {
        val group = GroupInfo(id, name, code, imageUrl)
        db.collection("groups").document(id.toString()).set(group).addOnSuccessListener {
            addMemberToGroup(id, creator) { callback(id) }
        }.addOnFailureListener { callback(-1) }
    }

    // Legacy method for compatibility if called elsewhere without callback
    // Not recommended for Firebase, but included to prevent crash if old code calls it
    fun createGroupGetId(name: String, code: String, creator: String, imageBytes: ByteArray?): Int {
        // This won't work synchronously with Firebase.
        // Returning -1 to force user to use the callback version (createGroup above)
        return -1
    }

    fun addMemberToGroup(groupId: Int, username: String, callback: (Boolean) -> Unit) {
        val member = hashMapOf("group_id" to groupId, "username" to username)
        db.collection("group_members").add(member)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun getGroupIdByCode(code: String, callback: (Int) -> Unit) {
        db.collection("groups").whereEqualTo("code", code).get().addOnSuccessListener {
            if (!it.isEmpty) {
                val group = it.documents[0].toObject(GroupInfo::class.java)
                callback(group?.id ?: -1)
            } else callback(-1)
        }
    }

    fun isUserInGroup(groupId: Int, username: String, callback: (Boolean) -> Unit) {
        db.collection("group_members")
            .whereEqualTo("group_id", groupId)
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { callback(!it.isEmpty) }
    }

    fun getUserGroups(user: String, callback: (ArrayList<GroupInfo>) -> Unit) {
        db.collection("group_members").whereEqualTo("username", user).get().addOnSuccessListener { members ->
            val ids = members.documents.mapNotNull { it.getLong("group_id")?.toInt() }
            if (ids.isEmpty()) { callback(ArrayList()); return@addOnSuccessListener }

            val groups = ArrayList<GroupInfo>()
            var count = 0
            for (id in ids) {
                db.collection("groups").document(id.toString()).get()
                    .addOnSuccessListener { doc ->
                        val g = doc.toObject(GroupInfo::class.java)
                        if (g != null) groups.add(g)
                        count++
                        if (count == ids.size) callback(groups)
                    }
                    .addOnFailureListener {
                        count++
                        if (count == ids.size) callback(groups)
                    }
            }
        }.addOnFailureListener { callback(ArrayList()) }
    }

    fun getGroupCreator(groupId: Int, callback: (String) -> Unit) {
        // In a real app, you'd store "creator" in the 'groups' document.
        // For now, let's assume the first member added or just return a placeholder.
        // Or you can query the first member in 'group_members'.
        // Returning 'Admin' as placeholder for now to prevent crashes.
        callback("Admin")
    }

    fun getGroupMemberUsernames(groupId: Int, exclude: String, callback: (ArrayList<String>) -> Unit) {
        db.collection("group_members").whereEqualTo("group_id", groupId).get().addOnSuccessListener {
            val list = ArrayList<String>()
            for (d in it) {
                val u = d.getString("username") ?: ""
                if (u.isNotEmpty() && u != exclude) list.add(u)
            }
            callback(list)
        }
    }

    fun leaveGroup(groupId: Int, username: String, callback: (Boolean) -> Unit) {
        db.collection("group_members")
            .whereEqualTo("group_id", groupId)
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener {
                for (d in it) d.reference.delete()
                callback(true)
            }
    }

    fun deleteGroup(groupId: Int, callback: (Boolean) -> Unit) {
        db.collection("groups").document(groupId.toString()).delete().addOnSuccessListener { callback(true) }
    }

    // ==========================================
    // NOTIFICATIONS & RSVP
    // ==========================================

    fun addNotification(user: String, title: String, msg: String, date: String, relatedId: Int = -1, type: String = "GENERAL") {
        val id = System.currentTimeMillis().toInt()
        val notif = NotificationItem(id, user, title, msg, date, false, relatedId, type)
        db.collection("notifications").document(id.toString()).set(notif)
    }

    fun getUserNotifications(user: String, callback: (ArrayList<NotificationItem>) -> Unit) {
        db.collection("notifications")
            .whereEqualTo("username", user)
            .get()
            .addOnSuccessListener { res ->
                val list = ArrayList<NotificationItem>()
                for (d in res) list.add(d.toObject(NotificationItem::class.java))
                callback(list)
            }
            .addOnFailureListener { callback(ArrayList()) }
    }

    fun markNotificationRead(id: Int) {
        db.collection("notifications").document(id.toString()).update("isRead", true)
    }

    fun getUnreadNotificationCount(user: String, callback: (Int) -> Unit) {
        db.collection("notifications")
            .whereEqualTo("username", user)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { callback(it.size()) }
            .addOnFailureListener { callback(0) }
    }

    // RSVP methods
    fun updateRSVP(schId: Int, user: String, status: Int) {
        val data = hashMapOf("scheduleId" to schId, "username" to user, "status" to status)
        val id = "${schId}_${user}"
        db.collection("rsvps").document(id).set(data)
    }

    fun getUserRSVPStatus(schId: Int, user: String, callback: (Int) -> Unit) {
        db.collection("rsvps").document("${schId}_${user}").get().addOnSuccessListener {
            callback(it.getLong("status")?.toInt() ?: 0)
        }
    }

    fun getScheduleAttendees(schId: Int, callback: (ArrayList<Map<String, String>>) -> Unit) {
        db.collection("rsvps").whereEqualTo("scheduleId", schId).get().addOnSuccessListener { res ->
            val list = ArrayList<Map<String, String>>()
            for (doc in res) {
                val u = doc.getString("username") ?: ""
                val s = doc.getLong("status")?.toInt() ?: 0
                val sStr = when(s) { 1->"GOING"; 2->"UNSURE"; 3->"NOT GOING"; else->"UNKNOWN" }
                list.add(mapOf("username" to u, "status" to sStr))
            }
            callback(list)
        }
    }
}