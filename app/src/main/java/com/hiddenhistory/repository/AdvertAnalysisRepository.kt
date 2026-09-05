package com.hiddenhistory.repository

import android.util.Log
import com.hiddenhistory.engine.ParsedVehicleAdvert
import com.hiddenhistory.models.AdvertAnalysis
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// ============================================================================
// HIDDEN HISTORY
// PRO ADVERT ANALYSIS REQUEST
// ============================================================================
//
// Pipeline:
//
// AdvertParserEngine
//        |
//        v
// ParsedVehicleAdvert
//        |
//        v
// Structured deterministic advert evidence
//        |
//        +-----------------------------+
//        |                             |
//        v                             v
// Official vehicle JSON          analyse-advert
//                                      |
//                                      v
//                                    Gemini
//
// IMPORTANT:
//
// AdvertParserEngine is NOT changed.
//
// Its existing output is adapted into structured JSON and supplied to the
// Pro/Gemini pipeline as evidence.
//
// IMPORTANT SOURCE SEPARATION:
//
// The advert itself, seller claims and official vehicle evidence are separate
// evidence sources.
//
// This repository must NOT blend official MOT/DVLA conclusions into the
// advert-summary evidence.
//
// Gemini remains the reasoning layer.
// ============================================================================

@Serializable
data class AdvertRequestPayload(

    val advert: String,

    val registration: String? = null,

    val officialVehicleData: JsonElement? = null,

    val deterministicAdvertAnalysis: JsonElement? = null
)

class AdvertAnalysisRepository(
    private val supabaseClient: SupabaseClient
) {

    // ========================================================================
    // JSON
    // ========================================================================

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    // ========================================================================
    // ANALYSE ADVERT
    // ========================================================================

    suspend fun analyzeAdvert(
        advertText: String,
        registration: String? = null,
        officialVehicleData: String? = null,
        deterministicAdvertAnalysis: ParsedVehicleAdvert? = null
    ): AdvertAnalysis {

        return try {

            /*
             * ------------------------------------------------------------
             * DETERMINISTIC ENGINE OUTPUT
             * ------------------------------------------------------------
             *
             * The deterministic engine has already run upstream.
             *
             * We do NOT run it again here.
             *
             * We only expose its existing structured result to the
             * Pro/Gemini pipeline.
             */

            val deterministicAnalysisJson:
                JsonElement? =
                deterministicAdvertAnalysis
                    ?.let { advert ->

                        buildDeterministicAnalysisJson(
                            advert
                        )
                    }

            /*
             * ------------------------------------------------------------
             * OFFICIAL VEHICLE JSON
             * ------------------------------------------------------------
             *
             * Official vehicle data is supplied separately from advert
             * evidence.
             *
             * It must remain a separate evidence source.
             *
             * Do not merge official MOT/DVLA statements into the advert
             * summary before sending the request.
             */

            val officialVehicleJson:
                JsonElement? =
                officialVehicleData
                    ?.let { raw ->

                        parseJsonElementOrNull(
                            raw
                        )
                    }

            /*
             * ------------------------------------------------------------
             * REQUEST
             * ------------------------------------------------------------
             */

            val requestPayload =
                AdvertRequestPayload(

                    advert =
                        advertText,

                    registration =
                        registration,

                    officialVehicleData =
                        officialVehicleJson,

                    deterministicAdvertAnalysis =
                        deterministicAnalysisJson
                )

            Log.d(
                "AdvertAnalysisRepo",
                "Sending Pro advert analysis request."
            )

            Log.d(
                "AdvertAnalysisRepo",
                "Official vehicle evidence supplied: ${
                    officialVehicleJson != null
                }"
            )

            Log.d(
                "AdvertAnalysisRepo",
                "Deterministic advert evidence supplied: ${
                    deterministicAnalysisJson != null
                }"
            )

            /*
             * ------------------------------------------------------------
             * EDGE FUNCTION
             * ------------------------------------------------------------
             */

            val response =
                supabaseClient.functions.invoke(

                    function =
                        "analyse-advert",

                    body =
                        requestPayload
                )

            /*
             * ------------------------------------------------------------
             * RESPONSE
             * ------------------------------------------------------------
             */

            val responseString =
                response.bodyAsText()

            if (
                responseString.isBlank()
            ) {

                throw IllegalStateException(
                    "analyse-advert returned an empty response."
                )
            }

            Log.d(
                "AdvertAnalysisRepo",
                "Pro advert analysis response received."
            )

            /*
             * ------------------------------------------------------------
             * GEMINI RESPONSE → EXISTING MODEL
             * ------------------------------------------------------------
             */

            json.decodeFromString<AdvertAnalysis>(
                responseString
            )

        } catch (
            e: CancellationException
        ) {

            /*
             * ------------------------------------------------------------
             * COROUTINE CANCELLATION
             * ------------------------------------------------------------
             *
             * Cancellation is not an application error.
             *
             * Do not convert it into a user-facing failure.
             */

            throw e

        } catch (
            e: Throwable
        ) {

            /*
             * ------------------------------------------------------------
             * SAFE ERROR BOUNDARY
             * ------------------------------------------------------------
             *
             * IMPORTANT:
             *
             * The original exception may contain:
             *
             * - Supabase URLs
             * - Edge Function URLs
             * - Ktor implementation details
             * - HTTP information
             * - Network details
             * - Stack-trace information
             *
             * None of that must reach the UI.
             *
             * The complete technical exception is retained in Logcat for
             * development/debugging only.
             */

            Log.e(
                "AdvertAnalysisRepo",
                "Failed to analyze advert.",
                e
            )

            /*
             * ------------------------------------------------------------
             * USER-FACING ERROR
             * ------------------------------------------------------------
             *
             * Deliberately generic.
             *
             * NEVER throw the original exception here.
             */

            throw IllegalStateException(
                "We couldn't complete the Pro analysis. Please try again."
            )
        }
    }

    // ========================================================================
    // PARSE JSON
    // ========================================================================

    private fun parseJsonElementOrNull(
        raw: String
    ): JsonElement? {

        val trimmed =
            raw.trim()

        if (
            trimmed.isEmpty()
        ) {
            return null
        }

        return try {

            json.parseToJsonElement(
                trimmed
            )

        } catch (
            e: Throwable
        ) {

            Log.w(
                "AdvertAnalysisRepo",
                "Official vehicle data was not valid JSON; omitting it from Pro analysis.",
                e
            )

            null
        }
    }

    // ========================================================================
    // DETERMINISTIC ANALYSIS → JSON
    // ========================================================================
    //
    // PURE ADAPTER.
    //
    // This does NOT alter AdvertParserEngine.
    //
    // It exposes the existing deterministic result as structured evidence.
    //
    // SOURCE-SEPARATION RULE:
    //
    // professionalSummary and overallSummary are intentionally NOT used as
    // authoritative advert evidence here because those fields may already
    // contain generated/blended language.
    //
    // The original advert text, seller claims, extracted attributes,
    // wording, risks, inconsistencies, missing information and verification
    // items remain available as structured evidence.
    // ========================================================================

    private fun buildDeterministicAnalysisJson(
        advert: ParsedVehicleAdvert
    ): JsonObject =
        buildJsonObject {

            // ----------------------------------------------------------------
            // ORIGINAL ADVERT
            // ----------------------------------------------------------------

            put(
                "rawText",
                JsonPrimitive(
                    advert.rawText
                )
            )

            // ----------------------------------------------------------------
            // BASIC EXTRACTION
            // ----------------------------------------------------------------

            put(
                "detectedDialect",
                JsonPrimitive(
                    advert.detectedDialect
                )
            )

            put(
                "make",
                JsonPrimitive(
                    advert.make ?: ""
                )
            )

            put(
                "model",
                JsonPrimitive(
                    advert.model ?: ""
                )
            )

            advert.year?.let {

                put(
                    "year",
                    JsonPrimitive(it)
                )
            }

            put(
                "mileage",
                JsonPrimitive(
                    advert.mileage ?: ""
                )
            )

            put(
                "price",
                JsonPrimitive(
                    advert.price ?: ""
                )
            )

            put(
                "transmission",
                JsonPrimitive(
                    advert.transmission ?: ""
                )
            )

            put(
                "engineSize",
                JsonPrimitive(
                    advert.engineSize ?: ""
                )
            )

            put(
                "fuelType",
                JsonPrimitive(
                    advert.fuelType ?: ""
                )
            )

            // ----------------------------------------------------------------
            // DETERMINISTIC CONDITION / INFORMATION INDEX
            // ----------------------------------------------------------------
            //
            // This value belongs to the advert-analysis layer.
            //
            // It is NOT a mechanical diagnosis and must not be described
            // as evidence of the vehicle's physical condition.
            // ----------------------------------------------------------------

            put(
                "conditionScore",
                JsonPrimitive(
                    advert.conditionScore
                )
            )

            // ----------------------------------------------------------------
            // NORMALISED TOKENS
            // ----------------------------------------------------------------

            put(
                "normalizedTokens",

                buildJsonArray {

                    advert.normalizedTokens.forEach { token ->

                        add(
                            JsonPrimitive(token)
                        )
                    }
                }
            )

            // ----------------------------------------------------------------
            // KEY INSIGHTS
            // ----------------------------------------------------------------

            put(
                "keyInsights",

                buildJsonArray {

                    advert.keyInsights.forEach { insight ->

                        add(
                            JsonPrimitive(insight)
                        )
                    }
                }
            )

            // ----------------------------------------------------------------
            // RISK FLAGS
            // ----------------------------------------------------------------

            put(
                "riskFlags",

                buildJsonArray {

                    advert.riskFlags.forEach { flag ->

                        add(
                            JsonPrimitive(flag)
                        )
                    }
                }
            )

            // ----------------------------------------------------------------
            // SELLER CLAIMS
            // ----------------------------------------------------------------
            //
            // These are claims extracted from the advert.
            //
            // They are NOT treated as verified facts.
            // ----------------------------------------------------------------

            put(
                "claimsMadeBySeller",

                buildJsonArray {

                    advert.claimsMadeBySeller.forEach { claim ->

                        add(
                            JsonPrimitive(claim)
                        )
                    }
                }
            )

            // ----------------------------------------------------------------
            // NOTABLE WORDING
            // ----------------------------------------------------------------

            put(
                "notableWording",

                buildJsonArray {

                    advert.notableWording.forEach { wording ->

                        add(
                            JsonPrimitive(wording)
                        )
                    }
                }
            )

            // ----------------------------------------------------------------
            // MISSING INFORMATION
            // ----------------------------------------------------------------

            put(
                "missingInformation",

                buildJsonArray {

                    advert.missingInformation.forEach { item ->

                        add(
                            JsonPrimitive(item)
                        )
                    }
                }
            )

            // ----------------------------------------------------------------
            // INCONSISTENCIES
            // ----------------------------------------------------------------

            put(
                "inconsistencies",

                buildJsonArray {

                    advert.inconsistencies.forEach { item ->

                        add(
                            JsonPrimitive(item)
                        )
                    }
                }
            )

            // ----------------------------------------------------------------
            // VERIFICATION ITEMS
            // ----------------------------------------------------------------

            put(
                "thingsWorthVerifying",

                buildJsonArray {

                    advert.thingsWorthVerifying.forEach { item ->

                        add(
                            JsonPrimitive(item)
                        )
                    }
                }
            )

            // ----------------------------------------------------------------
            // BUYER QUESTIONS
            // ----------------------------------------------------------------

            put(
                "questionsTheBuyerShouldAsk",

                buildJsonArray {

                    advert.questionsTheBuyerShouldAsk.forEach { question ->

                        add(
                            JsonPrimitive(question)
                        )
                    }
                }
            )

            // ----------------------------------------------------------------
            // RAW EXTRACTED ATTRIBUTES
            // ----------------------------------------------------------------

            put(
                "rawExtractedAttributes",

                buildJsonObject {

                    advert.rawExtractedAttributes.forEach {
                            (key, value) ->

                        put(
                            key,
                            JsonPrimitive(value)
                        )
                    }
                }
            )
        }
}