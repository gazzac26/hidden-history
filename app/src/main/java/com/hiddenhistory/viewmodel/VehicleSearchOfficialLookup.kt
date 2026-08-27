package com.hiddenhistory.viewmodel

import android.util.Log
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.models.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class VehicleSearchOfficialLookup(
    private val client: OkHttpClient
) {

    suspend fun performLookup(cleanPlate: String): String {
        val session =
            SupabaseManager.client
                .auth
                .currentSessionOrNull()

        val userId =
            session
                ?.user
                ?.id

        if (userId == null) {
            Log.e(
                "VehicleSearch",
                "Search failed: User is not logged in."
            )
            throw IllegalStateException("Error: You must be logged in to search vehicles.")
        }

        try {

            SupabaseManager.client
                .postgrest["profiles"]
                .select {

                    filter {

                        eq(
                            "id",
                            userId
                        )
                    }
                }
                .decodeSingleOrNull<UserProfile>()

        } catch (profileError: Throwable) {

            Log.w(
                "VehicleSearch",
                "Profile fetch warning: ${profileError.message}"
            )
        }

        val payload =
            JSONObject().apply {

                put(
                    "registration",
                    cleanPlate
                )

                put(
                    "userId",
                    userId
                )

            }.toString()

        val accessToken =
            session.accessToken

        val body =
            payload.toRequestBody(
                "application/json".toMediaType()
            )

        val request =
            Request.Builder()
                .url(
                    "https://svnsylgpfqzbpevhjdfe.supabase.co/functions/v1/smooth-handler"
                )
                .addHeader(
                    "Authorization",
                    "Bearer ${accessToken ?: ""}"
                )
                .post(body)
                .build()

        client
            .newCall(request)
            .execute()
            .use { response ->

                val responseData =
                    response.body
                        ?.string()
                        ?: ""

                if (!response.isSuccessful) {

                    throw IOException(
                        "Server returned code ${response.code}: $responseData"
                    )
                }

                return responseData
            }
    }
}
