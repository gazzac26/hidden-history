package com.hiddenhistory.repository

import android.util.Log
import com.hiddenhistory.models.AdvertAnalysis
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AdvertRequestPayload(
    val advert: String,
    val registration: String? = null,
    val officialVehicleData: String? = null
)

class AdvertAnalysisRepository(
    private val supabaseClient: SupabaseClient
) {

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    suspend fun analyzeAdvert(
        advertText: String,
        registration: String? = null,
        officialVehicleData: String? = null
    ): AdvertAnalysis {

        return try {

            val response =
                supabaseClient.functions.invoke(
                    function = "analyse-advert",

                    body =
                        AdvertRequestPayload(
                            advert = advertText,
                            registration = registration,
                            officialVehicleData = officialVehicleData
                        )
                )

            val responseString =
                response.bodyAsText()

            json.decodeFromString<AdvertAnalysis>(
                responseString
            )

        } catch (e: Throwable) {

            Log.e(
                "AdvertAnalysisRepo",
                "Failed to analyze advert: ${e.message}",
                e
            )

            throw e
        }
    }
}