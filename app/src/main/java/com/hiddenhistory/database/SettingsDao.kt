package com.hiddenhistory.database

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val analysisWarningsEnabled: Boolean = true
)

class SettingsDao(private val context: Context) {
    private val settingsFile = File(context.filesDir, "app_settings.json")
    
    private val _settingsFlow = MutableStateFlow(loadSettingsSync())
    val settingsFlow: Flow<AppSettings> = _settingsFlow.asStateFlow()

    private fun loadSettingsSync(): AppSettings {
        return try {
            if (settingsFile.exists()) {
                Json.decodeFromString<AppSettings>(settingsFile.readText())
            } else {
                AppSettings()
            }
        } catch (e: Exception) {
            AppSettings()
        }
    }

    suspend fun saveSettings(settings: AppSettings) {
        try {
            settingsFile.writeText(Json.encodeToString(settings))
            _settingsFlow.value = settings
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
