package com.hiddenhistory.adapter

import android.content.Context
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class SupabaseManagerAdapter(context: Context) : SessionManager {
    private val prefs = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)

    override suspend fun saveSession(session: UserSession) {
        val json = Json.encodeToString(session)
        prefs.edit().putString("session", json).apply()
    }

    override suspend fun loadSession(): UserSession? {
        val json = prefs.getString("session", null) ?: return null
        return try {
            Json.decodeFromString<UserSession>(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun deleteSession() {
        prefs.edit().remove("session").apply()
    }
}
