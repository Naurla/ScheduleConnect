package com.example.scheduleconnect

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NotificationWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "Schedule Reminder"
        val message = inputData.getString("message") ?: "You have an upcoming schedule!"

        // --- NEW: Retrieve Group Details ---
        val groupId = inputData.getInt("group_id", -1)
        val groupName = inputData.getString("group_name") ?: "Group Chat"

        // 1. Check Preferences
        val sharedPref = applicationContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val remindersEnabled = sharedPref.getBoolean("SCHEDULE_REMINDERS_ENABLED", true)
        val dndEnabled = sharedPref.getBoolean("DND_ENABLED", false)

        // If Reminders are completely disabled, do nothing.
        if (!remindersEnabled) {
            return Result.success()
        }

        // 2. Handle DND Logic
        if (dndEnabled) {
            // DND is ON: Send delayed email instead of App Notification
            sendDelayedEmail(title, message)
        } else {
            // Normal Mode: Show App Notification
            // Pass group details to the notification helper
            showNotification(title, message, groupId, groupName)
        }

        return Result.success()
    }

    private fun showNotification(title: String, message: String, groupId: Int, groupName: String) {
        val channelId = "schedule_channel"
        val notificationId = System.currentTimeMillis().toInt()
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Schedule Notifications"
            val descriptionText = "Channel for schedule reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        // --- NEW: Create Intent for Redirection ---
        val intent = Intent(applicationContext, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "CHAT")
            putExtra("GROUP_ID", groupId)
            putExtra("GROUP_NAME", groupName)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure you have this icon or change to R.drawable.ic_home
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // Attach the pending intent
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private suspend fun sendDelayedEmail(title: String, message: String) {
        // Retrieve current user username
        val userPref = applicationContext.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val username = userPref.getString("USERNAME", "") ?: ""

        if (username.isNotEmpty()) {
            val dbHelper = DatabaseHelper(applicationContext)

            // --- ASYNC WRAPPER ---
            val userDetails = suspendCoroutine<UserDataModel?> { continuation ->
                dbHelper.getUserDetails(username) { user ->
                    continuation.resume(user)
                }
            }

            if (userDetails != null && userDetails.email.isNotEmpty()) {
                val emailBody = "Hello ${userDetails.firstName},\n\nYou have missed a notification because 'Do Not Disturb' is on.\n\nNotification: $title\nDetails: $message\n\nRegards,\nScheduleConnect Team"

                // Send Email
                EmailHelper.sendEmail(listOf(userDetails.email), "Missed Notification: $title", emailBody)
            }
        }
    }
}
