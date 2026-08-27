package com.hiddenhistory.engine.advert.crosscheck

import com.hiddenhistory.engine.ParsedVehicleAdvert
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

internal fun extractAdvertMileage(
    value: Any?
): AdvertMileage? {

    val text =
        cleanText(value)
            ?: return null

    /*
     * Accept common advert forms:
     *
     * 113,000 miles
     * 113000 miles
     * 113k miles
     * 113k
     * 181,000 km
     * 181000 km
     *
     * Do not accept arbitrary bare numbers here.
     * A bare number is only accepted when the surrounding
     * advert field is already known to represent mileage.
     */

    // Filter out numbers preceded by maintenance/service keywords (e.g., timing belt done at 72k)
    val serviceContextPattern = Regex(
        """\b(?:timing\s+belt|cam\s+belt|cambelt|head\s+gasket|service|serviced|changed|done)\b.{0,25}""",
        RegexOption.IGNORE_CASE
    )

    val regex =
        Regex(
            """\b(\d{1,3}(?:,\d{3})*|\d+(?:\.\d+)?)\s*(k|miles?|mls|km|kms|kilometres?|kilometers?)?\b""",
            RegexOption.IGNORE_CASE
        )

    val match = regex.findAll(text).firstOrNull { foundMatch ->
        val startIndex = foundMatch.range.first
        val precedingText = text.substring(maxOf(0, startIndex - 30), startIndex)
        !serviceContextPattern.containsMatchIn(precedingText)
    } ?: return null

    val rawNumber =
        match.groupValues
            .getOrNull(1)
            ?.replace(",", "")
            ?.trim()
            ?: return null

    val number =
        rawNumber
            .toDoubleOrNull()
            ?: return null

    if (number <= 0.0) {
        return null
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
                MileageUnit.MILES
        }

    val originalMileage =
        when {
            rawUnit == "k" ->
                (number * 1000.0).roundToInt()

            else ->
                number.roundToInt()
        }

    val miles =
        when (unit) {

            MileageUnit.MILES ->
                originalMileage

            MileageUnit.KILOMETRES ->
                kilometresToMiles(originalMileage)

            MileageUnit.UNKNOWN ->
                return null
        }

    return AdvertMileage(
        originalMileage = originalMileage,
        originalUnit = unit,
        miles = miles
    )
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
     */

    val match =
        Regex(
            """\b\d{1,3}(?:,\d{3})*(?:\.\d+)?\b|\b\d+(?:\.\d+)?\b"""
        ).find(text)
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
