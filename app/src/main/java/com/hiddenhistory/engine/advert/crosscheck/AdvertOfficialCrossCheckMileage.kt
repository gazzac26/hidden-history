package com.hiddenhistory.engine.advert.crosscheck

import com.hiddenhistory.models.MotTest
import kotlin.math.roundToInt

internal fun buildOfficialMileageReading(
    test: MotTest
): OfficialMileageReading? {

    val resultType =
        test.odometerResultType
            ?.trim()
            ?.uppercase()
            ?.takeIf { it.isNotBlank() }

    /*
     * These MOT records do not provide a usable odometer reading.
     */
    if (
        resultType == "NO_ODOMETER" ||
        resultType == "UNREADABLE"
    ) {
        return null
    }

    val originalMileage =
        extractMileage(test.odometerValue)
            ?: return null

    val originalUnit =
        parseOfficialMileageUnit(test.odometerUnit)

    if (originalUnit == MileageUnit.UNKNOWN) {
        return null
    }

    val miles =
        when (originalUnit) {

            MileageUnit.MILES ->
                originalMileage

            MileageUnit.KILOMETRES ->
                kilometresToMiles(originalMileage)

            MileageUnit.UNKNOWN ->
                return null
        }

    return OfficialMileageReading(
        originalMileage = originalMileage,
        originalUnit = originalUnit,
        miles = miles,
        date = test.completedDate
            ?.trim()
            ?.takeIf { it.isNotBlank() },
        resultType = resultType,
        testResult = test.testResult
            ?.trim()
            ?.uppercase()
            ?.takeIf { it.isNotBlank() },
        test = test
    )
}

/**
 * Extracts a mileage value from an advert-specific mileage field.
 *
 * IMPORTANT:
 *
 * This function does NOT invent mileage when the advert does not
 * provide one.
 *
 * The upstream AdvertBasicExtractor is responsible for extracting
 * the advert mileage from the seller's advert text. This function
 * only interprets a value that has already been identified as
 * mileage by that extraction layer.
 *
 * Therefore:
 *
 * null = no usable advert mileage was supplied.
 *
 * A missing advert mileage is NOT itself a risk finding.
 */
internal fun extractAdvertMileage(
    value: Any?
): AdvertMileage? {

    val text =
        cleanText(value)
            ?: return null

    /*
     * At this stage we only accept values which contain an explicit
     * mileage unit or a recognised compact "k" mileage form.
     *
     * Bare arbitrary numbers are deliberately not treated as mileage.
     *
     * Examples accepted:
     *
     * 113,000 miles
     * 113000 miles
     * 113k miles
     * 113k
     * 181,000 km
     * 181000 km
     *
     * The upstream advert extractor is responsible for preventing
     * unrelated figures such as performance figures from reaching
     * this cross-check as mileage.
     */
    val regex =
        Regex(
            """\b(\d{1,3}(?:,\d{3})*|\d+(?:\.\d+)?)\s*(k|miles?|mls|km|kms|kilometres?|kilometers?)\b""",
            RegexOption.IGNORE_CASE
        )

    val matches =
        regex
            .findAll(text)
            .toList()

    if (matches.isEmpty()) {
        return null
    }

    /*
     * Evaluate all candidates rather than blindly accepting the first
     * numerical match.
     *
     * This protects the cross-check layer from historical mileage
     * figures associated with servicing or component replacement.
     */
    val serviceContextPattern =
        Regex(
            """\b(?:timing\s+belt|cam\s+belt|cambelt|head\s+gasket|service(?:d|ing)?|servicing|changed|replaced|replacement|done|fitted|repair|repaired|maintenance|mot)\b""",
            RegexOption.IGNORE_CASE
        )

    val validCandidate =
        matches
            .asSequence()
            .mapNotNull { match ->

                val startIndex =
                    match.range.first

                val endIndex =
                    match.range.last + 1

                /*
                 * Inspect context on both sides of the mileage figure.
                 *
                 * This catches both:
                 *
                 * "timing belt replaced at 72,000 miles"
                 *
                 * and:
                 *
                 * "72,000 miles when the timing belt was replaced"
                 */
                val contextStart =
                    (startIndex - 50)
                        .coerceAtLeast(0)

                val contextEnd =
                    (endIndex + 50)
                        .coerceAtMost(text.length)

                val context =
                    text.substring(
                        contextStart,
                        contextEnd
                    )

                /*
                 * A mileage figure associated with service/repair
                 * history is historical evidence rather than the
                 * advert's current odometer reading.
                 */
                if (
                    serviceContextPattern
                        .containsMatchIn(context)
                ) {
                    return@mapNotNull null
                }

                val rawNumber =
                    match.groupValues
                        .getOrNull(1)
                        ?.replace(",", "")
                        ?.trim()
                        ?: return@mapNotNull null

                val number =
                    rawNumber
                        .toDoubleOrNull()
                        ?: return@mapNotNull null

                if (number <= 0.0) {
                    return@mapNotNull null
                }

                val rawUnit =
                    match.groupValues
                        .getOrNull(2)
                        ?.trim()
                        ?.lowercase()
                        .orEmpty()

                val unit =
                    when {

                        rawUnit == "k" ->
                            MileageUnit.MILES

                        rawUnit == "mile" ||
                                rawUnit == "miles" ||
                                rawUnit == "mls" ->
                            MileageUnit.MILES

                        rawUnit == "km" ||
                                rawUnit == "kms" ||
                                rawUnit == "kilometre" ||
                                rawUnit == "kilometres" ||
                                rawUnit == "kilometer" ||
                                rawUnit == "kilometers" ->
                            MileageUnit.KILOMETRES

                        else ->
                            MileageUnit.UNKNOWN
                    }

                if (unit == MileageUnit.UNKNOWN) {
                    return@mapNotNull null
                }

                val originalMileage =
                    when {

                        rawUnit == "k" ->
                            (number * 1000.0)
                                .roundToInt()

                        else ->
                            number.roundToInt()
                    }

                if (originalMileage <= 0) {
                    return@mapNotNull null
                }

                val miles =
                    when (unit) {

                        MileageUnit.MILES ->
                            originalMileage

                        MileageUnit.KILOMETRES ->
                            kilometresToMiles(originalMileage)

                        MileageUnit.UNKNOWN ->
                            return@mapNotNull null
                    }

                AdvertMileage(
                    originalMileage = originalMileage,
                    originalUnit = unit,
                    miles = miles
                )
            }
            .firstOrNull()

    return validCandidate
}

internal fun extractMileage(
    value: Any?
): Int? {

    val text =
        cleanText(value)
            ?: return null

    /*
     * MOT odometer values can arrive with commas, spaces,
     * decimals or unit text.
     *
     * We deliberately extract the numeric component only here.
     * Unit interpretation is handled separately.
     *
     * This function is for an already identified MOT odometer
     * field, not arbitrary advert text.
     */
    val match =
        Regex(
            """\b\d{1,3}(?:,\d{3})*(?:\.\d+)?\b|\b\d+(?:\.\d+)?\b"""
        )
            .find(text)
            ?: return null

    val numericValue =
        match.value
            .replace(",", "")
            .toDoubleOrNull()
            ?: return null

    if (numericValue <= 0.0) {
        return null
    }

    return numericValue.roundToInt()
}

internal fun parseOfficialMileageUnit(
    value: Any?
): MileageUnit {

    val unit =
        value
            ?.toString()
            ?.trim()
            ?.uppercase()
            ?.takeIf { it.isNotBlank() }
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

internal fun kilometresToMiles(
    kilometres: Int
): Int {

    if (kilometres <= 0) {
        return 0
    }

    return (
        kilometres.toDouble() /
            KILOMETRES_PER_MILE
        ).roundToInt()
}

internal fun findMileageRegressions(
    readings: List<OfficialMileageReading>
): List<MileageRegression> {

    if (readings.size < 2) {
        return emptyList()
    }

    /*
     * MOT history should be evaluated chronologically.
     *
     * Do not rely on the order in which the API/database supplied
     * the records.
     */
    val orderedReadings =
        readings
            .sortedWith(
                compareBy<OfficialMileageReading> {
                    parseDateTime(it.date)?.dateTime
                }.thenBy {
                    it.test.motTestNumber
                }
            )

    val regressions =
        mutableListOf<MileageRegression>()

    for (index in 1 until orderedReadings.size) {

        val previous =
            orderedReadings[index - 1]

        val current =
            orderedReadings[index]

        val difference =
            previous.miles - current.miles

        /*
         * Small reductions can occur because of:
         *
         * - odometer entry variation
         * - conversion between km and miles
         * - recording/rounding differences
         *
         * Only report a regression when it exceeds the
         * project's significant-regression threshold.
         */
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

internal fun findSameDayFailToPassRetests(
    motTests: List<MotTest>
): List<MotTest> {

    if (motTests.size < 2) {
        return emptyList()
    }

    /*
     * A fail followed by a pass on the same day is normally
     * evidence of a retest, not automatically evidence of
     * fraud or mileage manipulation.
     *
     * We therefore expose the event as evidence for the wider
     * analysis layer rather than assigning a risk judgement here.
     */
    val sameDayTests =
        motTests
            .filter {
                !it.completedDate.isNullOrBlank()
            }
            .groupBy {
                parseDateTime(it.completedDate)
                    ?.date
            }

    val retests =
        mutableListOf<MotTest>()

    sameDayTests.forEach { (_, tests) ->

        val ordered =
            tests.sortedWith(
                compareBy<MotTest> {
                    parseDateTime(it.completedDate)?.dateTime
                }.thenBy {
                    it.motTestNumber
                }
            )

        var failureSeen = false

        ordered.forEach { test ->

            when {

                isFailedTest(test.testResult) -> {
                    failureSeen = true
                }

                failureSeen &&
                        isPassedTest(test.testResult) -> {

                    retests.add(test)
                }
            }
        }
    }

    return retests.distinctBy {
        it.motTestNumber
            ?: it.completedDate
    }
}

internal fun formatAdvertMileage(
    mileage: AdvertMileage
): String {

    return when (mileage.originalUnit) {

        MileageUnit.MILES ->
            "${formatMileage(mileage.originalMileage)} miles"

        MileageUnit.KILOMETRES ->
            "${formatMileage(mileage.originalMileage)} km"

        MileageUnit.UNKNOWN ->
            formatMileage(mileage.originalMileage)
    }
}

internal fun formatOfficialMileage(
    reading: OfficialMileageReading
): String {

    return when (reading.originalUnit) {

        MileageUnit.MILES ->
            "${formatMileage(reading.originalMileage)} miles"

        MileageUnit.KILOMETRES ->
            "${formatMileage(reading.originalMileage)} km"

        MileageUnit.UNKNOWN ->
            formatMileage(reading.originalMileage)
    }
}