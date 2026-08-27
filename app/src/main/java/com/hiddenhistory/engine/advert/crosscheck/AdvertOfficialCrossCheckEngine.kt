package com.hiddenhistory.engine.advert.crosscheck

import com.hiddenhistory.engine.ParsedVehicleAdvert
import com.hiddenhistory.models.MotTest
import com.hiddenhistory.models.Vehicle
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Deterministic evidence comparison engine between:
 *
 * 1. Claims extracted from the seller advert.
 * 2. Official vehicle data returned by the existing Vehicle pipeline.
 *
 * This engine does NOT perform general vehicle intelligence.
 *
 * Its responsibility is evidence comparison:
 *
 * - advert claims vs official vehicle data
 * - advert mileage vs MOT mileage
 * - MOT chronology
 * - same-day FAIL -> PASS retests
 * - repeated MOT failures/advisories
 * - repeated defect patterns
 * - vehicle identity/specification contradictions
 * - ownership contradictions
 * - salvage/write-off markers where available
 * - MOT status/expiry contradictions
 * - safety/recall/export markers where available
 * - cautious inspection prompts when the evidence pattern warrants
 *   physical verification
 *
 * IMPORTANT:
 *
 * Missing, blank, unknown or unavailable official data is NEVER
 * interpreted as negative evidence.
 *
 * Official history which merely contains an old defect is NOT
 * automatically treated as a contradiction of the seller's advert.
 *
 * A contradiction requires an actual, comparable seller claim.
 */
class AdvertOfficialCrossCheckEngine {

    data class CrossCheckResult(
        val warnings: List<String>,
        val confirmations: List<String>,
        val verificationItems: List<String>
    ) {
        companion object {
            fun empty(): CrossCheckResult =
                CrossCheckResult(
                    warnings = emptyList(),
                    confirmations = emptyList(),
                    verificationItems = emptyList()
                )
        }
    }

    private enum class MileageUnit {
        MILES,
        KILOMETRES,
        UNKNOWN
    }

    private data class AdvertMileage(
        val originalMileage: Int,
        val originalUnit: MileageUnit,
        val miles: Int
    )

    private data class OfficialMileageReading(
        val originalMileage: Int,
        val originalUnit: MileageUnit,
        val miles: Int,
        val date: String?,
        val resultType: String?,
        val testResult: String?,
        val test: MotTest
    )

    private data class MileageRegression(
        val previous: OfficialMileageReading,
        val current: OfficialMileageReading,
        val difference: Int
    )

    private data class ParsedDateTime(
        val raw: String,
        val date: LocalDate?,
        val dateTime: LocalDateTime?
    )

    private data class DefectEvidence(
        val test: MotTest,
        val text: String,
        val type: String?,
        val dangerous: Boolean
    )

    /*
     * A defect is only counted once per MOT test when looking for
     * repeated patterns. This prevents one MOT record containing
     * multiple similar lines from artificially inflating the evidence.
     */
    private data class DefectFamilyEvidence(
        val family: String,
        val testDate: String?,
        val testNumber: String?
    )

    /**
     * Preferred entry point.
     *
     * Uses the existing Vehicle model supplied by the DVSA/DVLA
     * pipeline.
     */
    fun compare(
        advert: ParsedVehicleAdvert,
        vehicle: Vehicle
    ): CrossCheckResult {

        val motTests = vehicle.motTests

        val result =
            compareAdvertAndMot(
                advert = advert,
                motTests = motTests
            )

        val warnings =
            result.warnings.toMutableList()

        val confirmations =
            result.confirmations.toMutableList()

        val verificationItems =
            result.verificationItems.toMutableList()

        /*
         * =========================================================
         * OFFICIAL VEHICLE DATA CROSS-CHECKS
         * =========================================================
         */

        /*
         * Registration.
         */
        crossCheckTextClaim(
            advertValue = advertValue(
                advert,
                "registration",
                "registrationNumber",
                "reg",
                "vrm"
            ),
            officialValue = firstNonBlank(
                vehicle.registrationNumber,
                vehicle.registration
            ),
            displayName = "registration",
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * Make.
         */
        crossCheckMake(
            advertValue = advertValue(
                advert,
                "make",
                "manufacturer",
                "vehicleMake"
            ),
            officialValue = vehicle.make,
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * Model.
         *
         * Model comparison deliberately allows advert trim/variant
         * wording around the official base model.
         */
        crossCheckModel(
            advertValue = advertValue(
                advert,
                "model",
                "vehicleModel"
            ),
            officialValue = vehicle.model,
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * Fuel type.
         */
        crossCheckFuelType(
            advertValue = advertValue(
                advert,
                "fuelType",
                "fuel",
                "fuel_type"
            ),
            officialValue = vehicle.fuelType,
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * Engine capacity.
         */
        crossCheckEngineCapacity(
            advertValue = advertValue(
                advert,
                "engineCapacity",
                "engineSize",
                "engine",
                "engineCapacityCc",
                "engineCc"
            ),
            officialCc = vehicle.engineCapacity
                ?: parseEngineSizeToCc(
                    vehicle.engineSize
                ),
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * Year.
         */
        crossCheckYear(
            advertValue = advertValue(
                advert,
                "year",
                "yearOfManufacture",
                "vehicleYear"
            ),
            officialYear = vehicle.year,
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * Seats.
         */
        crossCheckIntegerClaim(
            label = "seats",
            advertValue = advertValue(
                advert,
                "seats",
                "seatCount"
            ),
            officialValue = vehicle.seats,
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * Tow weight.
         */
        crossCheckNumericClaim(
            label = "maximum towing weight",
            advertValue = advertValue(
                advert,
                "maxTowWeight",
                "maximumTowWeight",
                "towWeight"
            ),
            officialValue = vehicle.maxTowWeight,
            tolerance = 1.0,
            unitText = " kg",
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * MOT expiry.
         */
        crossCheckDateClaim(
            label = "MOT expiry",
            advertValue = advertValue(
                advert,
                "motExpiryDate",
                "motExpiry",
                "motUntil",
                "motDueDate"
            ),
            officialValue = vehicle.motExpiryDate,
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * MOT status.
         */
        crossCheckMotStatus(
            advertValue = advertValue(
                advert,
                "motStatus",
                "mot"
            ),
            officialValue = vehicle.motStatus,
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * Ownership.
         */
        crossCheckOwnerClaim(
            advertValue = advertValue(
                advert,
                "previousOwners",
                "previousKeepers",
                "owners",
                "keepers",
                "ownerCount",
                "keeperCount"
            ),
            advertText = advertText(advert),
            officialOwners = vehicle.previousOwners
                ?: vehicle.previousKeepers,
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * Salvage/write-off.
         */
        crossCheckSalvageClaim(
            advertText = advertText(advert),
            officialSalvageCategory = vehicle.salvageCategory,
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * Export.
         */
        if (vehicle.markedForExport == true) {

            warnings.add(
                "Official vehicle data indicates that the vehicle is " +
                    "marked for export."
            )

            verificationItems.add(
                "Verify the vehicle's current registration/export status " +
                    "and documentation before purchase."
            )

        } else if (vehicle.markedForExport == false) {

            confirmations.add(
                "Official vehicle data does not currently indicate that " +
                    "the vehicle is marked for export."
            )
        }

        /*
         * Outstanding recall.
         */
        if (hasPositiveMarker(vehicle.hasOutstandingRecall)) {

            warnings.add(
                "Official vehicle data indicates an outstanding safety " +
                    "recall marker."
            )

            verificationItems.add(
                "Verify the outstanding recall with the manufacturer or " +
                    "authorised repairer and establish whether the recall " +
                    "work has been completed before purchase."
            )
        }

        /*
         * Tax.
         */
        crossCheckTaxStatus(
            advertValue = advertValue(
                advert,
                "taxStatus",
                "tax"
            ),
            officialValue = vehicle.taxStatus,
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * VIN.
         */
        crossCheckVin(
            advertValue = advertValue(
                advert,
                "vin",
                "vehicleIdentificationNumber"
            ),
            officialValue = vehicle.vin,
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * =========================================================
         * CLAIMS REQUIRING EXTERNAL EVIDENCE
         * =========================================================
         */

        val text =
            advertText(advert)

        if (
            containsAny(
                text,
                "full service history",
                "full service history available",
                "full service history present",
                "full service history included"
            )
        ) {

            verificationItems.add(
                "The advert claims full service history. MOT/DVLA data " +
                    "does not prove a complete service history. Request " +
                    "the service book or digital service record and " +
                    "supporting invoices, checking dates and mileage."
            )
        }

        if (
            containsAny(
                text,
                "hpi clear",
                "hpi checked",
                "hpi check"
            )
        ) {

            verificationItems.add(
                "The advert makes a vehicle-history clearance claim. " +
                    "Do not treat DVLA/DVSA data as proof of an HPI result. " +
                    "Request the actual history-check evidence or obtain " +
                    "an independent vehicle-history check."
            )
        }

        if (
            containsAny(
                text,
                "no outstanding finance",
                "no finance",
                "finance clear",
                "clear finance"
            )
        ) {

            verificationItems.add(
                "The advert makes a finance-status claim. No finance " +
                    "status should be inferred unless an authoritative " +
                    "finance source is actually available."
            )
        }

        /*
         * =========================================================
         * MOT DEFECT PATTERNS
         * =========================================================
         */

        analyseMotDefectPatterns(
            motTests = motTests,
            advertText = text,
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * =========================================================
         * CROSS-EVIDENCE IMPACT PATTERN
         * =========================================================
         */

        analysePotentialImpactPattern(
            motTests = motTests,
            advertText = text,
            officialSalvageCategory = vehicle.salvageCategory,
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        /*
         * =========================================================
         * ACTIVE SYMPTOMS
         * =========================================================
         */

        if (vehicle.activeSymptoms.isNotEmpty()) {

            val symptomCount =
                vehicle.activeSymptoms.count()

            warnings.add(
                "The vehicle record contains $symptomCount active " +
                    "reported symptom${if (symptomCount == 1) "" else "s"}."
            )

            verificationItems.add(
                "Review the reported vehicle symptoms and confirm the " +
                    "seller's explanation and current physical condition " +
                    "before purchase."
            )
        }

        /*
         * =========================================================
         * FINAL DEDUPLICATION
         * =========================================================
         *
         * Do NOT lowercase the output here.
         *
         * The previous implementation normalised complete sentences
         * to lowercase and then only capitalised the first character,
         * which damaged the presentation:
         *
         * "Official MOT..." became:
         * "Official mot..."
         *
         * We only need normal distinct() here because the generated
         * messages are deterministic.
         */
        return CrossCheckResult(
            warnings = warnings
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct(),
            confirmations = confirmations
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct(),
            verificationItems = verificationItems
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        )
    }

    /**
     * Compatibility overload.
     */
    fun compare(
        advert: ParsedVehicleAdvert,
        motTests: List<MotTest>
    ): CrossCheckResult =
        compareAdvertAndMot(
            advert = advert,
            motTests = motTests
        )

    /*
     * =============================================================
     * ADVERT + MOT
     * =============================================================
     */

    private fun compareAdvertAndMot(
        advert: ParsedVehicleAdvert,
        motTests: List<MotTest>
    ): CrossCheckResult {

        if (motTests.isEmpty()) {

            return CrossCheckResult(
                warnings = emptyList(),
                confirmations = emptyList(),
                verificationItems = listOf(
                    "No MOT history was available for comparison. " +
                        "This is not evidence that the vehicle has no MOT " +
                        "issues; the available evidence is incomplete."
                )
            )
        }

        val warnings = mutableListOf<String>()
        val confirmations = mutableListOf<String>()
        val verificationItems = mutableListOf<String>()

        val advertisedMileage =
            extractAdvertMileage(
                advert.mileage
            )

        /*
         * No advert mileage:
         *
         * This is not a warning.
         */
        if (advertisedMileage == null) {

            verificationItems.add(
                "The advert does not contain a usable numerical mileage " +
                    "claim, so mileage could not be cross-checked against " +
                    "official MOT readings."
            )

            analyseMotDefectPatterns(
                motTests = motTests,
                advertText = advertText(advert),
                warnings = warnings,
                confirmations = confirmations,
                verificationItems = verificationItems
            )

            return CrossCheckResult(
                warnings = warnings.distinct(),
                confirmations = confirmations.distinct(),
                verificationItems = verificationItems.distinct()
            )
        }

        val officialReadings =
            motTests.mapNotNull(
                ::buildOfficialMileageReading
            )

        if (officialReadings.isEmpty()) {

            verificationItems.add(
                "The available MOT history does not contain a usable " +
                    "numerical odometer reading that can be safely " +
                    "compared with the advert."
            )

            analyseMotDefectPatterns(
                motTests = motTests,
                advertText = advertText(advert),
                warnings = warnings,
                confirmations = confirmations,
                verificationItems = verificationItems
            )

            return CrossCheckResult(
                warnings = warnings.distinct(),
                confirmations = confirmations.distinct(),
                verificationItems = verificationItems.distinct()
            )
        }

        val chronologicalReadings =
            officialReadings.sortedWith(
                compareBy<OfficialMileageReading> {
                    parseDateTime(it.date)?.date
                        ?: LocalDate.MIN
                }.thenBy {
                    parseDateTime(it.date)?.dateTime
                        ?: LocalDateTime.MIN
                }
            )

        val latestOfficialReading =
            chronologicalReadings.lastOrNull()

        /*
         * ---------------------------------------------------------
         * OFFICIAL READING ABOVE ADVERTISED MILEAGE
         * ---------------------------------------------------------
         *
         * This is a genuine contradiction because a previous official
         * reading cannot legitimately be higher than the seller's
         * currently advertised mileage without explanation.
         */
        val readingsAboveAdvertisedMileage =
            officialReadings.filter {
                it.miles >
                    advertisedMileage.miles +
                        MILEAGE_ROUNDING_TOLERANCE
            }

        val strongestReading =
            readingsAboveAdvertisedMileage.maxByOrNull {
                it.miles
            }

        if (strongestReading != null) {

            val difference =
                strongestReading.miles -
                    advertisedMileage.miles

            warnings.add(
                "Mileage discrepancy detected: the advert states " +
                    "${formatAdvertMileage(advertisedMileage)}, but " +
                    "official MOT history records " +
                    formatOfficialMileage(strongestReading) +
                    "${strongestReading.date?.let { " on $it" } ?: ""}. " +
                    "The official reading is approximately " +
                    "${formatMileage(difference)} miles higher."
            )

            verificationItems.add(
                "Ask the seller to explain why the advertised mileage " +
                    "is lower than a previous official MOT reading. " +
                    "Request service records, MOT certificates and " +
                    "evidence of the current odometer reading."
            )
        }

        /*
         * ---------------------------------------------------------
         * LATEST MOT VS CURRENT ADVERTISED MILEAGE
         * ---------------------------------------------------------
         */

        if (latestOfficialReading != null) {

            val latestDifference =
                latestOfficialReading.miles -
                    advertisedMileage.miles

            if (
                latestDifference >
                    MILEAGE_ROUNDING_TOLERANCE
            ) {

                /*
                 * If another older MOT already proves a higher reading,
                 * don't generate another identical contradiction.
                 */
                if (readingsAboveAdvertisedMileage.isEmpty()) {

                    warnings.add(
                        "The advertised mileage is lower than the latest " +
                            "official MOT mileage. The advert states " +
                            "${formatAdvertMileage(advertisedMileage)}, " +
                            "while the latest official MOT records " +
                            formatOfficialMileage(latestOfficialReading) +
                            ". The difference is approximately " +
                            "${formatMileage(latestDifference)} miles."
                    )
                }

                verificationItems.add(
                    "Verify the current odometer reading directly and " +
                        "ask the seller to explain the difference between " +
                        "the advert mileage and the latest official MOT " +
                        "record."
                )
            }
        }

        /*
         * ---------------------------------------------------------
         * HISTORICAL EXACT MATCH
         * ---------------------------------------------------------
         */

        val matchingReading =
            officialReadings.firstOrNull {
                abs(
                    it.miles -
                        advertisedMileage.miles
                ) <= MILEAGE_ROUNDING_TOLERANCE
            }

        if (matchingReading != null) {

            confirmations.add(
                "The advertised mileage of " +
                    formatAdvertMileage(advertisedMileage) +
                    " matches an official MOT mileage reading" +
                    (
                        matchingReading.date
                            ?.let { " from $it" }
                            ?: ""
                    ) +
                    ". This is supporting historical evidence, not " +
                    "independent proof that the current odometer is genuine."
            )
        }

        /*
         * ---------------------------------------------------------
         * CURRENT MILEAGE ABOVE LAST MOT
         * ---------------------------------------------------------
         *
         * This is normally expected.
         */
        if (
            latestOfficialReading != null &&
            advertisedMileage.miles >
                latestOfficialReading.miles +
                    MILEAGE_ROUNDING_TOLERANCE
        ) {

            val difference =
                advertisedMileage.miles -
                    latestOfficialReading.miles

            if (difference >= SIGNIFICANT_CURRENT_MILEAGE_GAP) {

                verificationItems.add(
                    "The advertised mileage is approximately " +
                        "${formatMileage(difference)} miles above the " +
                        "latest official MOT reading. This may simply " +
                        "reflect mileage accumulated since that MOT, but " +
                        "the current odometer and recent mileage " +
                        "documentation should be checked."
                )

            } else {

                confirmations.add(
                    "The advertised mileage is approximately " +
                        "${formatMileage(difference)} miles above the " +
                        "latest official MOT reading, which is consistent " +
                        "with mileage accumulated since that MOT."
                )
            }
        }

        /*
         * ---------------------------------------------------------
         * OFFICIAL MILEAGE REGRESSION
         * ---------------------------------------------------------
         */

        val regressions =
            findMileageRegressions(
                chronologicalReadings
            )

        regressions.forEach { regression ->

            warnings.add(
                "Official mileage history contains a possible mileage " +
                    "regression: an earlier MOT recorded " +
                    formatOfficialMileage(regression.previous) +
                    ", while a later MOT recorded " +
                    formatOfficialMileage(regression.current) +
                    ". The later reading is approximately " +
                    "${formatMileage(regression.difference)} miles lower."
            )

            verificationItems.add(
                "Investigate the official mileage regression. Ask for " +
                    "service history and documentation relating to any " +
                    "odometer replacement, correction or instrument-cluster " +
                    "work before relying on the stated mileage."
            )
        }

        /*
         * ---------------------------------------------------------
         * UNIT CONVERSION
         * ---------------------------------------------------------
         */

        val unitConversionUsed =
            officialReadings.any {
                it.originalUnit != advertisedMileage.originalUnit
            }

        if (unitConversionUsed) {

            confirmations.add(
                "Mileage comparison accounted for different units in " +
                    "the advert and official MOT history. Kilometre " +
                    "readings were converted to miles for comparison."
            )
        }

        /*
         * ---------------------------------------------------------
         * SAME-DAY FAIL -> PASS
         * ---------------------------------------------------------
         */

        val sameDayRetests =
            findSameDayFailToPassRetests(
                motTests
            )

        sameDayRetests.forEach { retest ->

            warnings.add(
                "MOT retest sequence detected: the vehicle failed an MOT " +
                    "and subsequently passed on the same day" +
                    "${retest.completedDate?.let { " ($it)" } ?: ""}."
            )

            verificationItems.add(
                "Review the failure items from the same-day MOT sequence " +
                    "and establish what was repaired or rectified before " +
                    "purchase. A later pass does not remove the fact that " +
                    "the earlier test identified defects."
            )
        }

        /*
         * ---------------------------------------------------------
         * CLEAN MILEAGE RESULT
         * ---------------------------------------------------------
         */

        if (
            readingsAboveAdvertisedMileage.isEmpty() &&
            regressions.isEmpty() &&
            latestOfficialReading != null &&
            latestOfficialReading.miles <=
                advertisedMileage.miles +
                    MILEAGE_ROUNDING_TOLERANCE &&
            matchingReading == null
        ) {

            confirmations.add(
                "No material mileage contradiction was found between the " +
                    "advertised mileage and the available official MOT " +
                    "mileage history."
            )
        }

        analyseMotDefectPatterns(
            motTests = motTests,
            advertText = advertText(advert),
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        return CrossCheckResult(
            warnings = warnings.distinct(),
            confirmations = confirmations.distinct(),
            verificationItems = verificationItems.distinct()
        )
    }

    /*
     * =============================================================
     * OFFICIAL MOT READING
     * =============================================================
     */

    private fun buildOfficialMileageReading(
        test: MotTest
    ): OfficialMileageReading? {

        val mileage =
            extractMileage(test.odometerValue)
                ?: return null

        val resultType =
            test.odometerResultType
                ?.trim()
                ?.uppercase(Locale.ROOT)

        if (
            resultType == "NO_ODOMETER" ||
            resultType == "UNREADABLE"
        ) {
            return null
        }

        val unit =
            parseOfficialMileageUnit(
                test.odometerUnit
            )

        if (unit == MileageUnit.UNKNOWN) {
            return null
        }

        val miles =
            when (unit) {

                MileageUnit.MILES ->
                    mileage

                MileageUnit.KILOMETRES ->
                    kilometresToMiles(mileage)

                MileageUnit.UNKNOWN ->
                    return null
            }

        return OfficialMileageReading(
            originalMileage = mileage,
            originalUnit = unit,
            miles = miles,
            date = test.completedDate
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                },
            resultType = resultType,
            testResult = test.testResult
                ?.trim()
                ?.uppercase(Locale.ROOT),
            test = test
        )
    }

    /*
     * =============================================================
     * SAME-DAY FAIL -> PASS
     * =============================================================
     */

    private fun findSameDayFailToPassRetests(
        motTests: List<MotTest>
    ): List<MotTest> {

        if (motTests.size < 2) {
            return emptyList()
        }

        val ordered =
            motTests
                .mapNotNull { test ->

                    val parsed =
                        parseDateTime(
                            test.completedDate
                        )
                            ?: return@mapNotNull null

                    test to parsed
                }
                .sortedWith(
                    compareBy<Pair<MotTest, ParsedDateTime>> {
                        it.second.date
                            ?: LocalDate.MIN
                    }.thenBy {
                        it.second.dateTime
                            ?: LocalDateTime.MIN
                    }.thenBy {
                        it.first.motTestNumber.orEmpty()
                    }
                )

        val retests =
            mutableListOf<MotTest>()

        /*
         * Compare tests on the same date.
         */
        val byDate =
            ordered.groupBy {
                it.second.date
            }

        byDate.values.forEach { testsOnDate ->

            for (index in 1 until testsOnDate.size) {

                val previous =
                    testsOnDate[index - 1].first

                val current =
                    testsOnDate[index].first

                if (
                    isFailedTest(
                        previous.testResult
                    ) &&
                    isPassedTest(
                        current.testResult
                    )
                ) {
                    retests.add(current)
                }
            }
        }

        return retests.distinctBy {
            it.motTestNumber
                ?: "${it.completedDate}|${it.testResult}"
        }
    }

    /*
     * =============================================================
     * MOT DEFECT PATTERN ANALYSIS
     * =============================================================
     */

    private fun analyseMotDefectPatterns(
        motTests: List<MotTest>,
        advertText: String,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        if (motTests.isEmpty()) {
            return
        }

        val defects =
            motTests
                .flatMap { test ->

                    test.defects.mapNotNull { defect ->

                        val text =
                            defect.text
                                ?.trim()
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: return@mapNotNull null

                        DefectEvidence(
                            test = test,
                            text = text,
                            type = defect.type
                                ?.trim()
                                ?.uppercase(Locale.ROOT),
                            dangerous = defect.dangerous
                        )
                    }
                }

        if (defects.isEmpty()) {
            return
        }

        /*
         * Dangerous defects are official evidence.
         */
        val dangerousCount =
            defects.count {
                it.dangerous
            }

        if (dangerousCount > 0) {

            warnings.add(
                "Official MOT history contains $dangerousCount " +
                    "dangerous defect${if (dangerousCount == 1) "" else "s"}."
            )

            verificationItems.add(
                "Review every dangerous MOT defect and confirm the " +
                    "associated repair work before purchase."
            )
        }

        /*
         * Repeated defect families.
         *
         * Count distinct MOT records, not individual defect lines.
         */
        val familyEvidence =
            buildDefectFamilyEvidence(
                defects
            )

        familyEvidence
            .groupBy {
                it.family
            }
            .filter {
                it.value
                    .map {
                        it.testNumber
                            ?: it.testDate
                    }
                    .distinct()
                    .size >=
                    MIN_REPEATED_DEFECT_OCCURRENCES
            }
            .forEach { (family, evidence) ->

                val distinctTests =
                    evidence
                        .map {
                            it.testNumber
                                ?: it.testDate
                        }
                        .distinct()
                        .size

                warnings.add(
                    "Repeated MOT defect pattern detected: $family " +
                        "appears across $distinctTests MOT record(s)."
                )

                verificationItems.add(
                    inspectionAdviceForDefectFamily(
                        family
                    )
                )
            }

        /*
         * ---------------------------------------------------------
         * EXPLICIT "NO ADVISORIES" CLAIM
         * ---------------------------------------------------------
         */

        if (
            containsAny(
                advertText,
                "no advisories",
                "no advisory",
                "no advisories on the last mot"
            )
        ) {

            val latest =
                motTests.maxByOrNull {
                    parseDateTime(
                        it.completedDate
                    )?.dateTime
                        ?: LocalDateTime.MIN
                }

            val latestAdvisories =
                latest
                    ?.defects
                    ?.count {
                        it.type
                            ?.trim()
                            ?.equals(
                                "ADVISORY",
                                ignoreCase = true
                            ) == true
                    }
                    ?: 0

            if (latestAdvisories > 0) {

                warnings.add(
                    "The advert claims there were no advisories on the " +
                        "last MOT, but the latest available MOT record " +
                        "contains $latestAdvisories advisory item(s)."
                )

                verificationItems.add(
                    "Ask the seller to explain the discrepancy between " +
                        "the advert's 'no advisories' claim and the official " +
                        "MOT record."
                )
            }
        }

        /*
         * ---------------------------------------------------------
         * EXPLICIT BROAD NO-FAULT CLAIM
         * ---------------------------------------------------------
         */

        if (
            containsAny(
                advertText,
                "no faults whatsoever",
                "no faults",
                "no known mechanical issues",
                "no mechanical issues",
                "no known faults"
            )
        ) {

            val relevant =
                defects.filter {
                    isMechanicallyRelevant(
                        it.text
                    )
                }

            if (relevant.isNotEmpty()) {

                warnings.add(
                    "The advert makes a broad no-fault claim, but official " +
                        "MOT history contains mechanically relevant defect " +
                        "records."
                )

                verificationItems.add(
                    "Review the mechanically relevant MOT defects and ask " +
                        "the seller what repair work was performed. Inspect " +
                        "the affected systems during the viewing/test drive."
                )
            }
        }

        /*
         * Failed tests are evidence, not automatic advert contradictions.
         */
        val failedTests =
            motTests.count {
                isFailedTest(
                    it.testResult
                )
            }

        if (failedTests > 0) {

            confirmations.add(
                "The official MOT history contains $failedTests failed " +
                    "MOT test${if (failedTests == 1) "" else "s"}. " +
                    "Individual failure items should be reviewed rather " +
                    "than treating the latest MOT result in isolation."
            )
        }
    }

    private fun buildDefectFamilyEvidence(
        defects: List<DefectEvidence>
    ): List<DefectFamilyEvidence> {

        return defects.mapNotNull { defect ->

            val family =
                classifyDefectFamily(
                    defect.text
                )
                    ?: return@mapNotNull null

            DefectFamilyEvidence(
                family = family,
                testDate = defect.test.completedDate,
                testNumber = defect.test.motTestNumber
            )
        }
    }

    private fun classifyDefectFamily(
        text: String
    ): String? {

        val lower =
            text.lowercase(Locale.ROOT)

        return when {

            containsAny(
                lower,
                "headlamp",
                "headlamp aim",
                "headlight",
                "headlight aim"
            ) ->
                "headlamp/alignment"

            containsAny(
                lower,
                "suspension",
                "wishbone",
                "ball joint",
                "bush",
                "damper",
                "shock absorber",
                "spring"
            ) ->
                "suspension"

            containsAny(
                lower,
                "oil leak",
                "engine oil leak",
                "oil leakage",
                "fluid leak"
            ) ->
                "oil/fluid leakage"

            containsAny(
                lower,
                "brake",
                "braking",
                "brake disc",
                "brake pad",
                "brake efficiency"
            ) ->
                "braking system"

            containsAny(
                lower,
                "steering",
                "track rod",
                "tie rod",
                "steering rack"
            ) ->
                "steering"

            containsAny(
                lower,
                "tyre",
                "tire",
                "uneven wear"
            ) ->
                "tyres/wheel alignment"

            containsAny(
                lower,
                "exhaust",
                "emission",
                "emissions"
            ) ->
                "exhaust/emissions"

            containsAny(
                lower,
                "engine",
                "engine mount",
                "mounting"
            ) ->
                "engine"

            containsAny(
                lower,
                "gearbox",
                "transmission",
                "clutch"
            ) ->
                "transmission/clutch"

            containsAny(
                lower,
                "body",
                "panel",
                "door",
                "bonnet",
                "boot lid",
                "wing",
                "bumper",
                "structural"
            ) ->
                "body/impact-related"

            else ->
                null
        }
    }

    private fun inspectionAdviceForDefectFamily(
        family: String
    ): String {

        return when (family) {

            "headlamp/alignment" ->
                "Inspect the headlamp housings, mounting points, alignment " +
                    "and surrounding bodywork. Repeated alignment defects " +
                    "can have several causes and should not be assumed to " +
                    "prove collision damage."

            "suspension" ->
                "Inspect suspension components, bushes, ball joints, " +
                    "springs and dampers. During the test drive listen for " +
                    "knocks and check for uneven tyre wear or alignment issues."

            "oil/fluid leakage" ->
                "Inspect the engine bay and underside for fresh oil/fluid, " +
                    "staining, seepage and evidence of previous cleaning or " +
                    "repair. Ask the seller what work was carried out."

            "braking system" ->
                "Inspect discs, pads, calipers and braking performance. " +
                    "Check for vibration, pulling or unusual noises during " +
                    "the test drive."

            "steering" ->
                "Inspect steering components and wheel alignment. During " +
                    "the test drive check for play, pulling, wandering, " +
                    "knocking or unusual steering behaviour."

            "tyres/wheel alignment" ->
                "Inspect all tyres for uneven wear and check wheel " +
                    "alignment. Uneven wear can have multiple causes and " +
                    "should be investigated rather than attributed to one " +
                    "specific fault."

            "exhaust/emissions" ->
                "Inspect the exhaust/emissions system and ask for evidence " +
                    "of any emissions-related repairs or warning-light work."

            "engine" ->
                "Inspect the engine for leaks, unusual noise, vibration, " +
                    "mounting movement and evidence of previous repairs. " +
                    "Match any seller explanation against service invoices."

            "transmission/clutch" ->
                "During the test drive check clutch engagement, slipping, " +
                    "gear selection, noises and transmission behaviour."

            "body/impact-related" ->
                "Inspect body panels, panel gaps, paint finish, fasteners, " +
                    "lamp mounting, bumper alignment and evidence of previous " +
                    "repair. These findings alone do not prove an accident."

            else ->
                "Review the repeated MOT defect entries and inspect the " +
                    "affected system before purchase."
        }
    }

    /*
     * =============================================================
     * POTENTIAL IMPACT PATTERN
     * =============================================================
     */

    private fun analysePotentialImpactPattern(
        motTests: List<MotTest>,
        advertText: String,
        officialSalvageCategory: String?,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        val allDefectText =
            motTests
                .flatMap {
                    it.defects
                }
                .mapNotNull {
                    it.text
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                }
                .map {
                    it.lowercase(Locale.ROOT)
                }

        if (allDefectText.isEmpty()) {
            return
        }

        /*
         * Count distinct signal families, not individual matching
         * defect lines.
         */
        val impactFamilies =
            mutableSetOf<String>()

        allDefectText.forEach { text ->

            if (
                containsAny(
                    text,
                    "headlamp",
                    "headlight"
                )
            ) {
                impactFamilies.add("lighting")
            }

            if (
                containsAny(
                    text,
                    "front suspension",
                    "wishbone",
                    "ball joint"
                )
            ) {
                impactFamilies.add("front suspension")
            }

            if (
                containsAny(
                    text,
                    "steering",
                    "track rod",
                    "tie rod"
                )
            ) {
                impactFamilies.add("steering")
            }

            if (
                containsAny(
                    text,
                    "bumper",
                    "wing",
                    "front panel"
                )
            ) {
                impactFamilies.add("body")
            }

            if (
                containsAny(
                    text,
                    "front brake"
                )
            ) {
                impactFamilies.add("front brakes")
            }
        }

        val bodyClaim =
            containsAny(
                advertText,
                "no accident damage",
                "never accident damaged",
                "never been in an accident",
                "no previous repairs",
                "all panels original",
                "original bodywork",
                "bodywork is original",
                "never repaired"
            )

        val normalisedSalvage =
            normaliseComparableText(
                officialSalvageCategory.orEmpty()
            )

        val salvageMarker =
            !officialSalvageCategory
                .isNullOrBlank() &&
                normalisedSalvage !in setOf(
                    "none",
                    "no",
                    "notstated",
                    "unknown"
                )

        /*
         * Only use the pattern against a strong seller claim.
         */
        if (
            impactFamilies.size >=
                MIN_IMPACT_PATTERN_SIGNALS &&
            bodyClaim
        ) {

            warnings.add(
                "The official MOT history contains several distinct " +
                    "front-end, steering, suspension or body-related " +
                    "signals while the advert makes a strong claim that " +
                    "the vehicle has no accident damage or previous repair."
            )

            verificationItems.add(
                "Perform a careful physical inspection for previous impact " +
                    "or repair: compare panel gaps, paint finish, bumper and " +
                    "lamp alignment, mounting points, fasteners, suspension " +
                    "components and wheel alignment. The MOT pattern does " +
                    "not by itself prove collision damage; it identifies an " +
                    "area that deserves investigation."
            )
        }

        /*
         * Salvage evidence remains independently important.
         */
        if (salvageMarker) {

            val category =
                cleanText(
                    officialSalvageCategory
                )

            warnings.add(
                "Official vehicle data contains a salvage/write-off " +
                    "category marker: $category."
            )

            verificationItems.add(
                "Verify the salvage category and obtain supporting history " +
                    "before relying on any advert claim that the vehicle " +
                    "has never been written off."
            )
        }
    }

    /*
     * =============================================================
     * MAKE
     * =============================================================
     */

    private fun crossCheckMake(
        advertValue: Any?,
        officialValue: String?,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        val advertMake =
            cleanText(advertValue)

        val officialMake =
            cleanText(officialValue)

        if (
            advertMake == null ||
            officialMake == null
        ) {
            return
        }

        if (
            normaliseComparableText(advertMake) ==
            normaliseComparableText(officialMake)
        ) {

            confirmations.add(
                "Advert make '$advertMake' matches the official vehicle " +
                    "record."
            )

        } else {

            warnings.add(
                "Advert make '$advertMake' does not match the official " +
                    "vehicle record '$officialMake'."
            )

            verificationItems.add(
                "Verify the vehicle identity and ask the seller to explain " +
                    "the make discrepancy before purchase."
            )
        }
    }

    /*
     * =============================================================
     * MODEL
     * =============================================================
     */

    private fun crossCheckModel(
        advertValue: Any?,
        officialValue: String?,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        val advertModel =
            cleanText(advertValue)

        val officialModel =
            cleanText(officialValue)

        if (
            advertModel == null ||
            officialModel == null
        ) {
            return
        }

        val advertNormalised =
            normaliseVehicleModel(
                advertModel
            )

        val officialNormalised =
            normaliseVehicleModel(
                officialModel
            )

        /*
         * Exact model match.
         */
        if (
            advertNormalised ==
            officialNormalised
        ) {

            confirmations.add(
                "Advert model '$advertModel' matches the official " +
                    "vehicle record."
            )

            return
        }

        /*
         * Advert may contain trim/variant wording around the official
         * base model.
         */
        if (
            advertNormalised.contains(
                officialNormalised
            ) ||
            officialNormalised.contains(
                advertNormalised
            )
        ) {

            confirmations.add(
                "Advert model '$advertModel' is consistent with the " +
                    "official base model '$officialModel'; additional " +
                    "advert wording may represent a trim or variant."
            )

            return
        }

        warnings.add(
            "Advert model '$advertModel' does not match the official " +
                "vehicle record '$officialModel'."
        )

        verificationItems.add(
            "Verify the vehicle identity, specification and VIN because " +
                "the advertised model differs from the official record."
        )
    }

    private fun normaliseVehicleModel(
        value: String
    ): String =
        value
            .lowercase(Locale.ROOT)
            .replace(
                Regex("[^a-z0-9]"),
                ""
            )

    /*
     * =============================================================
     * GENERIC TEXT CLAIM
     * =============================================================
     */

    private fun crossCheckTextClaim(
        advertValue: Any?,
        officialValue: String?,
        displayName: String,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        val advertText =
            cleanText(advertValue)

        val officialText =
            cleanText(officialValue)

        if (
            advertText == null ||
            officialText == null
        ) {
            return
        }

        if (
            normaliseComparableText(advertText) ==
            normaliseComparableText(officialText)
        ) {

            confirmations.add(
                "Advert $displayName '$advertText' matches the official " +
                    "vehicle record."
            )

        } else {

            warnings.add(
                "Advert $displayName '$advertText' does not match the " +
                    "official vehicle record '$officialText'."
            )

            verificationItems.add(
                "Verify the vehicle identity and ask the seller to explain " +
                    "the $displayName discrepancy before purchase."
            )
        }
    }

    /*
     * =============================================================
     * FUEL
     * =============================================================
     */

    private fun crossCheckFuelType(
        advertValue: Any?,
        officialValue: String?,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        val advertFuel =
            normaliseFuelType(
                cleanText(advertValue)
            )

        val officialFuel =
            normaliseFuelType(
                cleanText(officialValue)
            )

        if (
            advertFuel == null ||
            officialFuel == null
        ) {
            return
        }

        if (
            advertFuel ==
            officialFuel
        ) {

            confirmations.add(
                "Advert fuel type '$advertFuel' matches the official " +
                    "vehicle record."
            )

        } else {

            warnings.add(
                "Fuel type discrepancy detected: the advert states " +
                    "'$advertFuel' while the official vehicle record " +
                    "states '$officialFuel'."
            )

            verificationItems.add(
                "Verify the vehicle identity and fuel type from the " +
                    "vehicle documentation and physical vehicle before " +
                    "purchase."
            )
        }
    }

    private fun normaliseFuelType(
        value: String?
    ): String? {

        val text =
            cleanText(value)
                ?.lowercase(Locale.ROOT)
                ?: return null

        /*
         * Specific combinations must be checked BEFORE component
         * words such as "diesel" or "electric".
         */
        return when {

            containsAny(
                text,
                "plug in hybrid",
                "plug-in hybrid",
                "phev"
            ) ->
                "plug-in hybrid"

            containsAny(
                text,
                "hybrid"
            ) &&
                containsAny(
                    text,
                    "diesel"
                ) ->
                "diesel hybrid"

            containsAny(
                text,
                "hybrid"
            ) &&
                containsAny(
                    text,
                    "petrol",
                    "gasoline"
                ) ->
                "petrol hybrid"

            containsAny(
                text,
                "electric diesel",
                "diesel electric"
            ) ->
                "diesel hybrid"

            containsAny(
                text,
                "electric petrol",
                "petrol electric"
            ) ->
                "petrol hybrid"

            containsAny(
                text,
                "electric",
                "fully electric",
                "battery electric",
                "bev",
                "ev"
            ) ->
                "electric"

            containsAny(
                text,
                "diesel"
            ) ->
                "diesel"

            containsAny(
                text,
                "petrol",
                "gasoline"
            ) ->
                "petrol"

            containsAny(
                text,
                "lpg"
            ) ->
                "lpg"

            containsAny(
                text,
                "cng"
            ) ->
                "cng"

            containsAny(
                text,
                "lng"
            ) ->
                "lng"

            else ->
                normaliseComparableText(text)
        }
    }

    /*
     * =============================================================
     * ENGINE CAPACITY
     * =============================================================
     */

    private fun crossCheckEngineCapacity(
        advertValue: Any?,
        officialCc: Int?,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        val official =
            officialCc
                ?: return

        val advertCc =
            parseCcFromText(
                cleanText(advertValue)
            )
                ?: return

        val tolerance =
            maxOf(
                ENGINE_CC_ABSOLUTE_TOLERANCE,
                (
                    official *
                        ENGINE_CC_PERCENT_TOLERANCE
                    ).roundToInt()
            )

        if (
            abs(
                advertCc -
                    official
            ) <= tolerance
        ) {

            confirmations.add(
                "Advert engine description is consistent with the " +
                    "official engine capacity of ${official}cc."
            )

        } else {

            warnings.add(
                "Engine capacity discrepancy detected: the advert " +
                    "describes approximately ${advertCc}cc while the " +
                    "official vehicle record reports ${official}cc."
            )

            verificationItems.add(
                "Verify the vehicle specification, VIN and engine identity " +
                    "because the advertised engine description does not " +
                    "match the official capacity."
            )
        }
    }

    /*
     * =============================================================
     * YEAR
     * =============================================================
     */

    private fun crossCheckYear(
        advertValue: Any?,
        officialYear: Int?,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        val advertYear =
            extractYear(
                cleanText(advertValue)
            )

        if (
            advertYear == null ||
            officialYear == null
        ) {
            return
        }

        if (
            advertYear ==
            officialYear
        ) {

            confirmations.add(
                "Advert vehicle year $advertYear matches the official " +
                    "vehicle record."
            )

        } else {

            warnings.add(
                "Vehicle year discrepancy detected: the advert states " +
                    "$advertYear while the official vehicle record " +
                    "reports $officialYear."
            )

            verificationItems.add(
                "Verify the vehicle identity, registration date and VIN " +
                    "before relying on the advertised year."
            )
        }
    }

    /*
     * =============================================================
     * INTEGER CLAIM
     * =============================================================
     */

    private fun crossCheckIntegerClaim(
        label: String,
        advertValue: Any?,
        officialValue: Int?,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        val advertNumber =
            extractInteger(
                cleanText(advertValue)
            )

        if (
            advertNumber == null ||
            officialValue == null
        ) {
            return
        }

        if (
            advertNumber ==
            officialValue
        ) {

            confirmations.add(
                "Advert $label value matches the official vehicle record."
            )

        } else {

            warnings.add(
                "Advert $label value $advertNumber does not match the " +
                    "official vehicle value $officialValue."
            )

            verificationItems.add(
                "Verify the vehicle specification because the advertised " +
                    "$label value differs from the official record."
            )
        }
    }

    /*
     * =============================================================
     * NUMERIC CLAIM
     * =============================================================
     */

    private fun crossCheckNumericClaim(
        label: String,
        advertValue: Any?,
        officialValue: Double?,
        tolerance: Double,
        unitText: String,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        val advertNumber =
            extractDouble(
                cleanText(advertValue)
            )

        if (
            advertNumber == null ||
            officialValue == null
        ) {
            return
        }

        if (
            abs(
                advertNumber -
                    officialValue
            ) <= tolerance
        ) {

            confirmations.add(
                "Advert $label is consistent with the official vehicle " +
                    "record."
            )

        } else {

            warnings.add(
                "Advert $label of $advertNumber$unitText does not match " +
                    "the official vehicle value of $officialValue$unitText."
            )

            verificationItems.add(
                "Verify the vehicle specification and supporting " +
                    "documentation for the advertised $label."
            )
        }
    }

    /*
     * =============================================================
     * DATE CLAIM
     * =============================================================
     */

    private fun crossCheckDateClaim(
        label: String,
        advertValue: Any?,
        officialValue: String?,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        val advertDate =
            parseAdvertDate(
                cleanText(advertValue)
            )

        val officialDate =
            parseAdvertDate(
                cleanText(officialValue)
            )

        if (
            advertDate == null ||
            officialDate == null
        ) {
            return
        }

        if (
            advertDate ==
            officialDate
        ) {

            confirmations.add(
                "Advert $label is consistent with the official vehicle " +
                    "record."
            )

        } else {

            warnings.add(
                "Advert $label does not match the official vehicle " +
                    "record. Advert: $advertDate; official: $officialDate."
            )

            verificationItems.add(
                "Verify the current MOT documentation and ask the seller " +
                    "to explain the $label discrepancy."
            )
        }
    }

    /*
     * =============================================================
     * MOT STATUS
     * =============================================================
     */

    private fun crossCheckMotStatus(
        advertValue: Any?,
        officialValue: String?,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        val advertStatus =
            normaliseStatus(
                cleanText(advertValue)
            )

        val officialStatus =
            normaliseStatus(
                cleanText(officialValue)
            )

        if (
            advertStatus == null ||
            officialStatus == null
        ) {
            return
        }

        if (
            statusesEquivalent(
                advertStatus,
                officialStatus
            )
        ) {

            confirmations.add(
                "Advert MOT status '$advertStatus' is consistent with " +
                    "the official vehicle record."
            )

        } else {

            warnings.add(
                "MOT status discrepancy detected: the advert states " +
                    "'$advertStatus' while the official vehicle record " +
                    "states '$officialStatus'."
            )

            verificationItems.add(
                "Verify the current MOT status directly from the official " +
                    "record before purchase."
            )
        }
    }

    private fun statusesEquivalent(
        first: String,
        second: String
    ): Boolean {

        if (first == second) {
            return true
        }

        val firstNormalised =
            normaliseStatusForComparison(first)

        val secondNormalised =
            normaliseStatusForComparison(second)

        return firstNormalised ==
            secondNormalised
    }

    private fun normaliseStatusForComparison(
        value: String
    ): String {

        val upper =
            value.uppercase(Locale.ROOT)

        return when {

            upper in setOf(
                "PASS",
                "PASSED",
                "VALID",
                "CURRENT",
                "IN FORCE"
            ) ->
                "POSITIVE"

            upper in setOf(
                "FAIL",
                "FAILED",
                "INVALID",
                "EXPIRED"
            ) ->
                "NEGATIVE"

            else ->
                upper
        }
    }

    /*
     * =============================================================
     * OWNER CLAIM
     * =============================================================
     */

    private fun crossCheckOwnerClaim(
        advertValue: Any?,
        advertText: String,
        officialOwners: Int?,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        val advertOwnerNumber =
            extractInteger(
                cleanText(advertValue)
            )

        if (
            advertOwnerNumber != null &&
            officialOwners != null
        ) {

            if (
                advertOwnerNumber ==
                officialOwners
            ) {

                confirmations.add(
                    "Advert owner/keeper count is consistent with the " +
                        "official vehicle record."
                )

            } else {

                warnings.add(
                    "Advert owner/keeper count $advertOwnerNumber does not " +
                        "match the official vehicle record of $officialOwners."
                )

                verificationItems.add(
                    "Ask the seller to explain the ownership/keeper " +
                        "difference and verify the V5C and vehicle history."
                )
            }
        }

        /*
         * Semantic ownership claim.
         */
        if (
            containsAny(
                advertText,
                "one owner",
                "1 owner",
                "one careful owner",
                "one owner from new"
            ) &&
            officialOwners != null &&
            officialOwners > 1
        ) {

            warnings.add(
                "The advert claims one owner from new, but the official " +
                    "vehicle record reports $officialOwners previous " +
                    "owner/keeper record(s)."
            )

            verificationItems.add(
                "Ask the seller to explain the ownership history and " +
                    "verify the V5C and keeper history."
            )
        }
    }

    /*
     * =============================================================
     * SALVAGE
     * =============================================================
     */

    private fun crossCheckSalvageClaim(
        advertText: String,
        officialSalvageCategory: String?,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        val category =
            cleanText(
                officialSalvageCategory
            )
                ?: return

        val normalisedCategory =
            category
                .uppercase(Locale.ROOT)
                .replace(
                    Regex("\\s+"),
                    " "
                )

        val writtenOffClaim =
            containsAny(
                advertText,
                "never been written off",
                "never written off",
                "not written off"
            )

        if (
            normalisedCategory ==
            "PENDING"
        ) {

            if (writtenOffClaim) {

                warnings.add(
                    "The advert claims the vehicle has never been written " +
                        "off, but the available official salvage/write-off " +
                        "status is 'PENDING'. This is not proof of a write-off."
                )

            } else {

                warnings.add(
                    "Official vehicle data contains a salvage/write-off " +
                        "status marker of 'PENDING'. This is not proof of a " +
                        "write-off; the status requires independent verification."
                )
            }

            verificationItems.add(
                "Verify the pending salvage/write-off status using an " +
                    "independent vehicle-history source before purchase."
            )

            return
        }

        val noSalvage =
            normalisedCategory in setOf(
                "NONE",
                "NO",
                "NOT STATED",
                "UNKNOWN"
            )

        if (!noSalvage) {

            if (writtenOffClaim) {

                warnings.add(
                    "The advert claims the vehicle has never been written " +
                        "off, but official vehicle data contains salvage " +
                        "category '$category'."
                )

            } else {

                warnings.add(
                    "Official vehicle data contains salvage category " +
                        "'$category'."
                )
            }

            verificationItems.add(
                "Verify the salvage category and obtain supporting vehicle " +
                    "history before purchase."
            )

        } else if (writtenOffClaim) {

            confirmations.add(
                "The available official vehicle record does not currently " +
                    "contain a salvage category indicating a recorded " +
                    "write-off."
            )
        }
    }

    /*
     * =============================================================
     * TAX
     * =============================================================
     */

    private fun crossCheckTaxStatus(
        advertValue: Any?,
        officialValue: String?,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        val advertStatus =
            normaliseStatus(
                cleanText(advertValue)
            )

        val officialStatus =
            normaliseStatus(
                cleanText(officialValue)
            )

        if (
            advertStatus == null ||
            officialStatus == null
        ) {
            return
        }

        if (
            statusesEquivalent(
                advertStatus,
                officialStatus
            )
        ) {

            confirmations.add(
                "Advert tax status is consistent with the official " +
                    "vehicle record."
            )

        } else {

            warnings.add(
                "Tax status discrepancy detected: the advert states " +
                    "'$advertStatus' while the official vehicle record " +
                    "states '$officialStatus'."
            )

            verificationItems.add(
                "Verify the current tax status and seller's explanation " +
                    "before purchase."
            )
        }
    }

    /*
     * =============================================================
     * VIN
     * =============================================================
     */

    private fun crossCheckVin(
        advertValue: Any?,
        officialValue: String?,
        warnings: MutableList<String>,
        confirmations: MutableList<String>,
        verificationItems: MutableList<String>
    ) {

        val advertVin =
            cleanText(advertValue)

        val officialVin =
            cleanText(officialValue)

        if (
            advertVin == null ||
            officialVin == null
        ) {
            return
        }

        if (
            normaliseComparableText(advertVin) ==
            normaliseComparableText(officialVin)
        ) {

            confirmations.add(
                "Advert VIN matches the official vehicle record."
            )

        } else {

            warnings.add(
                "VIN discrepancy detected between the advert and official " +
                    "vehicle record."
            )

            verificationItems.add(
                "Do not proceed until the VIN on the vehicle, V5C and " +
                    "official record have been reconciled."
            )
        }
    }

    /*
     * =============================================================
     * ADVERT DATA ACCESS
     * =============================================================
     */

    private fun advertValue(
        advert: ParsedVehicleAdvert,
        vararg names: String
    ): Any? {

        val clazz =
            advert::class.java

        names.forEach { name ->

            val getterName =
                "get" +
                    name.replaceFirstChar {
                        it.uppercaseChar()
                    }

            try {

                val getter =
                    clazz.methods.firstOrNull {
                        it.name == getterName &&
                            it.parameterTypes.isEmpty()
                    }

                if (getter != null) {

                    val value =
                        getter.invoke(advert)

                    if (
                        value != null &&
                        value.toString().isNotBlank()
                    ) {
                        return value
                    }
                }

            } catch (_: Exception) {
                /*
                 * Unavailable parser field = unavailable evidence.
                 */
            }

            try {

                val field =
                    clazz.declaredFields.firstOrNull {
                        it.name == name
                    }

                if (field != null) {

                    field.isAccessible = true

                    val value =
                        field.get(advert)

                    if (
                        value != null &&
                        value.toString().isNotBlank()
                    ) {
                        return value
                    }
                }

            } catch (_: Exception) {
                /*
                 * Unavailable parser field = unavailable evidence.
                 */
            }
        }

        return null
    }

    private fun advertText(
        advert: ParsedVehicleAdvert
    ): String {

        val candidates =
            listOf(
                advertValue(
                    advert,
                    "description",
                    "advertText",
                    "text",
                    "listingText",
                    "rawText",
                    "content"
                ),
                advertValue(
                    advert,
                    "title",
                    "headline"
                )
            )

        return candidates
            .mapNotNull {
                cleanText(it)
            }
            .joinToString(" ")
    }

    /*
     * =============================================================
     * GENERAL PARSERS
     * =============================================================
     */

    private fun cleanText(
        value: Any?
    ): String? {

        if (value == null) {
            return null
        }

        val text =
            value
                .toString()
                .trim()

        return text.takeIf {
            it.isNotBlank() &&
                !it.equals(
                    "null",
                    ignoreCase = true
                )
        }
    }

    private fun firstNonBlank(
        vararg values: String?
    ): String? =
        values
            .mapNotNull(::cleanText)
            .firstOrNull()

    private fun normaliseComparableText(
        value: String
    ): String =
        value
            .lowercase(Locale.ROOT)
            .replace(
                Regex("[^a-z0-9]"),
                ""
            )

    private fun normaliseStatus(
        value: String?
    ): String? =
        cleanText(value)
            ?.uppercase(Locale.ROOT)
            ?.replace(
                Regex("\\s+"),
                " "
            )

    private fun extractInteger(
        text: String?
    ): Int? {

        val value =
            cleanText(text)
                ?: return null

        return Regex(
            """(?<!\d)(\d[\d, ]*)(?!\d)"""
        )
            .find(value)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(
                Regex("[^0-9]"),
                ""
            )
            ?.toIntOrNull()
    }

    private fun extractDouble(
        text: String?
    ): Double? {

        val value =
            cleanText(text)
                ?: return null

        val match =
            Regex(
                """(?<!\d)(\d[\d, ]*(?:\.\d+)?)(?!\d)"""
            )
                .find(value)
                ?.groupValues
                ?.getOrNull(1)
                ?: return null

        return match
            .replace(
                Regex("[^0-9.]"),
                ""
            )
            .toDoubleOrNull()
    }

    private fun extractYear(
        text: String?
    ): Int? {

        val value =
            cleanText(text)
                ?: return null

        return Regex(
            """\b(19\d{2}|20\d{2})\b"""
        )
            .find(value)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
    }

    /*
     * =============================================================
     * ENGINE CAPACITY PARSING
     * =============================================================
     */

    private fun parseEngineSizeToCc(
        value: String?
    ): Int? {

        val text =
            cleanText(value)
                ?: return null

        val litre =
            Regex(
                """(?i)^\s*(\d+(?:\.\d+)?)\s*(?:l|litre|litres|liter|liters)?\s*$"""
            )
                .matchEntire(text)
                ?.groupValues
                ?.getOrNull(1)
                ?.toDoubleOrNull()

        if (litre != null) {

            return when {

                litre in 0.5..10.0 ->
                    (litre * 1000.0).roundToInt()

                litre >= 500.0 ->
                    litre.roundToInt()

                else ->
                    null
            }
        }

        return extractInteger(text)
            ?.takeIf {
                it in 500..10000
            }
    }

    private fun parseCcFromText(
        text: String?
    ): Int? {

        val value =
            cleanText(text)
                ?: return null

        val explicitCc =
            Regex(
                """(?i)\b(\d{3,5})\s*(?:cc|cm3|cm³|cubic\s*centimet(?:re|res))\b"""
            )
                .find(value)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()

        if (explicitCc != null) {
            return explicitCc
        }

        val litre =
            Regex(
                """(?i)\b(\d+(?:\.\d+)?)\s*(?:l|litre|litres|liter|liters)\b"""
            )
                .find(value)
                ?.groupValues
                ?.getOrNull(1)
                ?.toDoubleOrNull()

        if (litre != null) {
            return (
                litre *
                    1000.0
                ).roundToInt()
        }

        return extractInteger(value)
            ?.takeIf {
                it in 500..10000
            }
    }

    /*
     * =============================================================
     * MILEAGE
     * =============================================================
     *
     * IMPORTANT:
     *
     * Advert mileage may be expressed as:
     *
     * 92000
     * 92,000
     * 92 K
     * 92k
     * 92 thousand
     * 92K miles
     *
     * The parser already normalises this in rawExtractedAttributes,
     * but this cross-check engine must correctly interpret the original
     * advert.mileage value independently.
     */
    private fun extractAdvertMileage(
        value: Any?
    ): AdvertMileage? {

        val text =
            cleanText(value)
                ?: return null

        val lower =
            text.lowercase(Locale.ROOT)

        /*
         * ---------------------------------------------------------
         * UNIT
         * ---------------------------------------------------------
         */

        val unit =
            when {

                lower.contains("km") ||
                    lower.contains("kilometre") ||
                    lower.contains("kilometer") ->
                    MileageUnit.KILOMETRES

                lower.contains("mile") ||
                    Regex(
                        """\bmi\b"""
                    ).containsMatchIn(lower) ->
                    MileageUnit.MILES

                else ->
                    MileageUnit.MILES
            }

        /*
         * ---------------------------------------------------------
         * NUMBER
         * ---------------------------------------------------------
         *
         * First detect "K" / "thousand" shorthand.
         *
         * Examples:
         *
         * 92K
         * 92 K
         * 92k miles
         * 92 thousand miles
         * 92.5K
         */
        val thousandMatch =
            Regex(
                """(?i)\b(\d+(?:[.,]\d+)?)\s*(?:k|thousand)\b"""
            )
                .find(lower)

        val mileage: Int

        if (thousandMatch != null) {

            val numericPart =
                thousandMatch
                    .groupValues
                    .getOrNull(1)
                    ?.replace(",", "")
                    ?: return null

            val numericValue =
                numericPart
                    .toDoubleOrNull()
                    ?: return null

            mileage =
                (
                    numericValue *
                        1000.0
                    ).roundToInt()

        } else {

            /*
             * Standard full numerical mileage.
             *
             * Examples:
             *
             * 92,000
             * 92000
             * 92 000
             */
            mileage =
                Regex(
                    """(?<!\d)(\d[\d, ]*)(?:\.\d+)?"""
                )
                    .find(text)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.replace(
                        Regex("[^0-9]"),
                        ""
                    )
                    ?.toIntOrNull()
                    ?: return null
        }

        val miles =
            when (unit) {

                MileageUnit.MILES ->
                    mileage

                MileageUnit.KILOMETRES ->
                    kilometresToMiles(
                        mileage
                    )

                MileageUnit.UNKNOWN ->
                    return null
            }

        return AdvertMileage(
            originalMileage = mileage,
            originalUnit = unit,
            miles = miles
        )
    }

    private fun extractMileage(
        value: Any?
    ): Int? {

        val text =
            cleanText(value)
                ?: return null

        return text
            .replace(
                Regex("[^0-9]"),
                ""
            )
            .toIntOrNull()
    }

    private fun parseOfficialMileageUnit(
        value: Any?
    ): MileageUnit {

        val unit =
            cleanText(value)
                ?.uppercase(Locale.ROOT)
                ?: return MileageUnit.UNKNOWN

        return when (unit) {

            "MI",
            "MILE",
            "MILES" ->
                MileageUnit.MILES

            "KM",
            "KMS",
            "KILOMETRE",
            "KILOMETRES",
            "KILOMETER",
            "KILOMETERS" ->
                MileageUnit.KILOMETRES

            else ->
                MileageUnit.UNKNOWN
        }
    }

    private fun kilometresToMiles(
        kilometres: Int
    ): Int =
        (
            kilometres /
                KILOMETRES_PER_MILE
        ).roundToInt()

    private fun findMileageRegressions(
        readings: List<OfficialMileageReading>
    ): List<MileageRegression> {

        if (readings.size < 2) {
            return emptyList()
        }

        val regressions =
            mutableListOf<MileageRegression>()

        for (index in 1 until readings.size) {

            val previous =
                readings[index - 1]

            val current =
                readings[index]

            val difference =
                previous.miles -
                    current.miles

            if (
                difference >=
                    SIGNIFICANT_MILEAGE_REGRESSION
            ) {

                regressions.add(
                    MileageRegression(
                        previous = previous,
                        current = current,
                        difference = difference
                    )
                )
            }
        }

        return regressions
    }

    /*
     * =============================================================
     * DATE HELPERS
     * =============================================================
     */

    private fun parseDateTime(
        value: String?
    ): ParsedDateTime? {

        val raw =
            cleanText(value)
                ?: return null

        val offsetDateTime =
            try {

                OffsetDateTime.parse(
                    raw,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                )

            } catch (_: DateTimeParseException) {
                null
            }

        if (offsetDateTime != null) {

            return ParsedDateTime(
                raw = raw,
                date = offsetDateTime.toLocalDate(),
                dateTime = offsetDateTime.toLocalDateTime()
            )
        }

        val instant =
            try {

                Instant.parse(
                    raw
                )

            } catch (_: DateTimeParseException) {
                null
            }

        if (instant != null) {

            val localDateTime =
                LocalDateTime.ofInstant(
                    instant,
                    ZoneOffset.UTC
                )

            return ParsedDateTime(
                raw = raw,
                date = localDateTime.toLocalDate(),
                dateTime = localDateTime
            )
        }

        val dateTime =
            DATE_TIME_FORMATTERS
                .asSequence()
                .mapNotNull { formatter ->

                    try {

                        LocalDateTime.parse(
                            raw,
                            formatter
                        )

                    } catch (_: DateTimeParseException) {
                        null
                    }
                }
                .firstOrNull()

        if (dateTime != null) {

            return ParsedDateTime(
                raw = raw,
                date = dateTime.toLocalDate(),
                dateTime = dateTime
            )
        }

        val date =
            DATE_FORMATTERS
                .asSequence()
                .mapNotNull { formatter ->

                    try {

                        LocalDate.parse(
                            raw,
                            formatter
                        )

                    } catch (_: DateTimeParseException) {
                        null
                    }
                }
                .firstOrNull()

        return date?.let {

            ParsedDateTime(
                raw = raw,
                date = it,
                dateTime = null
            )
        }
    }

    private fun parseAdvertDate(
        value: String?
    ): LocalDate? {

        val text =
            cleanText(value)
                ?: return null

        parseDateTime(
            text
        )?.date?.let {
            return it
        }

        val monthYear =
            Regex(
                """(?i)\b(January|February|March|April|May|June|July|August|September|October|November|December)\s+(19\d{2}|20\d{2})\b"""
            )
                .find(text)

        if (monthYear != null) {

            val month =
                monthYear
                    .groupValues[1]

            val year =
                monthYear
                    .groupValues[2]

            return try {

                LocalDate.parse(
                    "1 $month $year",
                    DateTimeFormatter.ofPattern(
                        "d MMMM yyyy",
                        Locale.UK
                    )
                )

            } catch (_: DateTimeParseException) {
                null
            }
        }

        return null
    }

    /*
     * =============================================================
     * TEST RESULT HELPERS
     * =============================================================
     */

    private fun isFailedTest(
        result: String?
    ): Boolean {

        val value =
            cleanText(result)
                ?.uppercase(Locale.ROOT)
                ?: return false

        return value == "FAILED" ||
            value == "FAIL"
    }

    private fun isPassedTest(
        result: String?
    ): Boolean {

        val value =
            cleanText(result)
                ?.uppercase(Locale.ROOT)
                ?: return false

        return value == "PASSED" ||
            value == "PASS"
    }

    /*
     * =============================================================
     * TEXT CLAIM HELPERS
     * =============================================================
     */

    private fun containsAny(
        text: String,
        vararg phrases: String
    ): Boolean {

        val lower =
            text.lowercase(Locale.ROOT)

        return phrases.any { phrase ->

            lower.contains(
                phrase.lowercase(Locale.ROOT)
            )
        }
    }

    private fun hasPositiveMarker(
        value: String?
    ): Boolean {

        val text =
            cleanText(value)
                ?.lowercase(Locale.ROOT)
                ?: return false

        return text in setOf(
            "yes",
            "true",
            "y",
            "outstanding",
            "recall",
            "active"
        )
    }

    private fun isMechanicallyRelevant(
        text: String
    ): Boolean {

        val lower =
            text.lowercase(Locale.ROOT)

        return containsAny(
            lower,
            "engine",
            "oil leak",
            "suspension",
            "steering",
            "brake",
            "braking",
            "clutch",
            "gearbox",
            "transmission",
            "wheel bearing",
            "tyre",
            "tire",
            "headlamp",
            "headlight",
            "emission",
            "exhaust"
        )
    }

    /*
     * =============================================================
     * DISPLAY HELPERS
     * =============================================================
     */

    private fun formatMileage(
        mileage: Int
    ): String =
        "%,d".format(
            Locale.UK,
            mileage
        )

    private fun formatAdvertMileage(
        mileage: AdvertMileage
    ): String =
        when (
            mileage.originalUnit
        ) {

            MileageUnit.MILES ->
                "${formatMileage(mileage.originalMileage)} miles"

            MileageUnit.KILOMETRES ->
                "${formatMileage(mileage.originalMileage)} km"

            MileageUnit.UNKNOWN ->
                formatMileage(
                    mileage.originalMileage
                )
        }

    private fun formatOfficialMileage(
        reading: OfficialMileageReading
    ): String =
        when (
            reading.originalUnit
        ) {

            MileageUnit.MILES ->
                "${formatMileage(reading.originalMileage)} miles"

            MileageUnit.KILOMETRES ->
                "${formatMileage(reading.originalMileage)} km"

            MileageUnit.UNKNOWN ->
                formatMileage(
                    reading.originalMileage
                )
        }

    companion object {

        /*
         * Mileage comparison.
         */
        private const val
            MILEAGE_ROUNDING_TOLERANCE =
                10

        private const val
            SIGNIFICANT_CURRENT_MILEAGE_GAP =
                5_000

        private const val
            SIGNIFICANT_MILEAGE_REGRESSION =
                1_000

        private const val
            KILOMETRES_PER_MILE =
                1.609344

        /*
         * Engine capacity.
         */
        private const val
            ENGINE_CC_ABSOLUTE_TOLERANCE =
                100

        private const val
            ENGINE_CC_PERCENT_TOLERANCE =
                0.05

        /*
         * Repeated defect evidence.
         */
        private const val
            MIN_REPEATED_DEFECT_OCCURRENCES =
                2

        /*
         * Impact pattern requires three distinct signal families.
         */
        private const val
            MIN_IMPACT_PATTERN_SIGNALS =
                3

        private val DATE_TIME_FORMATTERS =
            listOf(

                DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
                ),

                DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd'T'HH:mm:ss"
                ),

                DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm"
                ),

                DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd'T'HH:mm"
                ),

                DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm:ss"
                ),

                DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm"
                )
            )

        private val DATE_FORMATTERS =
            listOf(

                DateTimeFormatter.ISO_LOCAL_DATE,

                DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy"
                ),

                DateTimeFormatter.ofPattern(
                    "dd-MM-yyyy"
                )
            )
    }
}