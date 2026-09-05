package com.hiddenhistory.engine.advert.crosscheck

import com.hiddenhistory.engine.ParsedVehicleAdvert
import com.hiddenhistory.models.MotTest
import kotlin.math.abs

internal fun compareAdvertAndMot(
    advert: ParsedVehicleAdvert,
    motTests: List<MotTest>
): AdvertOfficialCrossCheckEngine.CrossCheckResult {

    if (motTests.isEmpty()) {
        return AdvertOfficialCrossCheckEngine.CrossCheckResult(
            warnings = emptyList(),
            confirmations = emptyList(),
            verificationItems = listOf(
                "No MOT history was available for comparison. " +
                    "This is not evidence that the vehicle has no MOT issues; " +
                    "the available evidence is simply incomplete."
            )
        )
    }

    val warnings = mutableListOf<String>()
    val confirmations = mutableListOf<String>()
    val verificationItems = mutableListOf<String>()

    /*
     * ---------------------------------------------------------
     * OFFICIAL MOT MILEAGE READINGS
     * ---------------------------------------------------------
     *
     * Build these before deciding whether the advert contains
     * mileage.
     *
     * This is important because the absence of advert mileage
     * does NOT mean there is nothing useful to report.
     *
     * If the advert does not provide mileage, the latest usable
     * official MOT reading is reported as factual evidence only.
     *
     * It is NOT a warning.
     * It is NOT a verification requirement.
     * It is NOT treated as a discrepancy.
     * ---------------------------------------------------------
     */

    val officialReadings =
        motTests.mapNotNull(::buildOfficialMileageReading)

    /*
     * ---------------------------------------------------------
     * CHRONOLOGICAL OFFICIAL HISTORY
     * ---------------------------------------------------------
     *
     * MOT records may arrive:
     *
     * - newest first
     * - oldest first
     * - in arbitrary order
     *
     * Never trust the supplied list order.
     *
     * Only successfully dated readings participate in
     * chronological latest-reading and regression analysis.
     * ---------------------------------------------------------
     */

    val datedOfficialReadings =
        officialReadings
            .mapNotNull { reading ->
                parseDateTime(reading.date)
                    ?.dateTime
                    ?.let { parsedDateTime ->
                        reading to parsedDateTime
                    }
            }

    val chronologicalReadings =
        datedOfficialReadings
            .sortedWith(
                compareBy<Pair<OfficialMileageReading, java.time.LocalDateTime>> {
                    it.second
                }.thenBy {
                    it.first.test.motTestNumber
                }
            )
            .map {
                it.first
            }

    val latestOfficialReading =
        chronologicalReadings.lastOrNull()

    /*
     * ---------------------------------------------------------
     * ADVERTISED MILEAGE
     * ---------------------------------------------------------
     */

    val advertisedMileage =
        extractAdvertMileage(advert.mileage)
            ?: advert.rawExtractedAttributes["mileage_normalized"]
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
                ?.let {
                    AdvertMileage(
                        originalMileage = it,
                        originalUnit = MileageUnit.MILES,
                        miles = it
                    )
                }

    /*
     * ---------------------------------------------------------
     * NO USABLE ADVERT MILEAGE
     * ---------------------------------------------------------
     *
     * IMPORTANT:
     *
     * Missing advert mileage is NOT itself a risk.
     *
     * We do not know the seller's current claimed mileage, so
     * there is nothing to compare against the official record.
     *
     * Instead, if a usable official MOT reading exists, report
     * the latest known official mileage as a factual finding.
     *
     * Example:
     *
     * Advert: no mileage stated
     * Latest official MOT: 135,235 miles
     *
     * Report:
     *
     * "Latest official MOT mileage recorded: 135,235 miles."
     *
     * Nothing else is added about the missing advert mileage.
     * ---------------------------------------------------------
     */

    if (advertisedMileage == null) {

        if (latestOfficialReading != null) {

            confirmations.add(
                "Latest official MOT mileage recorded: " +
                    formatOfficialMileage(latestOfficialReading) +
                    latestOfficialReading.date
                        ?.let { " on $it" }
                        .orEmpty() +
                    "."
            )
        }

        /*
         * There is no advert mileage to compare, so:
         *
         * - no mileage discrepancy warning
         * - no mileage verification advice
         * - no mileage confirmation claiming a match
         *
         * We still continue with independent MOT analysis below.
         */

        val regressions =
            findMileageRegressions(chronologicalReadings)

        regressions.forEach { regression ->

            warnings.add(
                "Official mileage history contains a possible mileage regression: " +
                    "an earlier MOT recorded " +
                    formatOfficialMileage(regression.previous) +
                    ", while a later MOT recorded " +
                    formatOfficialMileage(regression.current) +
                    ". The later reading is approximately " +
                    "${formatMileage(regression.difference)} miles lower."
            )

            verificationItems.add(
                "Investigate the official mileage regression. Ask for service history " +
                    "and any documentation relating to odometer replacement, correction " +
                    "or instrument-cluster work before relying on the mileage history."
            )
        }

        /*
         * Same-day fail -> pass retests remain independent MOT
         * evidence and are still aggregated into one finding.
         */

        val sameDayRetests =
            findSameDayFailToPassRetests(motTests)

        if (sameDayRetests.isNotEmpty()) {

            val retestCount =
                sameDayRetests.size

            warnings.add(
                "MOT retest pattern detected: the official MOT history contains " +
                    "$retestCount same-day fail-to-pass retest sequence" +
                    "${if (retestCount == 1) "" else "s"}."
            )

            verificationItems.add(
                "Review the failure items associated with the same-day MOT retest " +
                    "sequences and establish what was repaired or rectified before purchase. " +
                    "A later pass does not remove the fact that the earlier tests identified defects."
            )
        }

        /*
         * Defect analysis continues even though there is no advert
         * mileage to cross-check.
         */

        analyseMotDefectPatterns(
            motTests = motTests,
            advertText = advertText(advert),
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        return AdvertOfficialCrossCheckEngine.CrossCheckResult(
            warnings = warnings
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct(),
            confirmations = confirmations
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct(),
            verificationItems = verificationItems
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
        )
    }

    /*
     * ---------------------------------------------------------
     * NO USABLE OFFICIAL MILEAGE
     * ---------------------------------------------------------
     *
     * If the advert DOES contain mileage but the official MOT
     * history contains no usable numerical odometer reading,
     * there is no safe comparison to perform.
     *
     * This remains a factual limitation rather than a mileage
     * discrepancy warning.
     * ---------------------------------------------------------
     */

    if (officialReadings.isEmpty()) {

        analyseMotDefectPatterns(
            motTests = motTests,
            advertText = advertText(advert),
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        return AdvertOfficialCrossCheckEngine.CrossCheckResult(
            warnings = warnings
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct(),
            confirmations = confirmations
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct(),
            verificationItems = verificationItems
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
        )
    }

    /*
     * ---------------------------------------------------------
     * UNIT NORMALISATION
     * ---------------------------------------------------------
     */

    val unitConversionUsed =
        officialReadings.any {
            it.originalUnit != advertisedMileage.originalUnit
        }

    /*
     * ---------------------------------------------------------
     * OFFICIAL READINGS ABOVE ADVERTISED MILEAGE
     * ---------------------------------------------------------
     *
     * Use ALL usable official readings.
     *
     * A usable official reading is evidence even when its date
     * cannot be parsed.
     * ---------------------------------------------------------
     */

    val readingsAboveAdvertisedMileage =
        officialReadings.filter {
            it.miles >
                advertisedMileage.miles +
                    MILEAGE_ROUNDING_TOLERANCE
        }

    if (readingsAboveAdvertisedMileage.isNotEmpty()) {

        val strongest =
            readingsAboveAdvertisedMileage.maxByOrNull {
                it.miles
            }

        if (strongest != null) {

            val difference =
                strongest.miles -
                    advertisedMileage.miles

            warnings.add(
                "Mileage discrepancy detected: the advert states " +
                    "${formatAdvertMileage(advertisedMileage)}, but official MOT " +
                    "history records ${formatOfficialMileage(strongest)}" +
                    "${strongest.date?.let { " on $it" } ?: ""}. After normalisation, " +
                    "the official reading is approximately " +
                    "${formatMileage(difference)} miles higher than the advertised mileage."
            )

            verificationItems.add(
                "Ask the seller to explain why the advertised mileage is lower than " +
                    "an official MOT mileage reading. Request service records, MOT " +
                    "certificates and evidence of the current odometer reading."
            )
        }
    }

    /*
     * ---------------------------------------------------------
     * LATEST OFFICIAL READING
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
             * Do not duplicate the warning if the historical
             * official-reading discrepancy already identified it.
             */
            if (readingsAboveAdvertisedMileage.isEmpty()) {

                warnings.add(
                    "The advertised mileage is lower than the latest official MOT " +
                        "mileage. The advert states " +
                        "${formatAdvertMileage(advertisedMileage)}, while the latest " +
                        "official MOT records ${formatOfficialMileage(latestOfficialReading)}. " +
                        "The difference is approximately " +
                        "${formatMileage(latestDifference)} miles."
                )
            }

            verificationItems.add(
                "Verify the current odometer reading directly and ask the seller to " +
                    "explain the difference between the advert mileage and the latest " +
                    "official MOT record."
            )
        }
    }

    /*
     * ---------------------------------------------------------
     * HISTORICAL EXACT / NEAR MATCH
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
                ". This is supporting historical evidence, not independent proof " +
                "that the current odometer is genuine."
        )
    }

    /*
     * ---------------------------------------------------------
     * ADVERT MILEAGE ABOVE LATEST OFFICIAL READING
     * ---------------------------------------------------------
     *
     * This is not automatically a contradiction.
     * Mileage can legitimately increase after the latest MOT.
     * ---------------------------------------------------------
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
                    "${formatMileage(difference)} miles above the latest official " +
                    "MOT reading. This may reflect mileage accumulated since the MOT, " +
                    "but the current odometer and recent mileage documentation should be checked."
            )

        } else {

            confirmations.add(
                "The advertised mileage is approximately " +
                    "${formatMileage(difference)} miles above the latest official MOT " +
                    "reading, which is consistent with mileage accumulated since that MOT."
            )
        }
    }

    /*
     * ---------------------------------------------------------
     * OFFICIAL MILEAGE REGRESSIONS
     * ---------------------------------------------------------
     */

    val regressions =
        findMileageRegressions(chronologicalReadings)

    regressions.forEach { regression ->

        warnings.add(
            "Official mileage history contains a possible mileage regression: " +
                "an earlier MOT recorded " +
                formatOfficialMileage(regression.previous) +
                ", while a later MOT recorded " +
                formatOfficialMileage(regression.current) +
                ". The later reading is approximately " +
                "${formatMileage(regression.difference)} miles lower."
        )

        verificationItems.add(
            "Investigate the official mileage regression. Ask for service history " +
                "and any documentation relating to odometer replacement, correction " +
                "or instrument-cluster work before relying on the stated mileage."
        )
    }

    /*
     * ---------------------------------------------------------
     * UNIT CONVERSION EVIDENCE
     * ---------------------------------------------------------
     */

    if (unitConversionUsed) {

        confirmations.add(
            "Mileage comparison accounted for different units in the advert and " +
                "official MOT history. Kilometre readings were converted to miles " +
                "for comparison where necessary; original units remain visible in the evidence."
        )
    }

    /*
     * ---------------------------------------------------------
     * SAME-DAY MOT FAIL -> PASS RETESTS
     * ---------------------------------------------------------
     */

    val sameDayRetests =
        findSameDayFailToPassRetests(motTests)

    if (sameDayRetests.isNotEmpty()) {

        val retestCount =
            sameDayRetests.size

        warnings.add(
            "MOT retest pattern detected: the official MOT history contains " +
                "$retestCount same-day fail-to-pass retest sequence" +
                "${if (retestCount == 1) "" else "s"}."
        )

        verificationItems.add(
            "Review the failure items associated with the same-day MOT retest " +
                "sequences and establish what was repaired or rectified before purchase. " +
                "A later pass does not remove the fact that the earlier tests identified defects."
        )
    }

    /*
     * ---------------------------------------------------------
     * NO MATERIAL MILEAGE CONTRADICTION
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
            "No material mileage contradiction was found between the advertised " +
                "mileage and the available official MOT mileage history."
        )
    }

    /*
     * ---------------------------------------------------------
     * MOT DEFECT / FAILURE PATTERNS
     * ---------------------------------------------------------
     */

    analyseMotDefectPatterns(
        motTests = motTests,
        advertText = advertText(advert),
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )

    /*
     * ---------------------------------------------------------
     * FINAL DEDUPLICATED RESULT
     * ---------------------------------------------------------
     */

    return AdvertOfficialCrossCheckEngine.CrossCheckResult(
        warnings = warnings
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct(),
        confirmations = confirmations
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct(),
        verificationItems = verificationItems
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
    )
}