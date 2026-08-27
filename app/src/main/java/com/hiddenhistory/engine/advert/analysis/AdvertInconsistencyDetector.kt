package com.hiddenhistory.engine.advert.analysis

import java.util.Locale

class AdvertInconsistencyDetector {

    private val mileagePattern =
        Regex(
            """\b(\d{1,3}(?:,\d{3})*|\d+(?:\.\d+)?)\s*(k|miles?|mls)\b""",
            RegexOption.IGNORE_CASE
        )

    private val pricePattern =
        Regex(
            """£\s?(\d{1,3}(?:,\d{3})*|\d+(?:\.\d+)?)""",
            RegexOption.IGNORE_CASE
        )

    /**
     * IMPORTANT:
     *
     * This intentionally does NOT blindly treat every four-digit year
     * as a vehicle year.
     *
     * Advertisements commonly contain other years such as:
     *
     * - MOT until July 2027
     * - MOT expires 2027
     * - tax due 2027
     * - serviced in 2025
     * - service history from 2024
     *
     * Those are not vehicle-year statements.
     *
     * Vehicle years are therefore identified separately using context.
     */
    private val yearPattern =
        Regex(
            """\b(19[89]\d|20[0-2]\d)\b"""
        )

    private val motMonthsPattern =
        Regex(
            """\b(\d+)\s+months?\s+(?:of\s+)?(?:mot)\b""",
            RegexOption.IGNORE_CASE
        )

    fun identifyInconsistencies(
        cleanText: String,
        lowerText: String
    ): List<String> {

        val inconsistencies =
            mutableListOf<String>()

        // ---------------------------------------------------------
        // 1. GENERAL CONDITION CONTRADICTIONS
        // ---------------------------------------------------------

        if (
            containsPhrase(
                lowerText,
                "no issues"
            ) &&
            containsAnyPhrase(
                lowerText,
                listOf(
                    "fault",
                    "faults",
                    "problem",
                    "problems",
                    "repair",
                    "repairs",
                    "needs work",
                    "requires work",
                    "mechanical issue",
                    "mechanical issues"
                )
            ) &&
            !onlyNegativeFormIsPresent(
                lowerText,
                listOf(
                    "no fault",
                    "no faults",
                    "no problem",
                    "no problems"
                )
            )
        ) {

            inconsistencies.add(
                "The advert contains a general 'no issues' claim alongside wording that indicates a fault, problem, repair or work requirement."
            )
        }

        if (
            containsPhrase(
                lowerText,
                "no faults"
            ) &&
            containsAnyPhrase(
                lowerText,
                listOf(
                    "engine fault",
                    "gearbox fault",
                    "clutch fault",
                    "electrical fault",
                    "mechanical fault",
                    "fault found",
                    "fault present",
                    "fault needs",
                    "fault requires"
                )
            )
        ) {

            inconsistencies.add(
                "The advert claims that there are no faults but also specifically mentions a fault."
            )
        }

        if (
            containsPhrase(
                lowerText,
                "no problems"
            ) &&
            containsAnyPhrase(
                lowerText,
                listOf(
                    "engine problem",
                    "gearbox problem",
                    "clutch problem",
                    "mechanical problem",
                    "problem with",
                    "problem requires",
                    "problem needs"
                )
            )
        ) {

            inconsistencies.add(
                "The advert claims that there are no problems but also specifically mentions a problem."
            )
        }

        if (
            containsPhrase(
                lowerText,
                "no issues"
            ) &&
            containsAnyPhrase(
                lowerText,
                listOf(
                    "oil leak",
                    "coolant leak",
                    "overheating",
                    "limp mode",
                    "clutch slipping",
                    "head gasket",
                    "warning light",
                    "non runner",
                    "non-runner",
                    "spares or repair"
                )
            )
        ) {

            inconsistencies.add(
                "The advert claims that the vehicle has no issues but also describes a specific fault, defect or repair-related condition."
            )
        }

        // ---------------------------------------------------------
        // 2. CONDITION CLAIM VS DAMAGE / REPAIR
        // ---------------------------------------------------------

        if (
            containsAnyPhrase(
                lowerText,
                listOf(
                    "mint condition",
                    "immaculate",
                    "perfect condition",
                    "excellent condition"
                )
            ) &&
            containsAnyPhrase(
                lowerText,
                listOf(
                    "damaged",
                    "damage",
                    "accident damage",
                    "bodywork damage",
                    "needs repair",
                    "requires repair",
                    "needs work",
                    "requires work"
                )
            )
        ) {

            inconsistencies.add(
                "The advert uses strong condition language while also mentioning damage or repair requirements."
            )
        }

        // ---------------------------------------------------------
        // 3. MILEAGE CONTRADICTIONS
        // ---------------------------------------------------------

        val mileageMatches =
            mileagePattern
                .findAll(
                    cleanText
                )
                .mapNotNull { match ->

                    val value =
                        normalizeMileage(
                            match.value
                        )

                    if (
                        value <= 0
                    ) {

                        return@mapNotNull null
                    }

                    /*
                     * A mileage figure may describe:
                     *
                     * 1. the vehicle's current advertised mileage
                     *
                     * OR
                     *
                     * 2. the mileage at which a historical maintenance
                     *    event took place.
                     *
                     * Examples:
                     *
                     *     timing belt done at 72k miles
                     *     cambelt changed at 72,000 miles
                     *     serviced at 80k miles
                     *     clutch replaced at 75k miles
                     *     brakes done at 70k miles
                     *
                     * These are historical event mileages and must NOT
                     * be treated as competing current odometer readings.
                     */

                    val start =
                        maxOf(
                            0,
                            match.range.first - 60
                        )

                    val end =
                        minOf(
                            cleanText.length,
                            match.range.last + 61
                        )

                    val context =
                        cleanText
                            .substring(
                                start,
                                end
                            )
                            .lowercase(
                                Locale.ROOT
                            )

                    val historicalMileagePatterns =
                        listOf(

                            /*
                             * Timing/cambelt replacement.
                             */
                            Regex(
                                """\b(?:timing|cambelt|cam\s*belt|cam-belt)\b.{0,30}\b(?:done|changed|replaced|renewed|fitted|carried out)\b""",
                                RegexOption.IGNORE_CASE
                            ),

                            Regex(
                                """\b(?:done|changed|replaced|renewed|fitted|carried out)\b.{0,30}\b(?:timing|cambelt|cam\s*belt|cam-belt)\b""",
                                RegexOption.IGNORE_CASE
                            ),

                            /*
                             * General servicing/maintenance mileage.
                             */
                            Regex(
                                """\b(?:serviced|service|maintenance|maintained)\b.{0,30}\b(?:at|on)\b""",
                                RegexOption.IGNORE_CASE
                            ),

                            /*
                             * Component replacement/maintenance.
                             */
                            Regex(
                                """\b(?:clutch|brake|brakes|tyre|tyres|tire|tires|battery|dpf|turbo|catalytic converter|cat)\b.{0,30}\b(?:done|changed|replaced|renewed|fitted|at)\b""",
                                RegexOption.IGNORE_CASE
                            )
                        )

                    if (
                        historicalMileagePatterns.any {
                            it.containsMatchIn(
                                context
                            )
                        }
                    ) {

                        return@mapNotNull null
                    }

                    value
                }
                .toList()

        val distinctMileage =
            mileageMatches
                .distinct()

        if (
            distinctMileage.size > 1
        ) {

            inconsistencies.add(
                "More than one different current mileage figure appears in the advert: ${
                    distinctMileage.joinToString(
                        ", "
                    )
                } miles. Historical maintenance mileages are excluded from this comparison."
            )
        }

        // ---------------------------------------------------------
        // 4. PRICE CONTRADICTIONS
        // ---------------------------------------------------------

        /*
         * Only monetary amounts that appear to represent the vehicle
         * price are compared here. Deposits, finance payments and other
         * transactional amounts are deliberately excluded.
         */
        val vehiclePriceMatches =
            extractVehiclePriceMatches(cleanText)

        val distinctPrices =
            vehiclePriceMatches
                .map { it.amount }
                .distinct()

        if (distinctPrices.size > 1) {

            inconsistencies.add(
                "The advert contains multiple vehicle price statements: ${
                    vehiclePriceMatches
                        .map { it.display }
                        .distinct()
                        .joinToString(", ")
                }. The advert should clearly state which amount is the actual purchase price and explain any deposit or cash-price difference."
            )
        }

        // ---------------------------------------------------------
        // 5. YEAR CONTRADICTIONS
        // ---------------------------------------------------------

        val vehicleYearMatches =
            extractVehicleYears(
                cleanText
            )

        val distinctYears =
            vehicleYearMatches
                .distinct()

        if (
            distinctYears.size > 1
        ) {

            inconsistencies.add(
                "More than one different vehicle year appears in the advert: ${
                    distinctYears.joinToString(
                        ", "
                    )
                }."
            )
        }

        // ---------------------------------------------------------
        // 6. MOT DURATION / MONTH CONTRADICTIONS
        // ---------------------------------------------------------

        val motMonthsMatches =
            motMonthsPattern
                .findAll(
                    lowerText
                )
                .mapNotNull {
                    it.groupValues
                        .getOrNull(1)
                        ?.toIntOrNull()
                }
                .toList()

        if (
            motMonthsMatches
                .distinct()
                .size > 1
        ) {

            inconsistencies.add(
                "The advert contains different stated MOT durations: ${
                    motMonthsMatches
                        .distinct()
                        .joinToString(
                            ", "
                        )
                } months."
            )
        }

        // ---------------------------------------------------------
        // 7. OWNER COUNT CONTRADICTIONS
        // ---------------------------------------------------------

        val ownerCounts =
            extractOwnerCounts(
                lowerText
            )

        if (
            ownerCounts
                .distinct()
                .size > 1
        ) {

            inconsistencies.add(
                "The advert appears to contain conflicting previous-owner/keeper counts: ${
                    ownerCounts
                        .distinct()
                        .joinToString(
                            ", "
                        )
                }."
            )
        }

        // ---------------------------------------------------------
        // 8. SERVICE / HISTORY CONTRADICTIONS
        // ---------------------------------------------------------

        val cleanHistoryClaim =
            containsAnyPhrase(
                lowerText,
                listOf(
                    "full service history",
                    "full history",
                    "complete service history",
                    "main dealer history",
                    "full main dealer history",
                    "hpi clear",
                    "clean history"
                )
            )

        val adverseHistory =
            containsAnyPhrase(
                lowerText,
                listOf(
                    "written off",
                    "write off",
                    "write-off",
                    "category s",
                    "category n",
                    "category c",
                    "category d",
                    "insurance loss",
                    "salvage",
                    "previously written off"
                )
            )

        if (
            cleanHistoryClaim &&
            adverseHistory
        ) {

            inconsistencies.add(
                "The advert claims a clean/HPI-clear history while also containing wording associated with previous insurance loss or write-off history."
            )
        }

        return inconsistencies
            .asSequence()
            .map {
                it.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .distinctBy {
                it.lowercase(
                    Locale.ROOT
                )
            }
            .toList()
    }

    // ---------------------------------------------------------
    // OWNER COUNT EXTRACTION
    // ---------------------------------------------------------

    private data class VehiclePriceMatch(
        val amount: Double,
        val display: String
    )

    private fun extractVehiclePriceMatches(
        text: String
    ): List<VehiclePriceMatch> {

        return pricePattern
            .findAll(text)
            .mapNotNull { match ->
                val start = match.range.first
                val end = match.range.last
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

                if (Regex(
                        "\\b(?:deposit|holding deposit|refundable deposit|monthly|per month|per week|weekly|finance payment|finance contribution)\\b",
                        RegexOption.IGNORE_CASE
                    ).containsMatchIn(before) ||
                    Regex(
                        "\\b(?:deposit|holding deposit|refundable deposit|monthly|per month|per week|weekly|finance payment|finance contribution)\\b",
                        RegexOption.IGNORE_CASE
                    ).containsMatchIn(after)
                ) {
                    null
                } else {
                    val amount = normalizePrice(match.value)
                    amount?.let {
                        VehiclePriceMatch(
                            amount = it,
                            display = formatPrice(it)
                        )
                    }
                }
            }
            .toList()
    }

    private fun extractOwnerCounts(
        lowerText: String
    ): List<Int> {

        val counts =
            mutableListOf<Int>()

        val numericPattern =
            Regex(
                """\b(\d+)\s+(?:previous\s+)?(?:owners?|keepers?)\b""",
                RegexOption.IGNORE_CASE
            )

        numericPattern
            .findAll(
                lowerText
            )
            .forEach { match ->

                match.groupValues
                    .getOrNull(1)
                    ?.toIntOrNull()
                    ?.let {
                        counts.add(
                            it
                        )
                    }
            }

        val wordCounts =
            mapOf(
                "one owner" to 1,
                "two owners" to 2,
                "three owners" to 3,
                "four owners" to 4,
                "five owners" to 5,
                "one keeper" to 1,
                "two keepers" to 2,
                "three keepers" to 3,
                "four keepers" to 4,
                "five keepers" to 5
            )

        wordCounts
            .forEach { (phrase, count) ->

                if (
                    containsPhrase(
                        lowerText,
                        phrase
                    )
                ) {

                    counts.add(
                        count
                    )
                }
            }

        return counts
    }

    // ---------------------------------------------------------
    // MILEAGE NORMALISATION
    // ---------------------------------------------------------

    private fun normalizeMileage(
        value: String
    ): Int {

        val cleaned =
            value
                .lowercase(
                    Locale.ROOT
                )
                .replace(
                    ",",
                    ""
                )
                .trim()

        val match =
            Regex(
                """([0-9]+(?:\.[0-9]+)?)\s*(k|miles?|mls)?"""
            ).find(
                cleaned
            )
                ?: return 0

        val number =
            match.groupValues
                .getOrNull(1)
                ?.toDoubleOrNull()
                ?: return 0

        val unit =
            match.groupValues
                .getOrNull(2)
                ?.lowercase(
                    Locale.ROOT
                )

        return when (
            unit
        ) {

            "k" ->
                (
                    number * 1000
                ).toInt()

            else ->
                number.toInt()
        }
    }

    // ---------------------------------------------------------
    // PRICE NORMALISATION
    // ---------------------------------------------------------

    private fun normalizePrice(
        value: String
    ): Double? {

        return value
            .replace(
                "£",
                ""
            )
            .replace(
                ",",
                ""
            )
            .trim()
            .toDoubleOrNull()
    }

    private fun formatPrice(
        value: Double
    ): String {

        return if (
            value % 1.0 == 0.0
        ) {

            "£${value.toInt()}"

        } else {

            "£$value"
        }
    }

    // ---------------------------------------------------------
    // YEAR EXTRACTION
    // ---------------------------------------------------------

    private fun extractVehicleYears(
        cleanText: String
    ): List<Int> {

        return yearPattern
            .findAll(
                cleanText
            )
            .filter { match ->

                isVehicleYearContext(
                    cleanText,
                    match
                )
            }
            .mapNotNull {
                it.value.toIntOrNull()
            }
            .toList()
    }

    private fun isVehicleYearContext(
        text: String,
        match: MatchResult
    ): Boolean {

        val start =
            maxOf(
                0,
                match.range.first - 60
            )

        val end =
            minOf(
                text.length,
                match.range.last + 61
            )

        val context =
            text
                .substring(
                    start,
                    end
                )
                .lowercase(
                    Locale.ROOT
                )

        // ---------------------------------------------------------
        // EXPLICIT NON-VEHICLE-DATE CONTEXT
        // ---------------------------------------------------------

        val nonVehicleDatePatterns =
            listOf(

                // MOT dates
                Regex(
                    """\bmot\b.{0,35}\b${Regex.escape(match.value)}\b""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """\b${Regex.escape(match.value)}\b.{0,35}\bmot\b""",
                    RegexOption.IGNORE_CASE
                ),

                // Tax dates
                Regex(
                    """\btax\b.{0,35}\b${Regex.escape(match.value)}\b""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """\b${Regex.escape(match.value)}\b.{0,35}\btax\b""",
                    RegexOption.IGNORE_CASE
                ),

                // Service history dates
                Regex(
                    """\bservice\b.{0,35}\b${Regex.escape(match.value)}\b""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """\b${Regex.escape(match.value)}\b.{0,35}\bservice\b""",
                    RegexOption.IGNORE_CASE
                ),

                // Servicing dates
                Regex(
                    """\bserviced\b.{0,35}\b${Regex.escape(match.value)}\b""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """\b${Regex.escape(match.value)}\b.{0,35}\bserviced\b""",
                    RegexOption.IGNORE_CASE
                ),

                // History dates
                Regex(
                    """\bhistory\b.{0,35}\b${Regex.escape(match.value)}\b""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """\b${Regex.escape(match.value)}\b.{0,35}\bhistory\b""",
                    RegexOption.IGNORE_CASE
                ),

                // Registered / registration dates
                Regex(
                    """\bregistered\b.{0,35}\b${Regex.escape(match.value)}\b""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """\bregistration\b.{0,35}\b${Regex.escape(match.value)}\b""",
                    RegexOption.IGNORE_CASE
                ),

                // Previous test dates
                Regex(
                    """\btest\b.{0,35}\b${Regex.escape(match.value)}\b""",
                    RegexOption.IGNORE_CASE
                ),

                // Generic date wording
                Regex(
                    """\b(?:in|from|during|on|until|expires?|expired|due)\s+${Regex.escape(match.value)}\b""",
                    RegexOption.IGNORE_CASE
                ),

                // Months immediately associated with the year
                Regex(
                    """\b(?:january|february|march|april|may|june|july|august|september|october|november|december)\s+${Regex.escape(match.value)}\b""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """\b${Regex.escape(match.value)}\s+(?:january|february|march|april|may|june|july|august|september|october|november|december)\b""",
                    RegexOption.IGNORE_CASE
                )
            )

        if (
            nonVehicleDatePatterns.any {
                it.containsMatchIn(
                    context
                )
            }
        ) {

            return false
        }

        // ---------------------------------------------------------
        // EXPLICIT VEHICLE-YEAR CONTEXT
        // ---------------------------------------------------------

        val explicitVehicleYearPatterns =
            listOf(

                Regex(
                    """\byear\s*[:\-]?\s*${Regex.escape(match.value)}\b""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """\b${Regex.escape(match.value)}\s+model\b""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """\b${Regex.escape(match.value)}\s+plate\b""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """\b${Regex.escape(match.value)}\s+(?:reg|registration)\b""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """\b(?:registered|registration)\s+(?:in|from)\s+${Regex.escape(match.value)}\b""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """\b(?:manufactured|manufacture|built)\s+(?:in|from)\s+${Regex.escape(match.value)}\b""",
                    RegexOption.IGNORE_CASE
                ),

                Regex(
                    """\b${Regex.escape(match.value)}\s+(?:car|vehicle)\b""",
                    RegexOption.IGNORE_CASE
                )
            )

        if (
            explicitVehicleYearPatterns.any {
                it.containsMatchIn(
                    context
                )
            }
        ) {

            return true
        }

        // ---------------------------------------------------------
        // VEHICLE-YEAR DEFAULT
        // ---------------------------------------------------------

        /**
         * If the year is not explicitly associated with MOT, tax,
         * service/history or another date-related phrase, we allow it
         * to count as a vehicle year.
         *
         * This preserves normal adverts such as:
         *
         * "2008 Volkswagen Golf"
         * "2016 BMW 320d"
         * "2020 Ford Fiesta"
         *
         * while preventing:
         *
         * "MOT until July 2027"
         * "Tax due 2027"
         * "Serviced in 2025"
         */
        return true
    }

    // ---------------------------------------------------------
    // PHRASE HELPERS
    // ---------------------------------------------------------

    private fun containsAnyPhrase(
        text: String,
        phrases: List<String>
    ): Boolean {

        return phrases.any {
            containsPhrase(
                text,
                it
            )
        }
    }

    private fun onlyNegativeFormIsPresent(
        text: String,
        negativePhrases: List<String>
    ): Boolean {

        return negativePhrases.any {
            containsPhrase(
                text,
                it
            )
        }
    }

    private fun containsPhrase(
        text: String,
        phrase: String
    ): Boolean {

        val escaped =
            Regex.escape(
                phrase
            )

        return Regex(
            """(?<![a-z0-9])$escaped(?![a-z0-9])""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(
            text
        )
    }
}