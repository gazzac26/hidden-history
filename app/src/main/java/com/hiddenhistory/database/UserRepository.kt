package com.hiddenhistory.database

import android.content.Context
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.models.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

class UserRepository private constructor() {

    // Temporary stub until AppDatabase and ProfileDao are ready
    fun getProfile(userId: String): Flow<UserProfile?> = flowOf(null)

    suspend fun refreshProfile(userId: String) = withContext(Dispatchers.IO) {
        try {
            val remoteProfile = SupabaseManager.client.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<UserProfile>()

            // TODO: Insert into local database once ProfileDao is added
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun isAuthenticated(): Boolean {
        return try {
            SupabaseManager.client.auth.currentUserOrNull() != null
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: UserRepository? = null

        fun getInstance(context: Context): UserRepository {
            return INSTANCE ?: synchronized(this) {
                // TODO: Re-enable AppDatabase.getDatabase(context) once ready
                val instance = UserRepository()
                INSTANCE = instance
                instance
            }
        }
    }
}
