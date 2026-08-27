package com.hiddenhistory.repository

import android.util.Log
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.database.ProfileDao
import com.hiddenhistory.models.UserProfile
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnalysisRepository(private val profileDao: ProfileDao) {

    suspend fun translateAndSaveProfile(
        userId: String,
        currentProfile: UserProfile,
        rawRequirementsText: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Create a local copy of the profile with the new requirement text included
            val updatedProfile = currentProfile.copy(
                rawRequirements = rawRequirementsText,
                id = userId
            )

            // 1. Network Update (Supabase)
            SupabaseManager.client.from("profiles")
                .upsert(updatedProfile) {
                    onConflict = "id"
                }

            // 2. Local Update (File-based storage)
            profileDao.insertProfile(updatedProfile)

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(
                "PROFILE_SAVE",
                "Failed to save and sync profile",
                e
            )

            Result.failure(e)
        }
    }
}
