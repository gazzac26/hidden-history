package com.hiddenhistory.database

import android.content.Context
import com.hiddenhistory.models.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ProfileDao(private val context: Context) {
    private val profileFile = File(context.filesDir, "user_profile.json")
    private val _profileFlow = MutableStateFlow<UserProfile?>(loadProfileSync())

    private fun loadProfileSync(): UserProfile? {
        return try {
            if (profileFile.exists()) {
                Json.decodeFromString<UserProfile>(profileFile.readText())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getProfileFlow(userId: String): Flow<UserProfile?> {
        return _profileFlow.asStateFlow()
    }

    suspend fun getProfile(userId: String): UserProfile? {
        return loadProfileSync()
    }

    suspend fun insertProfile(profile: UserProfile) {
        try {
            profileFile.writeText(Json.encodeToString(profile))
            _profileFlow.value = profile
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearAllProfiles() {
        try {
            if (profileFile.exists()) {
                profileFile.delete()
            }
            _profileFlow.value = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
