package com.hiddenhistory.engine.advert.crosscheck

import com.hiddenhistory.engine.ParsedVehicleAdvert
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.roundToInt

internal fun advertValue(
    advert: ParsedVehicleAdvert,
    vararg names: String
): Any? {

    val clazz = advert::class.java

    names.forEach { name ->

        val getterName =
            "get" + name.replaceFirstChar { it.uppercaseChar() }

        try {
            val getter = clazz.methods.firstOrNull {
                it.name == getterName &&
                        it.parameterTypes.isEmpty()
            }

            if (getter != null) {
                val value = getter.invoke(advert)

                if (
                    value != null &&
                    value.toString().isNotBlank()
                ) {
                    return value
                }
            }

        } catch (_: Exception) {
            // Unavailable field is not negative evidence.
        }

        try {
            val field =
                clazz.declaredFields.firstOrNull {
                    it.name == name
                }

            if (field != null) {
                field.isAccessible = true

                val value = field.get(advert)

                if (
                    value != null &&
                    value.toString().isNotBlank()
                ) {
                    return value
                }
            }

        } catch (_: Exception) {
            // Unavailable field is not negative evidence.
        }
    }

    return null
}

internal fun advertText(
    advert: ParsedVehicleAdvert
): String {

    val candidates = listOf(

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
        .mapNotNull(::cleanText)
        .joinToString(" ")
}

internal fun cleanText(
    value: Any?
): String? {

    if (value == null) return null

    val text = value
        .toString()
        .trim()

    return text.takeIf {
        it.isNotBlank() &&
                !it.equals("null", ignoreCase = true)
    }
}

internal fun firstNonBlank(
    vararg values: String?
): String? =
    values
        .mapNotNull(::cleanText)
        .firstOrNull()

internal fun normaliseComparableText(
    value: String
): String =
    value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]"), "")

internal fun normaliseFuelType(
    value: String?
): String? {

    val text =
        cleanText(value)
            ?.lowercase(Locale.ROOT)
            ?: return null

    return when {

        /*
         * Keep the more specific combined phrase before
         * the individual fuel types.
         */
        containsPhrase(text, "electric diesel") ->
            "electric diesel"

        containsPhrase(text, "diesel") ->
            "diesel"

        containsPhrase(text, "petrol") ||
                containsPhrase(text, "gasoline") ->
            "petrol"

        containsPhrase(text, "electric") ||
                containsWord(text, "ev") ->
            "electric"

        containsPhrase(text, "hybrid") ->
            "hybrid"

        containsPhrase(text, "lpg") ->
            "lpg"

        containsPhrase(text, "cng") ->
            "cng"

        containsPhrase(text, "lng") ->
            "lng"

        else ->
            normaliseComparableText(text)
    }
}

internal fun normaliseStatus(
    value: String?
): String? =
    cleanText(value)
        ?.uppercase(Locale.ROOT)
        ?.replace(Regex("\\s+"), " ")

internal fun extractInteger(
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
        ?.replace(Regex("[^0-9]"), "")
        ?.toIntOrNull()
}

internal fun extractDouble(
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
        .replace(Regex("[^0-9.]"), "")
        .toDoubleOrNull()
}

internal fun extractYear(
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

internal fun parseEngineSizeToCc(
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
        ?.takeIf { it in 500..10000 }
}

internal fun parseCcFromText(
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
        return (litre * 1000.0).roundToInt()
    }

    return extractInteger(value)
        ?.takeIf { it in 500..10000 }
}

internal fun parseDateTime(
    value: String?
): ParsedDateTime? {

    val raw =
        cleanText(value)
            ?: return null

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

internal fun parseAdvertDate(
    value: String?
): LocalDate? {

    val text =
        cleanText(value)
            ?: return null

    parseDateTime(text)
        ?.date
        ?.let { return it }

    val monthYear =
        Regex(
            """(?i)\b(January|February|March|April|May|June|July|August|September|October|November|December)\s+(19\d{2}|20\d{2})\b"""
        )
            .find(text)

    if (monthYear != null) {

        val month =
            monthYear.groupValues[1]

        val year =
            monthYear.groupValues[2]

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

internal fun isFailedTest(
    result: String?
): Boolean {

    val value =
        cleanText(result)
            ?.uppercase(Locale.ROOT)
            ?: return false

    return value == "FAILED" ||
            value == "FAIL"
}

internal fun isPassedTest(
    result: String?
): Boolean {

    val value =
        cleanText(result)
            ?.uppercase(Locale.ROOT)
            ?: return false

    return value == "PASSED" ||
            value == "PASS"
}

internal fun containsAny(
    text: String,
    vararg phrases: String
): Boolean {

    return phrases.any { phrase ->
        containsPhrase(text, phrase)
    }
}

internal fun hasPositiveMarker(
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

internal fun isMechanicallyRelevant(
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

internal fun formatMileage(
    mileage: Int
): String =
    "%,d".format(
        Locale.UK,
        mileage
    )

/**
 * Phrase matching with word boundaries.
 *
 * This prevents short tokens such as "ev" from matching
 * inside unrelated words.
 */
internal fun containsPhrase(
    text: String,
    phrase: String
): Boolean {

    val escaped =
        Regex.escape(
            phrase.trim()
        )

    return Regex(
        """(?<![a-z0-9])$escaped(?![a-z0-9])""",
        RegexOption.IGNORE_CASE
    )
        .containsMatchIn(text)
}

private fun containsWord(
    text: String,
    word: String
): Boolean =
    containsPhrase(
        text,
        word
    )

internal val DATE_TIME_FORMATTERS = listOf(

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
        "yyyy-MM-dd HH:mm:ss.SSS"
    ),

    DateTimeFormatter.ofPattern(
        "yyyy-MM-dd'T'HH:mm:ss.SSS"
    ),

    DateTimeFormatter.ofPattern(
        "dd/MM/yyyy HH:mm:ss"
    ),

    DateTimeFormatter.ofPattern(
        "dd/MM/yyyy HH:mm"
    )
)

internal val DATE_FORMATTERS = listOf(

    DateTimeFormatter.ISO_LOCAL_DATE,

    DateTimeFormatter.ofPattern(
        "dd/MM/yyyy"
    ),

    DateTimeFormatter.ofPattern(
        "dd-MM-yyyy"
    )
)

internal const val MILEAGE_ROUNDING_TOLERANCE = 10

internal const val SIGNIFICANT_CURRENT_MILEAGE_GAP = 5_000

internal const val SIGNIFICANT_MILEAGE_REGRESSION = 1_000

internal const val KILOMETRES_PER_MILE = 1.609344

internal const val ENGINE_CC_ABSOLUTE_TOLERANCE = 100

internal const val ENGINE_CC_PERCENT_TOLERANCE = 0.05

internal const val MIN_REPEATED_DEFECT_OCCURRENCES = 2

internal const val MIN_IMPACT_PATTERN_SIGNALS = 3