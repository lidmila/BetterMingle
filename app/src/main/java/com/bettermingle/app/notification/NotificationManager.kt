package com.bettermingle.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bettermingle.app.MainActivity
import com.bettermingle.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

object NotificationChannels {
    const val EVENT_UPDATES = "event_updates"
    const val CHAT_MESSAGES = "chat_messages"
    const val POLLS = "polls"
    const val EXPENSES = "expenses"

    fun createAll(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as android.app.NotificationManager

        val channels = listOf(
            NotificationChannel(
                EVENT_UPDATES,
                "Novinky v akcích",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Upozornění na změny v tvých akcích" },

            NotificationChannel(
                CHAT_MESSAGES,
                "Zprávy",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Nové zprávy v chatu akce" },

            NotificationChannel(
                POLLS,
                "Hlasování",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Nové ankety a výsledky hlasování" },

            NotificationChannel(
                EXPENSES,
                "Výdaje",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Nové výdaje a vyrovnání" }
        )

        channels.forEach { manager.createNotificationChannel(it) }
    }
}

class BetterMingleMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Token is synced to Firestore when user logs in
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: "event_update"
        val title = data["title"] ?: message.notification?.title ?: "BetterMingle"
        val body = data["body"] ?: message.notification?.body ?: ""
        val eventId = data["eventId"]

        val channelId = when (type) {
            "chat" -> NotificationChannels.CHAT_MESSAGES
            "poll" -> NotificationChannels.POLLS
            "expense" -> NotificationChannels.EXPENSES
            else -> NotificationChannels.EVENT_UPDATES
        }

        showNotification(title, body, channelId, eventId)
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        eventId: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            eventId?.let { putExtra("eventId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this)
                .notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}
