package com.hiddenhistory.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hiddenhistory.database.AppSettings
import com.hiddenhistory.database.SettingsDao
import com.hiddenhistory.notifications.HiddenHistoryNotificationManager
import com.hiddenhistory.notifications.NotificationType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDao: SettingsDao
) : ViewModel() {

    private val settingsFlow: StateFlow<AppSettings> =
        settingsDao.settingsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    val notificationsEnabled: StateFlow<Boolean> =
        settingsFlow
            .map { it.notificationsEnabled }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )

    val analysisWarningsEnabled: StateFlow<Boolean> =
        settingsFlow
            .map { it.analysisWarningsEnabled }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )

    fun setNotificationsEnabled(enabled: Boolean, context: Context? = null) {
        viewModelScope.launch {
            val current = settingsFlow.value
            settingsDao.saveSettings(current.copy(notificationsEnabled = enabled))

            if (enabled && context != null) {
                HiddenHistoryNotificationManager.show(
                    context = context,
                    type = NotificationType.ANNOUNCEMENT,
                    title = "Hidden History",
                    message = "Notifications are successfully enabled and configured."
                )
            }
        }
    }

    fun setAnalysisWarningsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsFlow.value
            settingsDao.saveSettings(current.copy(analysisWarningsEnabled = enabled))
        }
    }

    class Factory(private val settingsDao: SettingsDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(settingsDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
