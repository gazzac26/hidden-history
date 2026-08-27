package com.hiddenhistory.engine.advert.extraction

import java.util.Locale

class AdvertBasicExtractor {

    /*
     * Four-digit vehicle years.
     *
     * Supported range:
     * 1980-2029
     *
     * The upper range intentionally extends slightly beyond the
     * currently expected vehicle data so the extractor does not
     * become stale immediately.
     */
    private val yearPattern =
        Regex("""\b(19[89]\d|20[0-2]\d)\b""")

    /*
     * Mileage examples supported:
     *
     * 67,500 miles
     * 67500 miles
     * 67.5k
     * 67.5 k miles
     * 67500 mi
     * 67500 mls
     */
    private val mileagePattern =
        Regex(
            """\b(\d{1,3}(?:,\d{3})*|\d+(?:\.\d+)?)\s*(k|miles?|mi|mls)\b""",
            RegexOption.IGNORE_CASE
        )

    /*
     * Explicit UK pound prices.
     *
     * Examples:
     * £8,495
     * £8495
     * £8,495.00
     * £8495.50
     *
     * IMPORTANT:
     *
     * The numeric portion is deliberately captured as one complete
     * sequence. The previous pattern could match the first three
     * digits of a four-digit uncomma'd price such as £2850.
     */
    private val poundPricePattern =
        Regex(
            """£\s*(\d[\d,]*(?:\.\d{1,2})?)(?!\d)""",
            RegexOption.IGNORE_CASE
        )

    /*
     * Prices explicitly labelled as price/asking price.
     *
     * This allows adverts containing:
     * Price: £8,495
     * Asking price £8,495
     * Asking Price: 8495
     *
     * IMPORTANT:
     *
     * The numeric portion must consume the complete number.
     */
    private val labelledPricePattern =
        Regex(
            """(?:asking\s+price|price)\s*[:\-]?\s*(£\s*)?(\d[\d,]*(?:\.\d{1,2})?)(?!\d)""",
            RegexOption.IGNORE_CASE
        )

    /*
     * Horsepower examples:
     *
     * 150 bhp
     * 150BHP
     * 150 PS
     * 150 hp
     */
    private val bhpPattern =
        Regex(
            """\b(\d{2,3})\s*(?:bhp|ps|hp)\b""",
            RegexOption.IGNORE_CASE
        )

    /*
     * Removes years that belong to MOT expiry statements.
     *
     * Examples:
     * MOT until March 2027
     * MOT until 2027
     * MOT to March 2027
     * MOT expires March 2027
     *
     * We don't want the MOT expiry year being mistaken for
     * the vehicle's registration/manufacture year.
     */
    private val motYearPattern =
        Regex(
            """\bmot\b\s*(?:until|to|expires?|expiry)?\s*(?:[a-z]+\s+)?(19[89]\d|20[0-2]\d)\b""",
            RegexOption.IGNORE_CASE
        )

    fun extractYear(
        text: String,
        registrationExtractor: AdvertRegistrationExtractor
    ): Int? {

        if (text.isBlank()) {
            return null
        }

        /*
         * Remove MOT-related years before searching for the vehicle year.
         */
        val cleanedText =
            text.replace(motYearPattern, "")

        /*
         * Prefer an explicit vehicle year appearing in the advert.
         */
        val directYear =
            yearPattern
                .findAll(cleanedText)
                .mapNotNull {
                    it.value.toIntOrNull()
                }
                .firstOrNull()

        if (directYear != null) {
            return directYear
        }

        /*
         * If the advert does not explicitly state a year, allow the
         * existing registration extractor to determine it from the
         * registration format.
         */
        return registrationExtractor.extractYearFromRegistration(text)
    }

    fun extractMileage(text: String): String? {

        if (text.isBlank()) {
            return null
        }

        val match =
            mileagePattern.find(text)
                ?: return null

        val number =
            match.groupValues[1]

        val unit =
            match.groupValues[2]

        return "$number ${unit.uppercase(Locale.ROOT)}"
    }

    fun extractPrice(text: String): String? {

        if (text.isBlank()) {
            return null
        }

        /*
         * Price extraction is context-sensitive. An advert can contain
         * several pound amounts for different purposes, for example:
         *
         *   £2,495 asking price
         *   £2,850 cash price
         *   £500 deposit
         *
         * A deposit, finance payment or other transactional amount must
         * never become the vehicle's advertised price.
         */
        val candidates =
            poundPricePattern
                .findAll(text)
                .map { match ->
                    PriceCandidate(
                        value = match.value,
                        amount = normalizePrice(match.value) ?: 0.0,
                        start = match.range.first,
                        end = match.range.last
                    )
                }
                .filter { !isNonVehiclePrice(text, it.start, it.end) }
                .toList()

        if (candidates.isEmpty()) {
            return null
        }

        val best =
            candidates
                .map { candidate ->
                    candidate to priceContextScore(
                        text = text,
                        candidate = candidate
                    )
                }
                .sortedWith(
                    compareByDescending<Pair<PriceCandidate, Int>> { it.second }
                        .thenBy { it.first.start }
                )
                .first()
                .first

        return "£${formatPriceNumber(best.amount)}"
    }

    private data class PriceCandidate(
        val value: String,
        val amount: Double,
        val start: Int,
        val end: Int
    )

    private fun isNonVehiclePrice(
        text: String,
        start: Int,
        end: Int
    ): Boolean {

        val before =
            text.substring(
                (start - 30).coerceAtLeast(0),
                start
            ).lowercase(Locale.ROOT)

        val after =
            text.substring(
                end + 1,
                (end + 31).coerceAtMost(text.length)
            ).lowercase(Locale.ROOT)

        val immediateNonVehiclePattern =
            Regex(
                "\\b(?:deposit|holding deposit|refundable deposit|monthly|per month|per week|weekly|finance payment|finance contribution)\\b",
                RegexOption.IGNORE_CASE
            )

        return immediateNonVehiclePattern.containsMatchIn(before) ||
                immediateNonVehiclePattern.containsMatchIn(after)
    }

    private fun priceContextScore(
        text: String,
        candidate: PriceCandidate
    ): Int {

        val contextStart =
            (candidate.start - 45).coerceAtLeast(0)

        val contextEnd =
            (candidate.end + 55).coerceAtMost(text.length)

        val context =
            text.substring(contextStart, contextEnd)
                .lowercase(Locale.ROOT)

        var score = 0

        if (Regex("\\basking\\s+price\\b").containsMatchIn(context)) score += 12
        if (Regex("\\bprice\\b").containsMatchIn(context)) score += 8
        if (Regex("\\bcash\\s+price\\b").containsMatchIn(context)) score += 7
        if (Regex("\\bono\\b").containsMatchIn(context)) score += 7
        if (Regex("\\bfor\\s+sale\\b").containsMatchIn(context)) score += 4
        if (Regex("\\bdeposit\\b").containsMatchIn(context)) score -= 20
        if (Regex("\\bfinance\\b").containsMatchIn(context)) score -= 15

        return score
    }

    private fun formatPriceNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            value.toString()
        }
    }

    fun extractBhp(text: String): String? {

        if (text.isBlank()) {
            return null
        }

        return bhpPattern
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
    }

    /**
     * Converts advertised mileage into a numeric value.
     *
     * Examples:
     *
     * "67,500 MILES" -> 67500
     * "67500 miles"  -> 67500
     * "67.5K"        -> 67500
     * "67.5 k miles" -> 67500
     */
    fun normalizeMileage(value: String): Int {

        if (value.isBlank()) {
            return 0
        }

        val cleaned =
            value
                .lowercase(Locale.ROOT)
                .replace(",", "")
                .trim()

        val match =
            Regex(
                """([0-9]+(?:\.[0-9]+)?)\s*(k|miles?|mi|mls)?"""
            ).find(cleaned)
                ?: return 0

        val number =
            match.groupValues[1]
                .toDoubleOrNull()
                ?: return 0

        val unit =
            match.groupValues
                .getOrNull(2)
                ?.lowercase(Locale.ROOT)

        return when (unit) {

            "k" ->
                (number * 1000.0)
                    .toInt()

            else ->
                number.toInt()
        }
    }

    /**
     * Converts an advertised price into a numeric value.
     *
     * Examples:
     *
     * "£8,495"    -> 8495.0
     * "£8495"     -> 8495.0
     * "8495.50"   -> 8495.5
     */
    fun normalizePrice(value: String): Double? {

        if (value.isBlank()) {
            return null
        }

        return value
            .replace("£", "")
            .replace(",", "")
            .trim()
            .toDoubleOrNull()
    }
}