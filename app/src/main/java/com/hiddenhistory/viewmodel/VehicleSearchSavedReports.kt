package com.hiddenhistory.viewmodel

import android.util.Log
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.models.AdvertAnalysis
import com.hiddenhistory.models.HiddenHistoryReportEntity
import com.hiddenhistory.models.HiddenHistoryReportInsert
import com.hiddenhistory.models.Vehicle
import com.hiddenhistory.repository.HiddenHistoryRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject

/*
 * Temporary local repository.
 *
 * IMPORTANT:
 * This is NOT the permanent storage for Hidden History reports.
 * Permanent reports are stored in Supabase through
 * HiddenHistoryRepository.
 *
 * This can remain temporarily because other parts of the existing
 * application may still reference it.
 */
object LocalReportRepository {

    private val _savedReportJsons =
        MutableStateFlow<List<String>>(emptyList())

    val savedReportJsons: StateFlow<List<String>> =
        _savedReportJsons.asStateFlow()

    fun saveReportJson(jsonString: String) {
        val currentList = _savedReportJsons.value

        if (!currentList.contains(jsonString)) {
            _savedReportJsons.value =
                currentList + jsonString
        }
    }
}

class VehicleSearchSavedReports(
    private val hiddenHistoryRepository: HiddenHistoryRepository,
    private val jsonParser: Json
) {

    suspend fun loadSavedReports(): List<HiddenHistoryReportEntity> {
        val session =
            SupabaseManager.client
                .auth
                .currentSessionOrNull()

        val userId =
            session
                ?.user
                ?.id

        if (userId == null) {
            Log.w(
                "VehicleSearch",
                "Cannot load saved reports: user is not logged in."
            )
            throw IllegalStateException("You must be logged in to load saved reports.")
        }

        val reports =
            hiddenHistoryRepository
                .getUserReports(
                    userId
                )

        Log.d(
            "VehicleSearch",
            "Loaded ${reports.size} saved Hidden History reports."
        )

        return reports
    }

    suspend fun saveCurrentReport(
        currentRawJson: String?,
        parsedAdvert: com.hiddenhistory.engine.ParsedVehicleAdvert? = null,
        advertAnalysis: AdvertAnalysis? = null,
        officialCrossCheck: com.hiddenhistory.engine.advert.crosscheck.AdvertOfficialCrossCheckEngine.CrossCheckResult? = null,
        rawAdvertInput: String? = null
    ): List<HiddenHistoryReportEntity> {

        val jsonString =
            currentRawJson

        if (jsonString.isNullOrBlank()) {
            throw IllegalStateException(
                "No vehicle data available to save."
            )
        }

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
                "Save report failed: user is not logged in."
            )

            throw IllegalStateException(
                "You must be logged in to save a report."
            )
        }

        val vehicleJson =
            try {
                jsonParser
                    .parseToJsonElement(
                        jsonString
                    )
                    .jsonObject
            } catch (e: Throwable) {
                Log.e(
                    "VehicleSearch",
                    "Failed to convert vehicle JSON: ${e.message}",
                    e
                )

                throw IllegalStateException(
                    "The vehicle report could not be prepared for saving."
                )
            }

        val vehicle =
            runCatching {
                jsonParser
                    .decodeFromString<Vehicle>(
                        jsonString
                    )
            }.getOrNull()

        val registration =
            vehicle
                ?.registrationNumber
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: vehicle
                    ?.registration
                    ?.takeIf {
                        it.isNotBlank()
                    }

        if (registration.isNullOrBlank()) {
            throw IllegalStateException(
                "Vehicle registration could not be determined."
            )
        }

        /*
         * =========================================================
         * COMPLETE ADVERT ANALYSIS
         * =========================================================
         *
         * The AdvertAnalyzer is deliberately saved separately from
         * the main vehicle intelligence result.
         *
         * This keeps Hidden History connected to the advert-analysis
         * engine only, as required.
         */
        val advertJson =
            when {

                advertAnalysis != null ->
                    buildAdvertAnalysisJson(
                        analysis = advertAnalysis,
                        rawAdvertInput = rawAdvertInput
                    )

                parsedAdvert != null ->
                    buildAdvertJson(
                        advert = parsedAdvert,
                        rawAdvertInput = rawAdvertInput
                    )

                else ->
                    null
            }

        /*
         * =========================================================
         * ADVERT ↔ OFFICIAL CROSS-CHECK
         * =========================================================
         *
         * This is evidence produced from the advert analysis and the
         * official vehicle/MOT result. It is not the main intelligence
         * engine.
         */
        val crossCheckJson =
            officialCrossCheck?.let {
                buildJsonObject {
                    put(
                        "warnings",
                        buildJsonArray {
                            it.warnings.forEach { value ->
                                add(value)
                            }
                        }
                    )

                    put(
                        "confirmations",
                        buildJsonArray {
                            it.confirmations.forEach { value ->
                                add(value)
                            }
                        }
                    )

                    put(
                        "verificationItems",
                        buildJsonArray {
                            it.verificationItems.forEach { value ->
                                add(value)
                            }
                        }
                    )
                }
            }

        val vehicleCache =
            try {
                hiddenHistoryRepository
                    .getVehicleCache(
                        registration.uppercase()
                    )
            } catch (cacheError: Throwable) {
                Log.w(
                    "VehicleSearch",
                    "Vehicle cache lookup failed: ${cacheError.message}"
                )

                null
            }

        val report =
            HiddenHistoryReportInsert(
                userId =
                    userId,

                vehicleCacheId =
                    vehicleCache?.id,

                registration =
                    registration.uppercase(),

                reportType =
                    "FULL_VEHICLE_REPORT",

                /*
                 * Keep the COMPLETE official vehicle payload.
                 *
                 * This preserves every field returned by the official
                 * vehicle search, including fields not represented by
                 * the Kotlin Vehicle model.
                 */
                vehicleData =
                    vehicleJson,

                dvlaData =
                    null,

                dvsaData =
                    null,

                thirdPartyData =
                    null,

                /*
                 * Complete AdvertAnalyzer result.
                 */
                advertData =
                    advertJson,

                /*
                 * Advert ↔ official vehicle/MOT cross-check.
                 *
                 * This is not the removed legacy intelligence result.
                 */
                languageAnalysis =
                    crossCheckJson,

                reportSummary =
                    advertAnalysis
                        ?.overallSummary
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: parsedAdvert
                            ?.overallSummary
                            ?.takeIf {
                                it.isNotBlank()
                            },

                providerName =
                    null,

                providerReportId =
                    null
            )

        hiddenHistoryRepository
            .insertUserReport(
                report
            )

        /*
         * Keep the complete raw vehicle report in the temporary local
         * cache as well.
         */
        LocalReportRepository
            .saveReportJson(
                jsonString
            )

        val updatedReports =
            hiddenHistoryRepository
                .getUserReports(
                    userId
                )

        Log.d(
            "VehicleSearch",
            "Complete Hidden History report saved for ${registration.uppercase()}."
        )

        return updatedReports
    }

    private fun buildAdvertAnalysisJson(
        analysis: AdvertAnalysis,
        rawAdvertInput: String?
    ): JsonObject {

        val analysisJson =
            runCatching {
                jsonParser
                    .encodeToString(
                        analysis
                    )
            }.getOrElse { error ->
                Log.e(
                    "VehicleSearch",
                    "Failed to serialize Pro AdvertAnalysis: ${error.message}",
                    error
                )

                throw IllegalStateException(
                    "The Pro advert analysis could not be prepared for saving."
                )
            }

        val analysisObject =
            runCatching {
                jsonParser
                    .parseToJsonElement(
                        analysisJson
                    )
                    .jsonObject
            }.getOrElse { error ->
                Log.e(
                    "VehicleSearch",
                    "Failed to parse serialized Pro AdvertAnalysis: ${error.message}",
                    error
                )

                throw IllegalStateException(
                    "The Pro advert analysis could not be prepared for saving."
                )
            }

        return buildJsonObject {

            analysisObject.forEach { (key, value) ->
                put(
                    key,
                    value
                )
            }

            put(
                "analysisType",
                "PRO_ADVERT_ANALYSIS"
            )

            rawAdvertInput
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    put(
                        "rawAdvertInput",
                        it
                    )
                }

            put(
                "savedAt",
                java.time.OffsetDateTime
                    .now(
                        java.time.ZoneOffset.UTC
                    )
                    .toString()
            )
        }
    }

    private fun buildAdvertJson(
        advert: com.hiddenhistory.engine.ParsedVehicleAdvert,
        rawAdvertInput: String?
    ): kotlinx.serialization.json.JsonObject {

        return buildJsonObject {

            put(
                "rawText",
                advert.rawText
            )

            put(
                "rawAdvertInput",
                rawAdvertInput ?: advert.rawText
            )

            put(
                "normalizedTokens",
                buildJsonArray {
                    advert.normalizedTokens.forEach {
                        add(it)
                    }
                }
            )

            put(
                "detectedDialect",
                advert.detectedDialect
            )

            advert.make?.let {
                put("make", it)
            }

            advert.model?.let {
                put("model", it)
            }

            advert.year?.let {
                put("year", it)
            }

            advert.mileage?.let {
                put("mileage", it)
            }

            advert.price?.let {
                put("price", it)
            }

            advert.transmission?.let {
                put("transmission", it)
            }

            advert.engineSize?.let {
                put("engineSize", it)
            }

            advert.fuelType?.let {
                put("fuelType", it)
            }

            put(
                "conditionScore",
                advert.conditionScore
            )

            put(
                "professionalSummary",
                advert.professionalSummary
            )

            put(
                "keyInsights",
                buildJsonArray {
                    advert.keyInsights.forEach {
                        add(it)
                    }
                }
            )

            put(
                "riskFlags",
                buildJsonArray {
                    advert.riskFlags.forEach {
                        add(it)
                    }
                }
            )

            put(
                "rawExtractedAttributes",
                buildJsonObject {
                    advert.rawExtractedAttributes.forEach { (key, value) ->
                        put(key, value)
                    }
                }
            )

            put(
                "claimsMadeBySeller",
                buildJsonArray {
                    advert.claimsMadeBySeller.forEach {
                        add(it)
                    }
                }
            )

            put(
                "notableWording",
                buildJsonArray {
                    advert.notableWording.forEach {
                        add(it)
                    }
                }
            )

            put(
                "missingInformation",
                buildJsonArray {
                    advert.missingInformation.forEach {
                        add(it)
                    }
                }
            )

            put(
                "inconsistencies",
                buildJsonArray {
                    advert.inconsistencies.forEach {
                        add(it)
                    }
                }
            )

            put(
                "thingsWorthVerifying",
                buildJsonArray {
                    advert.thingsWorthVerifying.forEach {
                        add(it)
                    }
                }
            )

            put(
                "questionsTheBuyerShouldAsk",
                buildJsonArray {
                    advert.questionsTheBuyerShouldAsk.forEach {
                        add(it)
                    }
                }
            )

            put(
                "overallSummary",
                advert.overallSummary
            )

            /*
             * Server created_at remains the authoritative saved
             * report timestamp. This value is additionally kept inside
             * the saved advert snapshot so the report itself carries
             * the save event timestamp.
             */
            put(
                "savedAt",
                java.time.OffsetDateTime
                    .now(
                        java.time.ZoneOffset.UTC
                    )
                    .toString()
            )
        }
    }

    suspend fun deleteSavedReport(reportId: String): List<HiddenHistoryReportEntity> {
        hiddenHistoryRepository
            .deleteUserReport(
                reportId
            )

        val session =
            SupabaseManager.client
                .auth
                .currentSessionOrNull()

        val userId =
            session
                ?.user
                ?.id

        return if (userId != null) {
            hiddenHistoryRepository.getUserReports(userId)
        } else {
            emptyList()
        }
    }
}
