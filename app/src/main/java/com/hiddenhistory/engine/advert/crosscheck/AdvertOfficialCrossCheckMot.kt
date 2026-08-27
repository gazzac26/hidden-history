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
     */

    if (advertisedMileage == null) {

        verificationItems.add(
            "The advert does not contain a usable numerical mileage claim, " +
                "so mileage could not be cross-checked against official MOT readings."
        )

        analyseMotDefectPatterns(
            motTests = motTests,
            advertText = advertText(advert),
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        return AdvertOfficialCrossCheckEngine.CrossCheckResult(
            warnings = warnings.distinct(),
            confirmations = confirmations.distinct(),
            verificationItems = verificationItems.distinct()
        )
    }

    /*
     * ---------------------------------------------------------
     * OFFICIAL MOT MILEAGE READINGS
     * ---------------------------------------------------------
     */

    val officialReadings =
        motTests.mapNotNull(::buildOfficialMileageReading)

    if (officialReadings.isEmpty()) {

        verificationItems.add(
            "The available MOT history does not contain a usable numerical " +
                "odometer reading that can be safely compared with the advert."
        )

        analyseMotDefectPatterns(
            motTests = motTests,
            advertText = advertText(advert),
            warnings = warnings,
            confirmations = confirmations,
            verificationItems = verificationItems
        )

        return AdvertOfficialCrossCheckEngine.CrossCheckResult(
            warnings = warnings.distinct(),
            confirmations = confirmations.distinct(),
            verificationItems = verificationItems.distinct()
        )
    }

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
     * Only successfully dated readings participate in chronological
     * regression/latest-reading analysis.
     *
     * IMPORTANT:
     *
     * Advert-versus-official mileage comparison is deliberately
     * performed against ALL usable official readings below.
     *
     * This means a usable official mileage reading is still capable
     * of exposing a discrepancy even when its date cannot be parsed.
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
     * DO NOT restrict this check to chronologicalReadings.
     *
     * A usable official odometer reading is evidence regardless
     * of whether its date was successfully parsed.
     *
     * This is specifically the advert-vs-official discrepancy check.
     *
     * Example:
     *
     * Advert: 67,500 miles
     * Official: 72,000 miles
     *
     * This must produce a discrepancy.
     * ---------------------------------------------------------
     */

    val readingsAboveAdvertisedMileage =
        officialReadings.filter {
            it.miles >
                advertisedMileage.miles +
                    MILEAGE_ROUNDING_TOLERANCE
        }

    if (readingsAboveAdvertisedMileage.isNotEmpty()) {

        /*
         * Prefer the highest official reading because that gives the
         * strongest evidence that the advertised mileage is below an
         * already-recorded official mileage.
         */
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
     *
     * The latest reading is determined ONLY from successfully
     * parsed dates.
     *
     * The order supplied by the API/database is irrelevant.
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
     *
     * Use all official readings. A date is not required for a
     * mileage reading to provide supporting historical evidence.
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
     *
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
     *
     * ONLY chronological readings are allowed here.
     *
     * This prevents the API/database delivery order from being
     * interpreted as vehicle chronology.
     *
     * findMileageRegressions() also protects itself by sorting
     * chronologically.
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
     *
     * IMPORTANT:
     *
     * The engine still analyses every same-day fail -> pass
     * sequence internally.
     *
     * We deliberately do NOT expose each individual retest date
     * as a separate warning.
     *
     * The user already has the complete MOT history available,
     * including the individual test dates and results.
     *
     * Therefore:
     *
     *     1 retest  -> 1 aggregated warning
     *     5 retests -> 1 aggregated warning
     *     20 retests -> 1 aggregated warning
     *
     * No analytical evidence is removed. Only the presentation
     * is aggregated so the analysis does not flood the user with
     * information they can already see in the MOT history.
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
     *
     * Do not claim that no contradiction exists when an undated
     * official reading is above the advertised mileage.
     *
     * readingsAboveAdvertisedMileage deliberately contains ALL
     * usable official readings, not only dated readings.
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
        warnings = warnings.distinct(),
        confirmations = confirmations.distinct(),
        verificationItems = verificationItems.distinct()
    )
}