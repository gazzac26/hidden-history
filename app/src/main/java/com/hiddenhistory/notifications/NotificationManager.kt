package com.hiddenhistory.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hiddenhistory.MainActivity

enum class NotificationType(
    val channelId: String,
    val channelName: String,
    val defaultImportance: Int
) {
    APP_UPDATE(
        "hidden_history_updates",
        "Hidden History Updates",
        NotificationManager.IMPORTANCE_DEFAULT
    ),
    SUBSCRIPTION_REMINDER(
        "hidden_history_subscriptions",
        "Subscriptions & Billing",
        NotificationManager.IMPORTANCE_DEFAULT
    ),
    VEHICLE_REPORT(
        "hidden_history_reports",
        "Vehicle Reports",
        NotificationManager.IMPORTANCE_HIGH
    ),
    COMMUNITY(
        "hidden_history_community",
        "Community Activity",
        NotificationManager.IMPORTANCE_DEFAULT
    ),
    MARKETPLACE(
        "hidden_history_marketplace",
        "Marketplace & Offers",
        NotificationManager.IMPORTANCE_LOW
    ),
    ANNOUNCEMENT(
        "hidden_history_announcements",
        "Announcements",
        NotificationManager.IMPORTANCE_HIGH
    )
}

object HiddenHistoryNotificationManager {

    private const val DEFAULT_NOTIFICATION_ID = 2001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            NotificationType.values().forEach { type ->
                val channel = NotificationChannel(
                    type.channelId,
                    type.channelName,
                    type.defaultImportance
                ).apply {
                    description = "Notifications for ${type.channelName.lowercase()}"
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun show(
        context: Context,
        type: NotificationType,
        title: String,
        message: String,
        notificationId: Int = DEFAULT_NOTIFICATION_ID,
        targetRoute: String? = null
    ) {
        createNotificationChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (!targetRoute.isNullOrBlank()) {
                putExtra("EXTRA_TARGET_ROUTE", targetRoute)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, type.channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
}
