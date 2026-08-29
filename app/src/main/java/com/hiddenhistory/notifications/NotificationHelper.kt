package com.hiddenhistory.notifications

import android.content.Context
import com.hiddenhistory.database.SettingsDao
import kotlinx.coroutines.flow.first

class NotificationHelper(
    private val settingsDao: SettingsDao
) {
    suspend fun showVehicleReportReady(context: Context, vehicleRegistration: String, reportId: String) {
        val settings = settingsDao.settingsFlow.first()
        if (!settings.notificationsEnabled) {
            return
        }

        HiddenHistoryNotificationManager.show(
            context = context,
            type = NotificationType.VEHICLE_REPORT,
            title = "Vehicle Report Ready",
            message = "Analysis for $vehicleRegistration is complete and ready to view.",
            notificationId = reportId.hashCode(),
            targetRoute = "vehicle_report/$reportId"
        )
    }
}
