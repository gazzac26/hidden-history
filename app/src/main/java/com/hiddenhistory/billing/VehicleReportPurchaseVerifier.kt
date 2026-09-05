package com.hiddenhistory.billing

import com.hiddenhistory.data.SupabaseManager
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Verifies a Google Play vehicle-report purchase through the
 * authenticated Supabase Edge Function.
 *
 * IMPORTANT:
 * Google Play Billing's PURCHASED callback is NOT treated as
 * entitlement by the Android app.
 *
 * The purchase token is sent to Supabase, where the Edge Function:
 *
 * Google Play purchase token
 *          ↓
 * Google Play Developer API verification
 *          ↓
 * purchase confirmed
 *          ↓
 * grant_vehicle_report_token()
 *          ↓
 * Google Play purchase consumed
 *
 * Only a successful response from the Edge Function is considered
 * confirmation that the Hidden History report credit was granted.
 */
class VehicleReportPurchaseVerifier {

    private val supabase
        get() = SupabaseManager.client

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    suspend fun verifyAndGrant(
        purchaseToken: String
    ): Result<String> {

        return try {

            if (
                purchaseToken.isBlank()
            ) {

                return Result.failure(
                    IllegalArgumentException(
                        "The Google Play purchase token is empty."
                    )
                )
            }

            val response =
                supabase
                    .functions
                    .invoke(
                        function =
                            "verify-vehicle-report-purchase",

                        body =
                            mapOf(
                                "purchaseToken" to purchaseToken
                            )
                    )

            val responseBody =
                response.bodyAsText()

            if (
                responseBody.isBlank()
            ) {

                return Result.failure(
                    IllegalStateException(
                        "The purchase verification service returned an empty response."
                    )
                )
            }

            val responseJson =
                json
                    .parseToJsonElement(
                        responseBody
                    )
                    .jsonObject

            val success =
                responseJson["success"]
                    ?.jsonPrimitive
                    ?.content
                    ?.toBooleanStrictOrNull()
                    ?: false

            if (!success) {

                val error =
                    responseJson["error"]
                        ?.jsonPrimitive
                        ?.content
                        ?: "Google Play purchase verification failed."

                return Result.failure(
                    IllegalStateException(
                        error
                    )
                )
            }

            val tokenId =
                responseJson["tokenId"]
                    ?.jsonPrimitive
                    ?.content
                    .orEmpty()

            if (
                tokenId.isBlank()
            ) {

                return Result.failure(
                    IllegalStateException(
                        "Purchase was verified but no Hidden History report token was returned."
                    )
                )
            }

            Result.success(
                tokenId
            )

        } catch (
            e: Exception
        ) {

            Result.failure(
                e
            )
        }
    }
}