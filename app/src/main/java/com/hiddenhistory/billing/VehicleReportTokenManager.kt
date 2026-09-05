package com.hiddenhistory.billing

import com.hiddenhistory.data.SupabaseManager
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class VehicleReportTokenManager {

    private val supabase
        get() = SupabaseManager.client

    /*
     * =========================================================
     * GET AVAILABLE TOKEN COUNT
     * =========================================================
     */

    suspend fun getAvailableTokenCount(): Result<Int> {

        return try {

            val count =
                supabase
                    .postgrest
                    .rpc(
                        function =
                            "get_available_vehicle_report_tokens",

                        parameters =
                            emptyMap<String, String>()
                    )
                    .decodeAs<Int>()

            Result.success(
                count
            )

        } catch (
            e: Exception
        ) {

            Result.failure(
                e
            )
        }
    }

    /*
     * =========================================================
     * RESERVE TOKEN
     * =========================================================
     *
     * Backend changes:
     *
     * available
     *     ↓
     * held
     *
     * The returned token ID is retained by the ViewModel until
     * either:
     *
     *     consumeToken()
     *
     * or
     *
     *     refundToken()
     *
     * succeeds.
     */

    suspend fun reserveToken(): Result<String> {

        return try {

            val tokenId =
                supabase
                    .postgrest
                    .rpc(
                        function =
                            "reserve_vehicle_report_token",

                        parameters =
                            emptyMap<String, String>()
                    )
                    .decodeAs<String>()

            if (
                tokenId.isBlank()
            ) {

                Result.failure(
                    IllegalStateException(
                        "The vehicle report token reservation returned an empty token ID."
                    )
                )

            } else {

                Result.success(
                    tokenId
                )
            }

        } catch (
            e: Exception
        ) {

            Result.failure(
                e
            )
        }
    }

    /*
     * =========================================================
     * CONSUME TOKEN
     * =========================================================
     *
     * PostgreSQL expects:
     *
     *     p_token_id
     *
     * The token must currently belong to the authenticated user
     * and must still have status = 'held'.
     *
     * The backend returns:
     *
     *     true
     *
     * only when the held token was actually changed to consumed.
     */

    suspend fun consumeToken(
        tokenId: String
    ): Result<Boolean> {

        return try {

            val params =
                ConsumeVehicleReportTokenParams(
                    tokenId =
                        tokenId
                )

            val consumed =
                supabase
                    .postgrest
                    .rpc(
                        function =
                            "consume_vehicle_report_token",

                        parameters =
                            params
                    )
                    .decodeAs<Boolean>()

            if (
                consumed
            ) {

                Result.success(
                    true
                )

            } else {

                Result.failure(
                    IllegalStateException(
                        "The vehicle report token could not be consumed."
                    )
                )
            }

        } catch (
            e: Exception
        ) {

            Result.failure(
                e
            )
        }
    }

    /*
     * =========================================================
     * REFUND TOKEN
     * =========================================================
     *
     * PostgreSQL expects:
     *
     *     p_token_id
     *
     * The token must currently:
     *
     *     belong to the authenticated user
     *     AND
     *     have status = 'held'
     *
     * The backend then changes:
     *
     *     held
     *       ↓
     *     available
     *
     * and clears held_at.
     */

    suspend fun refundToken(
        tokenId: String
    ): Result<Boolean> {

        return try {

            val params =
                RefundVehicleReportTokenParams(
                    tokenId =
                        tokenId
                )

            val refunded =
                supabase
                    .postgrest
                    .rpc(
                        function =
                            "refund_vehicle_report_token",

                        parameters =
                            params
                    )
                    .decodeAs<Boolean>()

            if (
                refunded
            ) {

                Result.success(
                    true
                )

            } else {

                Result.failure(
                    IllegalStateException(
                        "The vehicle report token could not be refunded."
                    )
                )
            }

        } catch (
            e: Exception
        ) {

            Result.failure(
                e
            )
        }
    }
}


/*
 * =========================================================
 * CONSUME TOKEN PARAMETERS
 * =========================================================
 */

@Serializable
private data class ConsumeVehicleReportTokenParams(

    @SerialName("p_token_id")
    val tokenId: String
)


/*
 * =========================================================
 * REFUND TOKEN PARAMETERS
 * =========================================================
 */

@Serializable
private data class RefundVehicleReportTokenParams(

    @SerialName("p_token_id")
    val tokenId: String
)