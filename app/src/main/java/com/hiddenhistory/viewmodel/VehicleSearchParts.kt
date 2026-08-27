package com.hiddenhistory.viewmodel

import android.util.Log
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.models.MotTest
import com.hiddenhistory.models.Vehicle
import io.github.jan.supabase.auth.auth
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class VehicleSearchParts(
    private val client: OkHttpClient
) {

    /*
     * ============================================================
     * EBAY SEARCH USING MOT DATA
     * ============================================================
     */

    suspend fun executeEbaySearch(
        defectText: String,
        motTest: MotTest
    ): String {

        return executeEbaySearch(
            defectText = defectText,
            vehicle = null,
            motTest = motTest
        )
    }

    /*
     * ============================================================
     * EBAY SEARCH USING OFFICIAL VEHICLE + MOT DATA
     * ============================================================
     *
     * The Vehicle returned by the official vehicle search is the
     * preferred source of vehicle identity.
     *
     * MOT data is used as a fallback for values which may not exist
     * on the Vehicle object.
     *
     * IMPORTANT:
     *
     * Registration is NOT placed into the eBay search query.
     *
     * The raw MOT defect text is NOT placed into the eBay query.
     *
     * The defect is translated into:
     *
     *      actual part
     *      position
     *
     * The eBay query therefore describes the actual vehicle and
     * actual replacement part required.
     */

    suspend fun executeEbaySearch(
        defectText: String,
        vehicle: Vehicle?,
        motTest: MotTest
    ): String {

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
                "VehicleSearchParts",
                "eBay search failed: User is not logged in."
            )

            throw IllegalStateException(
                "You must be logged in to search parts."
            )
        }

        /*
         * ========================================================
         * TRANSLATE MOT DEFECT INTO ACTUAL PART
         * ========================================================
         */

        val partSearch =
            buildPartSearch(
                defectText
            )

        if (
            partSearch.part.isBlank()
        ) {

            throw IllegalArgumentException(
                "Could not identify the vehicle part from the MOT defect."
            )
        }

        /*
         * ========================================================
         * OFFICIAL VEHICLE DATA
         * ========================================================
         *
         * Vehicle is preferred.
         *
         * MOT is fallback only.
         */

        val registration =
            firstNonBlank(
                vehicle?.registrationNumber,
                vehicle?.registration,
                motTest.vehicleRegistration,
                motTest.registrationAtTimeOfTest
            )

        val make =
            firstNonBlank(
                vehicle?.make,
                motTest.vehicleMake
            )

        val model =
            firstNonBlank(
                vehicle?.model,
                motTest.vehicleModel
            )

        val year =
            vehicle?.year
                ?.takeIf {
                    it > 0
                }
                ?: motTest.vehicleYear
                    ?.takeIf {
                        it > 0
                    }

        val engineCapacity =
            vehicle?.engineCapacity
                ?.takeIf {
                    it > 0
                }
                ?: motTest.vehicleEngineCapacity
                    ?.takeIf {
                        it > 0
                    }

        val fuelType =
            firstNonBlank(
                vehicle?.fuelType,
                motTest.vehicleFuelType
            )

        val vin =
            firstNonBlank(
                vehicle?.vin,
                motTest.vehicleVin
            )

        /*
         * ========================================================
         * EBAY QUERY
         * ========================================================
         *
         * DO NOT add:
         *
         *      registration
         *      VIN
         *      raw MOT defect
         *
         * Add only information useful for identifying the actual
         * replacement part.
         */

        val queryTerms =
            mutableListOf<String>()

        if (
            make.isNotBlank()
        ) {

            queryTerms.add(
                make
            )
        }

        if (
            model.isNotBlank()
        ) {

            queryTerms.add(
                model
            )
        }

        if (
            year != null
        ) {

            queryTerms.add(
                year.toString()
            )
        }

        if (
            engineCapacity != null
        ) {

            queryTerms.add(
                "${engineCapacity}cc"
            )
        }

        if (
            fuelType.isNotBlank()
        ) {

            queryTerms.add(
                fuelType
            )
        }

        if (
            partSearch.position.isNotBlank()
        ) {

            queryTerms.add(
                partSearch.position
            )
        }

        queryTerms.add(
            partSearch.part
        )

        val fullSearchQuery =
            queryTerms
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .distinct()
                .joinToString(
                    " "
                )

        if (
            fullSearchQuery.isBlank()
        ) {

            throw IllegalArgumentException(
                "Could not build a valid eBay search query."
            )
        }

        /*
         * ========================================================
         * EDGE FUNCTION PAYLOAD
         * ========================================================
         *
         * The structured official vehicle information is still
         * supplied to the Edge Function.
         *
         * Registration and VIN are therefore available to the
         * backend as vehicle identity metadata without contaminating
         * the actual eBay keyword query.
         */

        val payload =
            JSONObject().apply {

                put(
                    "query",
                    fullSearchQuery
                )

                put(
                    "registration",
                    registration
                )

                put(
                    "make",
                    make
                )

                put(
                    "model",
                    model
                )

                put(
                    "year",
                    year ?: 0
                )

                put(
                    "engineSize",
                    engineCapacity
                        ?.toString()
                        ?: ""
                )

                put(
                    "engineCapacity",
                    engineCapacity ?: 0
                )

                put(
                    "fuelType",
                    fuelType
                )

                put(
                    "vin",
                    vin
                )

                put(
                    "part",
                    partSearch.part
                )

                put(
                    "position",
                    partSearch.position
                )

                put(
                    "userId",
                    userId
                )
            }
                .toString()

        /*
         * ========================================================
         * DEBUG
         * ========================================================
         */

        Log.d(
            "VehicleSearchParts",
            "----------------------------------------"
        )

        Log.d(
            "VehicleSearchParts",
            "MOT defect: $defectText"
        )

        Log.d(
            "VehicleSearchParts",
            "Official vehicle available: ${vehicle != null}"
        )

        Log.d(
            "VehicleSearchParts",
            "Registration metadata: $registration"
        )

        Log.d(
            "VehicleSearchParts",
            "Make: $make"
        )

        Log.d(
            "VehicleSearchParts",
            "Model: $model"
        )

        Log.d(
            "VehicleSearchParts",
            "Year: $year"
        )

        Log.d(
            "VehicleSearchParts",
            "Engine capacity: $engineCapacity"
        )

        Log.d(
            "VehicleSearchParts",
            "Fuel type: $fuelType"
        )

        Log.d(
            "VehicleSearchParts",
            "VIN metadata: $vin"
        )

        Log.d(
            "VehicleSearchParts",
            "Translated part: ${partSearch.part}"
        )

        Log.d(
            "VehicleSearchParts",
            "Position: ${partSearch.position}"
        )

        Log.d(
            "VehicleSearchParts",
            "FINAL EBAY QUERY: $fullSearchQuery"
        )

        Log.d(
            "VehicleSearchParts",
            "eBay payload: $payload"
        )

        Log.d(
            "VehicleSearchParts",
            "----------------------------------------"
        )

        /*
         * ========================================================
         * CALL SUPABASE EDGE FUNCTION
         * ========================================================
         */

        val accessToken =
            session.accessToken

        val body =
            payload.toRequestBody(
                "application/json".toMediaType()
            )

        val request =
            Request.Builder()
                .url(
                    "https://svnsylgpfqzbpevhjdfe.supabase.co/functions/v1/ebay-search"
                )
                .addHeader(
                    "Authorization",
                    "Bearer ${accessToken ?: ""}"
                )
                .post(
                    body
                )
                .build()

        client
            .newCall(request)
            .execute()
            .use { response ->

                val responseData =
                    response.body
                        ?.string()
                        ?.trim()
                        .orEmpty()

                if (
                    !response.isSuccessful
                ) {

                    throw IOException(
                        "Server returned code ${response.code}: $responseData"
                    )
                }

                return responseData
                    .takeIf {
                        it.startsWith(
                            "http://"
                        ) ||
                            it.startsWith(
                                "https://"
                            )
                    }
                    ?: buildEbaySearchUrl(
                        vehicle = vehicle,
                        motTest = motTest,
                        defectText = defectText
                    )
            }
    }

    /*
     * ============================================================
     * DIRECT EBAY URL
     * ============================================================
     */

    fun buildEbaySearchUrl(
        motTest: MotTest,
        defectText: String
    ): String {

        return buildEbaySearchUrl(
            vehicle = null,
            motTest = motTest,
            defectText = defectText
        )
    }

    fun buildEbaySearchUrl(
        vehicle: Vehicle?,
        motTest: MotTest,
        defectText: String
    ): String {

        val partSearch =
            buildPartSearch(
                defectText
            )

        val make =
            firstNonBlank(
                vehicle?.make,
                motTest.vehicleMake
            )

        val model =
            firstNonBlank(
                vehicle?.model,
                motTest.vehicleModel
            )

        val year =
            vehicle?.year
                ?.takeIf {
                    it > 0
                }
                ?: motTest.vehicleYear
                    ?.takeIf {
                        it > 0
                    }

        val engineCapacity =
            vehicle?.engineCapacity
                ?.takeIf {
                    it > 0
                }
                ?: motTest.vehicleEngineCapacity
                    ?.takeIf {
                        it > 0
                    }

        val fuelType =
            firstNonBlank(
                vehicle?.fuelType,
                motTest.vehicleFuelType
            )

        /*
         * Registration deliberately NOT included.
         */

        val queryTerms =
            mutableListOf<String>()

        if (
            make.isNotBlank()
        ) {

            queryTerms.add(
                make
            )
        }

        if (
            model.isNotBlank()
        ) {

            queryTerms.add(
                model
            )
        }

        if (
            year != null
        ) {

            queryTerms.add(
                year.toString()
            )
        }

        if (
            engineCapacity != null
        ) {

            queryTerms.add(
                "${engineCapacity}cc"
            )
        }

        if (
            fuelType.isNotBlank()
        ) {

            queryTerms.add(
                fuelType
            )
        }

        if (
            partSearch.position.isNotBlank()
        ) {

            queryTerms.add(
                partSearch.position
            )
        }

        queryTerms.add(
            partSearch.part
        )

        val query =
            queryTerms
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .distinct()
                .joinToString(
                    " "
                )

        return "https://www.ebay.co.uk/sch/i.html?_nkw=${
            URLEncoder.encode(
                query,
                "UTF-8"
            )
        }"
    }

    /*
     * ============================================================
     * PART SEARCH MODEL
     * ============================================================
     */

    private data class PartSearch(
        val part: String,
        val position: String
    )

    /*
     * ============================================================
     * MOT DEFECT -> PART TRANSLATION
     * ============================================================
     */

    private fun buildPartSearch(
        text: String
    ): PartSearch {

        var clean =
            text.replace(
                Regex("\\([^)]*\\)"),
                " "
            )

        val positionParts =
            mutableListOf<String>()

        if (
            Regex(
                "(?i)\\bnearside\\b"
            ).containsMatchIn(clean) ||
            Regex(
                "(?i)\\bnsf\\b|\\bnsr\\b"
            ).containsMatchIn(clean)
        ) {

            positionParts.add(
                "left"
            )
        }

        if (
            Regex(
                "(?i)\\boffside\\b"
            ).containsMatchIn(clean) ||
            Regex(
                "(?i)\\bosf\\b|\\bosr\\b"
            ).containsMatchIn(clean)
        ) {

            positionParts.add(
                "right"
            )
        }

        if (
            Regex(
                "(?i)\\brear\\b|\\bnsr\\b|\\bosr\\b"
            ).containsMatchIn(clean)
        ) {

            positionParts.add(
                "rear"
            )
        }

        if (
            Regex(
                "(?i)\\bfront\\b|\\bnsf\\b|\\bosf\\b"
            ).containsMatchIn(clean)
        ) {

            positionParts.add(
                "front"
            )
        }

        clean =
            clean.replace(
                Regex(
                    "(?i)\\broad\\s+wheels?\\b"
                ),
                "alloy wheel"
            )

        clean =
            clean.replace(
                Regex(
                    "(?i)\\bslightly\\s+distorted\\b"
                ),
                " "
            )

        clean =
            clean.replace(
                Regex(
                    "(?i)\\bdistorted\\b"
                ),
                " "
            )

        clean =
            clean.replace(
                Regex(
                    "(?i)\\bbuckled\\b"
                ),
                " "
            )

        val conditionWords =
            Regex(
                "(?i)\\b(" +
                    "slightly|" +
                    "slight|" +
                    "excessive|" +
                    "excessively|" +
                    "minor|" +
                    "major|" +
                    "worn|" +
                    "damaged|" +
                    "defective|" +
                    "failed|" +
                    "failing|" +
                    "corroded|" +
                    "corrosion|" +
                    "pitted|" +
                    "scored|" +
                    "cracked|" +
                    "split|" +
                    "insecure|" +
                    "secure|" +
                    "leaking|" +
                    "leak|" +
                    "ineffective|" +
                    "restricted|" +
                    "inoperative|" +
                    "noisy|" +
                    "not|" +
                    "working|" +
                    "advisory|" +
                    "with|" +
                    "and|" +
                    "the|" +
                    "a|" +
                    "an|" +
                    "is|" +
                    "are|" +
                    "to|" +
                    "on|" +
                    "failure|" +
                    "deteriorated|" +
                    "deterioration|" +
                    "poor|" +
                    "loose|" +
                    "binding|" +
                    "seized|" +
                    "perished|" +
                    "fractured|" +
                    "wearing|" +
                    "thin|" +
                    "play" +
                    ")\\b"
            )

        clean =
            clean.replace(
                conditionWords,
                " "
            )

        clean =
            clean
                .replace(
                    Regex(
                        "[^A-Za-z0-9\\s]"
                    ),
                    " "
                )
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()

        val lower =
            clean.lowercase()

        val part =
            when {

                lower.contains("brake disc") ||
                    lower.contains("brake rotor") ||
                    lower.contains("disc brake") ->
                    "brake disc"

                lower.contains("brake pad") ||
                    lower.contains("brake pads") ->
                    "brake pads"

                lower.contains("brake caliper") ||
                    lower.contains("caliper") ->
                    "brake caliper"

                lower.contains("brake hose") ||
                    lower.contains("brake pipe") ||
                    lower.contains("brake line") ->
                    "brake hose"

                lower.contains("brake master cylinder") ->
                    "brake master cylinder"

                lower.contains("master cylinder") ->
                    "master cylinder"

                lower.contains("brake fluid") ->
                    "brake fluid"

                lower.contains("handbrake") ||
                    lower.contains("hand brake") ||
                    lower.contains("parking brake") ->
                    "handbrake"

                lower.contains("shock absorber") ||
                    lower.contains("shock absorber unit") ||
                    lower.contains("damper") ->
                    "shock absorber"

                lower.contains("coil spring") ||
                    lower.contains("suspension spring") ->
                    "coil spring"

                lower.contains("wishbone") ->
                    "wishbone"

                lower.contains("suspension arm") ||
                    lower.contains("control arm") ||
                    lower.contains("track control arm") ->
                    "suspension arm"

                lower.contains("ball joint") ->
                    "ball joint"

                lower.contains("track rod end") ->
                    "track rod end"

                lower.contains("track rod") ->
                    "track rod"

                lower.contains("anti-roll bar link") ||
                    lower.contains("anti roll bar link") ||
                    lower.contains("anti-rollbar link") ||
                    lower.contains("drop link") ->
                    "anti roll bar drop link"

                lower.contains("anti-roll bar") ||
                    lower.contains("anti roll bar") ||
                    lower.contains("anti-rollbar") ->
                    "anti roll bar"

                lower.contains("stabiliser link") ||
                    lower.contains("stabilizer link") ->
                    "stabiliser link"

                lower.contains("suspension bush") ||
                    lower.contains("suspension bushes") ||
                    lower.contains("bush") ->
                    "suspension bush"

                lower.contains("wheel bearing") ||
                    lower.contains("hub bearing") ->
                    "wheel bearing"

                lower.contains("wheel hub") ->
                    "wheel hub"

                lower.contains("steel wheel") ->
                    "steel wheel"

                lower.contains("road wheel") ||
                    lower.contains("alloy wheel") ||
                    lower.contains("wheel rim") ||
                    lower.contains("rim") ||
                    lower.contains("wheel") ->
                    "alloy wheel"

                lower.contains("steering rack") ->
                    "steering rack"

                lower.contains("steering column") ->
                    "steering column"

                lower.contains("steering joint") ->
                    "steering joint"

                lower.contains("steering arm") ->
                    "steering arm"

                lower.contains("steering linkage") ->
                    "steering linkage"

                lower.contains("steering gaiter") ->
                    "steering gaiter"

                lower.contains("tyre") ||
                    lower.contains("tire") ->
                    "tyre"

                lower.contains("headlamp") ||
                    lower.contains("head lamp") ||
                    lower.contains("headlight") ||
                    lower.contains("head light") ->
                    "headlamp"

                lower.contains("rear lamp") ||
                    lower.contains("rear light") ||
                    lower.contains("tail lamp") ||
                    lower.contains("tail light") ->
                    "rear light"

                lower.contains("stop lamp") ||
                    lower.contains("brake light") ||
                    lower.contains("stop light") ->
                    "brake light"

                lower.contains("direction indicator") ||
                    lower.contains("directional indicator") ||
                    lower.contains("indicator") ->
                    "indicator"

                lower.contains("fog lamp") ||
                    lower.contains("fog light") ->
                    "fog lamp"

                lower.contains("number plate lamp") ||
                    lower.contains("number plate light") ||
                    lower.contains("registration plate lamp") ->
                    "number plate light"

                lower.contains("windscreen") ||
                    lower.contains("windshield") ->
                    "windscreen"

                lower.contains("wiper blade") ||
                    lower.contains("wiper") ->
                    "windscreen wiper"

                lower.contains("washer jet") ||
                    lower.contains("washer nozzle") ||
                    lower.contains("windscreen washer") ||
                    lower.contains("windshield washer") ->
                    "windscreen washer"

                lower.contains("door mirror") ||
                    lower.contains("wing mirror") ||
                    lower.contains("side mirror") ->
                    "door mirror"

                lower.contains("rear view mirror") ->
                    "rear view mirror"

                lower.contains("registration plate") ||
                    lower.contains("number plate") ->
                    "registration plate"

                lower.contains("tailgate") ->
                    "tailgate"

                lower.contains("boot lid") ||
                    lower.contains("bootlid") ->
                    "boot lid"

                lower.contains("bonnet") ||
                    lower.contains("hood") ->
                    "bonnet"

                lower.contains("door") ->
                    "door"

                lower.contains("catalytic converter") ||
                    lower.contains("catalytic convertor") ||
                    lower.contains("catalyst") ->
                    "catalytic converter"

                lower.contains("diesel particulate filter") ||
                    lower.contains("particulate filter") ||
                    lower.contains("dpf") ->
                    "DPF"

                lower.contains("exhaust silencer") ||
                    lower.contains("silencer") ->
                    "exhaust silencer"

                lower.contains("exhaust manifold") ->
                    "exhaust manifold"

                lower.contains("exhaust") ->
                    "exhaust"

                lower.contains("engine mount") ->
                    "engine mount"

                lower.contains("engine oil") ||
                    lower.contains("oil leak") ->
                    "engine oil"

                lower.contains("coolant leak") ->
                    "coolant leak"

                lower.contains("radiator") ->
                    "radiator"

                lower.contains("coolant hose") ->
                    "coolant hose"

                lower.contains("water pump") ->
                    "water pump"

                lower.contains("clutch") ->
                    "clutch"

                lower.contains("gearbox") ||
                    lower.contains("transmission") ->
                    "gearbox"

                lower.contains("driveshaft") ||
                    lower.contains("drive shaft") ->
                    "driveshaft"

                lower.contains("cv joint") ||
                    lower.contains("cv boot") ||
                    lower.contains("constant velocity") ->
                    "CV joint"

                lower.contains("propshaft") ||
                    lower.contains("prop shaft") ->
                    "propshaft"

                lower.contains("seat belt") ||
                    lower.contains("seatbelt") ->
                    "seat belt"

                lower.contains("airbag") ->
                    "airbag"

                lower.contains("horn") ->
                    "horn"

                lower.contains("fuel cap") ->
                    "fuel cap"

                lower.contains("fuel tank") ->
                    "fuel tank"

                else ->
                    clean
            }

        return PartSearch(
            part =
                part
                    .trim()
                    .lowercase(),

            position =
                positionParts
                    .distinct()
                    .joinToString(
                        " "
                    )
        )
    }

    private fun firstNonBlank(
        vararg values: String?
    ): String {

        return values
            .firstOrNull {
                !it.isNullOrBlank()
            }
            ?.trim()
            .orEmpty()
    }
}