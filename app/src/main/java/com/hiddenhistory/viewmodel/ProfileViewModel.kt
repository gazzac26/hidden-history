package com.hiddenhistory.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.database.ProfileDao
import com.hiddenhistory.models.RequirementCategory
import com.hiddenhistory.models.UserProfile
import com.hiddenhistory.repository.AnalysisRepository
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days

class ProfileViewModel(private val profileDao: ProfileDao) : ViewModel() {

    private val _saveResult = MutableStateFlow<Result<Unit>?>(null)
    val saveResult = _saveResult.asStateFlow()

    private val analysisRepository = AnalysisRepository(profileDao)

    init {
        // Automatically fetch and cache remote profile data when authenticated
        viewModelScope.launch {
            SupabaseManager.client.auth.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    refreshProfileFromNetwork()
                }
            }
        }
    }

    // Expose flow for reactive UI updates
    fun getProfileFlow(userId: String): Flow<UserProfile?> {
        return profileDao.getProfileFlow(userId)
    }

    suspend fun getLocalProfile(userId: String): UserProfile? {
        return profileDao.getProfile(userId)
    }

    suspend fun refreshProfileFromNetwork(): Result<UserProfile> {
        return try {
            val user = SupabaseManager.client.auth.currentUserOrNull() ?: return Result.failure(Exception("Not logged in"))

            val remoteProfile = SupabaseManager.client.from("profiles")
                .select { filter { eq("id", user.id) } }
                .decodeSingle<UserProfile>()

            profileDao.insertProfile(remoteProfile)

            Result.success(remoteProfile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getSignedUrl(path: String?): String? {
        if (path.isNullOrEmpty()) return null
        return try {
            SupabaseManager.client.storage.from("profile-images").createSignedUrl(path = path, expiresIn = 7.days)
        } catch (e: Exception) {
            null
        }
    }

    fun incrementMeetsAttended(currentProfile: UserProfile) {
        val updatedProfile = currentProfile.copy(
            meetsAttended = currentProfile.meetsAttended + 1,
            garageProgress = (currentProfile.garageProgress + 10).coerceAtMost(100)
        )
        updateProfile(updatedProfile)
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                val user = SupabaseManager.client.auth.currentUserOrNull() ?: throw Exception("Not logged in")
                profileDao.insertProfile(profile)

                val result = analysisRepository.translateAndSaveProfile(
                    userId = user.id,
                    currentProfile = profile,
                    rawRequirementsText = RequirementCategory().rawRequirements ?: ""
                )

                if (result.isSuccess) {
                    refreshProfileFromNetwork()
                    _saveResult.value = Result.success(Unit)
                } else {
                    _saveResult.value = Result.failure(result.exceptionOrNull() ?: Exception("Pipeline translation failed"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _saveResult.value = Result.failure(e)
            }
        }
    }

    fun resetSaveResult() {
        _saveResult.value = null
    }

    class Factory(private val profileDao: ProfileDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(profileDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
