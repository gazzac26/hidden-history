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

        val currentList =
            _savedReportJsons.value

        if (
            !currentList.contains(
                jsonString
            )
        ) {

            _savedReportJsons.value =
                currentList + jsonString
        }
    }
}


class VehicleSearchSavedReports(
    private val hiddenHistoryRepository: HiddenHistoryRepository,
    private val jsonParser: Json
) {

    suspend fun loadSavedReports():
        List<HiddenHistoryReportEntity> {

        val session =
            SupabaseManager.client
                .auth
                .currentSessionOrNull()

        val userId =
            session
                ?.user
                ?.id

        if (
            userId == null
        ) {

            Log.w(
                "VehicleSearch",
                "Cannot load saved reports: user is not logged in."
            )

            throw IllegalStateException(
                "You must be logged in to load saved reports."
            )
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

    /*
     * =========================================================
     * SAVE CURRENT REPORT
     * =========================================================
     *
     * Supports BOTH:
     *
     * 1. Full vehicle reports
     *
     *    official vehicle JSON
     *    + optional advert analysis
     *
     * 2. Advert-only Pro reports
     *
     *    no official vehicle JSON
     *    + AdvertAnalysis and/or parsed advert
     *
     * This is intentionally additive.
     *
     * Existing full vehicle report behaviour is preserved.
     */

    suspend fun saveCurrentReport(
        currentRawJson: String?,
        parsedAdvert: com.hiddenhistory.engine.ParsedVehicleAdvert? = null,
        advertAnalysis: AdvertAnalysis? = null,
        officialCrossCheck: com.hiddenhistory.engine.advert.crosscheck.AdvertOfficialCrossCheckEngine.CrossCheckResult? = null,
        rawAdvertInput: String? = null
    ): List<HiddenHistoryReportEntity> {

        /*
         * =========================================================
         * DETERMINE REPORT TYPE
         * =========================================================
         */

        val hasVehicleJson =
            !currentRawJson.isNullOrBlank()

        val hasAdvertAnalysis =
            advertAnalysis != null

        val hasParsedAdvert =
            parsedAdvert != null

        val hasAdvertData =
            hasAdvertAnalysis ||
                hasParsedAdvert

        /*
         * There must be at least one actual report source.
         */

        if (
            !hasVehicleJson &&
            !hasAdvertData
        ) {

            throw IllegalStateException(
                "No report data available to save."
            )
        }

        /*
         * =========================================================
         * AUTHENTICATION
         * =========================================================
         */

        val session =
            SupabaseManager.client
                .auth
                .currentSessionOrNull()

        val userId =
            session
                ?.user
                ?.id

        if (
            userId == null
        ) {

            Log.e(
                "VehicleSearch",
                "Save report failed: user is not logged in."
            )

            throw IllegalStateException(
                "You must be logged in to save a report."
            )
        }

        /*
         * =========================================================
         * VEHICLE JSON
         * =========================================================
         *
         * Full vehicle searches retain the complete official JSON.
         *
         * Advert-only searches do not have vehicle JSON, so an empty
         * JSON object is stored rather than inventing vehicle data.
         *
         * This preserves the JSON column's object structure while
         * making it possible to distinguish an advert-only report
         * through reportType.
         */

        val vehicleJson =
            if (
                hasVehicleJson
            ) {

                try {

                    jsonParser
                        .parseToJsonElement(
                            currentRawJson!!
                        )
                        .jsonObject

                } catch (
                    e: Throwable
                ) {

                    Log.e(
                        "VehicleSearch",
                        "Failed to convert vehicle JSON: ${e.message}",
                        e
                    )

                    throw IllegalStateException(
                        "The vehicle report could not be prepared for saving."
                    )
                }

            } else {

                buildJsonObject {}
            }

        /*
         * =========================================================
         * VEHICLE
         * =========================================================
         */

        val vehicle =
            if (
                hasVehicleJson
            ) {

                runCatching {

                    jsonParser
                        .decodeFromString<Vehicle>(
                            currentRawJson!!
                        )

                }.getOrNull()

            } else {

                null
            }

        /*
         * =========================================================
         * REGISTRATION
         * =========================================================
         *
         * Full vehicle reports continue to use the official vehicle
         * registration.
         *
         * Advert-only reports deliberately do NOT invent a vehicle
         * registration.
         *
         * Because the existing HiddenHistoryReportInsert model expects
         * a registration value, the advert-only report receives the
         * explicit marker "ADVERT_ONLY".
         *
         * This makes the saved record unambiguous and avoids falsely
         * associating an advert with a registration that was never
         * supplied or officially verified.
         */

        val officialRegistration =
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

        val registration =
            officialRegistration
                ?.uppercase()
                ?: if (
                    hasAdvertData
                ) {

                    "ADVERT_ONLY"

                } else {

                    null
                }

        /*
         * =========================================================
         * REGISTRATION VALIDATION
         * =========================================================
         *
         * Only full vehicle reports require a genuine vehicle
         * registration.
         *
         * Advert-only reports are explicitly allowed to continue.
         */

        if (
            hasVehicleJson &&
            registration.isNullOrBlank()
        ) {

            throw IllegalStateException(
                "Vehicle registration could not be determined."
            )
        }

        /*
         * =========================================================
         * COMPLETE ADVERT ANALYSIS
         * =========================================================
         */

        val advertJson =
            when {

                advertAnalysis != null ->

                    buildAdvertAnalysisJson(
                        analysis =
                            advertAnalysis,

                        rawAdvertInput =
                            rawAdvertInput
                    )

                parsedAdvert != null ->

                    buildAdvertJson(
                        advert =
                            parsedAdvert,

                        rawAdvertInput =
                            rawAdvertInput
                    )

                else ->

                    null
            }

        /*
         * =========================================================
         * ADVERT ↔ OFFICIAL CROSS-CHECK
         * =========================================================
         *
         * This is only present when an official vehicle result
         * actually exists.
         *
         * Advert-only reports correctly have no cross-check.
         */

        val crossCheckJson =
            officialCrossCheck?.let {

                buildJsonObject {

                    put(
                        "warnings",
                        buildJsonArray {

                            it.warnings.forEach { value ->

                                add(
                                    value
                                )
                            }
                        }
                    )

                    put(
                        "confirmations",
                        buildJsonArray {

                            it.confirmations.forEach { value ->

                                add(
                                    value
                                )
                            }
                        }
                    )

                    put(
                        "verificationItems",
                        buildJsonArray {

                            it.verificationItems.forEach { value ->

                                add(
                                    value
                                )
                            }
                        }
                    )
                }
            }

        /*
         * =========================================================
         * VEHICLE CACHE
         * =========================================================
         *
         * Advert-only reports have no official registration, so they
         * deliberately skip the vehicle cache lookup.
         */

        val vehicleCache =
            if (
                !registration.isNullOrBlank() &&
                registration != "ADVERT_ONLY"
            ) {

                try {

                    hiddenHistoryRepository
                        .getVehicleCache(
                            registration
                        )

                } catch (
                    cacheError: Throwable
                ) {

                    Log.w(
                        "VehicleSearch",
                        "Vehicle cache lookup failed: ${cacheError.message}"
                    )

                    null
                }

            } else {

                null
            }

        /*
         * =========================================================
         * REPORT TYPE
         * =========================================================
         */

        val reportType =
            if (
                hasVehicleJson
            ) {

                "FULL_VEHICLE_REPORT"

            } else {

                "PRO_ADVERT_REPORT"
            }

        /*
         * =========================================================
         * REPORT INSERT
         * =========================================================
         */

        val report =
            HiddenHistoryReportInsert(

                userId =
                    userId,

                vehicleCacheId =
                    vehicleCache?.id,

                registration =
                    registration
                        ?: "ADVERT_ONLY",

                reportType =
                    reportType,

                /*
                 * Full official vehicle payload when available.
                 *
                 * Advert-only reports receive {} because there is no
                 * official vehicle payload to store.
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

        /*
         * =========================================================
         * INSERT INTO SUPABASE
         * =========================================================
         */

        hiddenHistoryRepository
            .insertUserReport(
                report
            )

        /*
         * =========================================================
         * LOCAL RAW VEHICLE CACHE
         * =========================================================
         *
         * Only cache raw official vehicle JSON when it actually
         * exists.
         *
         * An advert-only report must not put "{}" into the local
         * vehicle-report cache and pretend that it is a vehicle
         * report.
         */

        if (
            hasVehicleJson
        ) {

            LocalReportRepository
                .saveReportJson(
                    currentRawJson!!
                )
        }

        /*
         * =========================================================
         * RETURN UPDATED REPORT LIST
         * =========================================================
         */

        val updatedReports =
            hiddenHistoryRepository
                .getUserReports(
                    userId
                )

        Log.d(
            "VehicleSearch",
            "Complete Hidden History report saved. type=$reportType registration=${
                registration ?: "ADVERT_ONLY"
            }"
        )

        return updatedReports
    }

    /*
     * =========================================================
     * BUILD PRO ADVERT ANALYSIS JSON
     * =========================================================
     */

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
                ?.takeIf {
                    it.isNotBlank()
                }
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

    /*
     * =========================================================
     * BUILD DETERMINISTIC ADVERT JSON
     * =========================================================
     */

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
                rawAdvertInput
                    ?: advert.rawText
            )

            put(
                "normalizedTokens",
                buildJsonArray {

                    advert.normalizedTokens.forEach {

                        add(
                            it
                        )
                    }
                }
            )

            put(
                "detectedDialect",
                advert.detectedDialect
            )

            advert.make?.let {

                put(
                    "make",
                    it
                )
            }

            advert.model?.let {

                put(
                    "model",
                    it
                )
            }

            advert.year?.let {

                put(
                    "year",
                    it
                )
            }

            advert.mileage?.let {

                put(
                    "mileage",
                    it
                )
            }

            advert.price?.let {

                put(
                    "price",
                    it
                )
            }

            advert.transmission?.let {

                put(
                    "transmission",
                    it
                )
            }

            advert.engineSize?.let {

                put(
                    "engineSize",
                    it
                )
            }

            advert.fuelType?.let {

                put(
                    "fuelType",
                    it
                )
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

                        add(
                            it
                        )
                    }
                }
            )

            put(
                "riskFlags",
                buildJsonArray {

                    advert.riskFlags.forEach {

                        add(
                            it
                        )
                    }
                }
            )

            put(
                "rawExtractedAttributes",
                buildJsonObject {

                    advert.rawExtractedAttributes
                        .forEach { (key, value) ->

                            put(
                                key,
                                value
                            )
                        }
                }
            )

            put(
                "claimsMadeBySeller",
                buildJsonArray {

                    advert.claimsMadeBySeller.forEach {

                        add(
                            it
                        )
                    }
                }
            )

            put(
                "notableWording",
                buildJsonArray {

                    advert.notableWording.forEach {

                        add(
                            it
                        )
                    }
                }
            )

            put(
                "missingInformation",
                buildJsonArray {

                    advert.missingInformation.forEach {

                        add(
                            it
                        )
                    }
                }
            )

            put(
                "inconsistencies",
                buildJsonArray {

                    advert.inconsistencies.forEach {

                        add(
                            it
                        )
                    }
                }
            )

            put(
                "thingsWorthVerifying",
                buildJsonArray {

                    advert.thingsWorthVerifying.forEach {

                        add(
                            it
                        )
                    }
                }
            )

            put(
                "questionsTheBuyerShouldAsk",
                buildJsonArray {

                    advert.questionsTheBuyerShouldAsk.forEach {

                        add(
                            it
                        )
                    }
                }
            )

            put(
                "overallSummary",
                advert.overallSummary
            )

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

    /*
     * =========================================================
     * DELETE SAVED REPORT
     * =========================================================
     */

    suspend fun deleteSavedReport(
        reportId: String
    ): List<HiddenHistoryReportEntity> {

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

        return if (
            userId != null
        ) {

            hiddenHistoryRepository
                .getUserReports(
                    userId
                )

        } else {

            emptyList()
        }
    }
}