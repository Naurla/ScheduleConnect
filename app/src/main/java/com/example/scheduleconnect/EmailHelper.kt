package com.example.scheduleconnect

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailHelper {

    private const val SENDER_EMAIL = "scheduleconnect2025@gmail.com"
    private const val SENDER_PASSWORD = "zcml lkrm qeff xayy" // Keep your app password here

    fun sendEmail(recipients: List<String>, subject: String, body: String) {
        if (recipients.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val props = Properties()
                props["mail.smtp.auth"] = "true"
                props["mail.smtp.starttls.enable"] = "true"
                props["mail.smtp.host"] = "smtp.gmail.com"
                props["mail.smtp.port"] = "587"

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                    }
                })

                val message = MimeMessage(session)
                message.setFrom(InternetAddress(SENDER_EMAIL, "ScheduleConnect Team"))

                // Add all recipients
                val addressArray = recipients.map { InternetAddress(it) }.toTypedArray()
                message.setRecipients(Message.RecipientType.TO, addressArray)

                message.subject = subject
                message.setText(body)

                Transport.send(message)
                println("Email sent successfully to ${recipients.size} recipients.")

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}