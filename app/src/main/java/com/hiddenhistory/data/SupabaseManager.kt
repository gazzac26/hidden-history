package com.hiddenhistory.data

import android.content.Context
import com.hiddenhistory.BuildConfig
import com.hiddenhistory.adapter.SupabaseManagerAdapter
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.HttpTimeout

object SupabaseManager {

    private var _client:
        io.github.jan.supabase.SupabaseClient? =
        null

    val client:
        io.github.jan.supabase.SupabaseClient
        get() =
            _client
                ?: throw IllegalStateException(
                    "SupabaseClient not initialized! Make sure to call SupabaseManager.init(context) in your Application class."
                )

    @OptIn(
        io.github.jan.supabase.annotations.SupabaseInternal::class
    )
    fun init(
        context: Context
    ) {

        if (_client != null) {
            return
        }

        _client =
            createSupabaseClient(
                supabaseUrl =
                    BuildConfig.SUPABASE_URL,

                supabaseKey =
                    BuildConfig.SUPABASE_KEY
            ) {

                install(Auth) {

                    alwaysAutoRefresh =
                        true

                    sessionManager =
                        SupabaseManagerAdapter(
                            context
                        )
                }

                install(Postgrest)

                install(Realtime)

                install(Storage)

                install(Functions)

                httpConfig {

                    install(HttpTimeout) {

                        /*
                         * -------------------------------------------------
                         * SUPABASE EDGE FUNCTION REQUEST TIMEOUT
                         * -------------------------------------------------
                         *
                         * Advert analysis calls Gemini and can take
                         * longer than a normal database/API request.
                         *
                         * The previous 30 second timeout was causing:
                         *
                         * HttpRequestTimeoutException:
                         * Request timeout has expired
                         *
                         * at exactly 30000 ms.
                         *
                         * Allow the Edge Function up to 120 seconds
                         * to complete.
                         */

                        requestTimeoutMillis =
                            120_000L

                        /*
                         * -------------------------------------------------
                         * CONNECTION TIMEOUT
                         * -------------------------------------------------
                         *
                         * This only controls how long we wait to
                         * establish the connection.
                         */

                        connectTimeoutMillis =
                            30_000L

                        /*
                         * -------------------------------------------------
                         * SOCKET TIMEOUT
                         * -------------------------------------------------
                         *
                         * Allow the response connection to remain
                         * open while Gemini/Edge Function completes.
                         */

                        socketTimeoutMillis =
                            120_000L
                    }
                }
            }
    }
}