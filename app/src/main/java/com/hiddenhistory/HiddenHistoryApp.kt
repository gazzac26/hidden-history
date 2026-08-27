package com.hiddenhistory

import android.app.Application
import com.hiddenhistory.data.SupabaseManager

class HiddenHistoryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // This initializes Supabase when the app starts up!
        SupabaseManager.init(this)
    }
}
