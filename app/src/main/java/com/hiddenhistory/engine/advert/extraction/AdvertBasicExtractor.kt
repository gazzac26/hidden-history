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
 *
 * IMPORTANT:
 *
 * This pattern deliberately finds CANDIDATES.
 *
 * It does NOT decide that every number followed by "miles"
 * is the vehicle's odometer mileage.
 *
 * Context is evaluated by extractMileage().
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

/*
 * =============================================================
 * MILEAGE CONTEXT PROTECTION
 * =============================================================
 *
 * A mileage-shaped number is NOT automatically the vehicle's
 * current odometer reading.
 *
 * Adverts commonly contain numbers followed by "miles" which
 * refer to:
 *
 * - 0-60 performance
 * - speed
 * - fuel economy
 * - EV range
 * - distance from a location
 * - service intervals
 * - timing belt replacement mileage
 * - previous-owner mileage
 * - previous MOT mileage
 * - historical mileage
 * - warranty/maintenance milestones
 *
 * These must never become the advertised vehicle mileage.
 */

/*
 * Strong indicators that the number refers to speed or
 * acceleration rather than vehicle odometer mileage.
 *
 * Examples:
 *
 * 0 to 60 miles per hour
 * 0-60 mph
 * 60 miles per hour
 * top speed 60 mph
 * maximum speed 60 mph
 */
private val speedContextPattern =
    Regex(
        """(?i)(?:\b0\s*(?:to|-|–|—)\s*)?\d+(?:\.\d+)?\s*(?:miles?\s+per\s+hour|mph|km/?h|kph)\b"""
    )

/*
 * Explicit acceleration/performance wording around a mileage-shaped
 * number.
 *
 * This is intentionally separate from speedContextPattern because
 * adverts can phrase performance information in many different ways.
 */
private val performanceContextPattern =
    Regex(
        """(?i)\b(?:0\s*(?:to|-|–|—)\s*)?\d+(?:\.\d+)?\s*(?:miles?|mph|km/?h|kph)\b"""
    )

/*
 * Non-odometer uses of "miles".
 *
 * Examples:
 *
 * 60 miles per gallon
 * 60 miles range
 * 60 miles away
 * 60 miles from Manchester
 * 60 mile radius
 */
private val nonOdometerDistancePattern =
    Regex(
        """(?i)\b\d+(?:\.\d+)?\s*(?:miles?|mi)\s+(?:per|range|away|from|radius|round\s+trip|each\s+way)\b"""
    )

/*
 * Service / maintenance context.
 *
 * Examples:
 *
 * timing belt done at 72,000 miles
 * clutch replaced at 80k miles
 * serviced at 60k
 * oil change at 50,000 miles
 * brakes replaced at 70k miles
 *
 * The number is historical maintenance information rather
 * than a claim about the current odometer.
 */
private val serviceContextPattern =
    Regex(
        """(?i)\b(?:timing\s+)?(?:belt|cam\s+belt|cam\s+chain|chain|clutch|brake(?:s)?|pads?|discs?|disc|oil|service|serviced|maintenance|repair|repaired|replaced|changed|fitted|done|work)\b.{0,55}?\b\d+(?:,\d{3})*(?:\.\d+)?\s*(?:k|miles?|mi|mls)\b"""
    )

/*
 * Reverse-direction service wording.
 *
 * Examples:
 *
 * 72,000 miles when timing belt was replaced
 * 80k when clutch was changed
 * 60,000 miles service
 */
private val reverseServiceContextPattern =
    Regex(
        """(?i)\b\d+(?:,\d{3})*(?:\.\d+)?\s*(?:k|miles?|mi|mls)\b.{0,55}?\b(?:timing\s+belt|cam\s+belt|cam\s+chain|chain|clutch|brake(?:s)?|pads?|discs?|disc|oil|service|serviced|maintenance|repair|repaired|replaced|changed|fitted|work)\b"""
    )

/*
 * Historical mileage wording.
 *
 * Examples:
 *
 * previous owner covered 80,000 miles
 * previous owner did 70k miles
 * had 90,000 miles when purchased
 * recorded 85,000 miles
 * previously 75k miles
 * MOT recorded 80,000 miles
 */
private val historicalMileageContextPattern =
    Regex(
        """(?i)\b(?:previous|prior|former|last)\s+(?:owner|keeper|seller|vehicle)?|""" +
            """\b(?:when|at\s+the\s+time)\s+(?:purchased|bought|sold|acquired)\b|""" +
            """\b(?:previously|historically|history|historical|recorded|record|reported)\b|""" +
            """\b(?:mot|m\.o\.t)\b.{0,55}?\b\d+(?:,\d{3})*(?:\.\d+)?\s*(?:k|miles?|mi|mls)\b"""
    )

/*
 * Strong odometer/current-mileage phrases.
 *
 * These receive a positive score when selecting between multiple
 * candidates.
 *
 * Examples:
 *
 * current mileage 92,000
 * mileage 92k
 * currently 92,000 miles
 * only 92k miles
 * just 92,000 miles
 * covered 92,000 miles
 * has done 92,000 miles
 */
private val currentMileageContextPattern =
    Regex(
        """(?i)\b(?:current|currently|present|today|actual)\s+(?:vehicle\s+)?(?:mileage|miles|odometer|reading)\b|""" +
            """\b(?:mileage|odometer|reading)\s*(?:is|:|-)?\s*\d+(?:,\d{3})*(?:\.\d+)?\s*(?:k|miles?|mi|mls)?\b|""" +
            """\b(?:only|just|currently)\s+\d+(?:,\d{3})*(?:\.\d+)?\s*(?:k|miles?|mi|mls)\b|""" +
            """\b(?:has\s+done|done|covered|driven|travelled|traveling)\s+\d+(?:,\d{3})*(?:\.\d+)?\s*(?:k|miles?|mi|mls)\b"""
    )

/*
 * Common phrases that explicitly introduce a service interval.
 *
 * This protects against wording such as:
 *
 * "60k service"
 * "service due at 60,000 miles"
 * "major service at 72k"
 */
private val serviceIntervalContextPattern =
    Regex(
        """(?i)\b(?:service|servicing|maintenance|inspection)\b.{0,35}?\b(?:due|at|around|every)\b.{0,15}?\d+(?:,\d{3})*(?:\.\d+)?\s*(?:k|miles?|mi|mls)\b|""" +
            """\b\d+(?:,\d{3})*(?:\.\d+)?\s*(?:k|miles?|mi|mls)\s+(?:service|servicing|maintenance|inspection)\b"""
    )

/*
 * =============================================================
 * YEAR
 * =============================================================
 */

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

/*
 * =============================================================
 * MILEAGE EXTRACTION
 * =============================================================
 */

fun extractMileage(text: String): String? {

    if (text.isBlank()) {
        return null
    }

    /*
     * IMPORTANT:
     *
     * Do NOT use mileagePattern.find(text).
     *
     * That was the original bug.
     *
     * In:
     *
     * "0 to 60 miles per hour"
     *
     * find() sees "60 miles" and immediately returns it.
     *
     * Kotlin's find() returns the first matching occurrence.
     * We therefore inspect ALL candidates and score their
     * surrounding context before accepting one.
     */
    val candidates =
        mileagePattern
            .findAll(text)
            .mapNotNull { match ->
                buildMileageCandidate(
                    text = text,
                    match = match
                )
            }
            .toList()

    if (candidates.isEmpty()) {
        return null
    }

    /*
     * Remove candidates which are strongly identified as
     * non-odometer information.
     */
    val validCandidates =
        candidates.filterNot {
            it.isDefinitelyNonOdometer
        }

    if (validCandidates.isEmpty()) {
        return null
    }

    /*
     * Select the strongest remaining current-mileage candidate.
     *
     * We deliberately prefer explicit current-mileage wording,
     * while penalising weak/historical contexts.
     *
     * If several candidates are otherwise equal, the earliest
     * one wins. This preserves deterministic behaviour.
     */
    val best =
        validCandidates
            .sortedWith(
                compareByDescending<MileageCandidate> {
                    it.confidenceScore
                }.thenBy {
                    it.start
                }
            )
            .first()

    return best.displayValue
}

/*
 * Builds a contextual mileage candidate.
 */
private fun buildMileageCandidate(
    text: String,
    match: MatchResult
): MileageCandidate? {

    val numberPart =
        match.groupValues
            .getOrNull(1)
            ?: return null

    val unit =
        match.groupValues
            .getOrNull(2)
            ?.lowercase(Locale.ROOT)
            ?: return null

    val numericValue =
        parseMileageNumber(
            numberPart = numberPart,
            unit = unit
        )
            ?: return null

    /*
     * Sanity limits.
     *
     * Passenger/commercial vehicle adverts can legitimately
     * contain high mileages, but values outside this range are
     * overwhelmingly likely to be something else.
     */
    if (numericValue <= 0 || numericValue > 1_000_000) {
        return null
    }

    val context =
        mileageContext(
            text = text,
            start = match.range.first,
            end = match.range.last
        )

    val lowerContext =
        context.lowercase(Locale.ROOT)

    /*
     * ---------------------------------------------------------
     * DEFINITIVE NON-ODOMETER CHECKS
     * ---------------------------------------------------------
     *
     * These should cause an immediate rejection rather than
     * merely reducing the candidate score.
     */

    if (
        isSpeedCandidate(
            text = text,
            match = match
        )
    ) {
        return MileageCandidate(
            displayValue = formatMileage(numberPart, unit),
            numericValue = numericValue,
            start = match.range.first,
            confidenceScore = Int.MIN_VALUE,
            isDefinitelyNonOdometer = true
        )
    }

    if (
        nonOdometerDistancePattern
            .containsMatchIn(lowerContext)
    ) {
        return MileageCandidate(
            displayValue = formatMileage(numberPart, unit),
            numericValue = numericValue,
            start = match.range.first,
            confidenceScore = Int.MIN_VALUE,
            isDefinitelyNonOdometer = true
        )
    }

    /*
     * Service and maintenance references are historical/interval
     * information unless the same candidate is explicitly
     * described as the current mileage.
     */
    val serviceContext =
        serviceContextPattern.containsMatchIn(
            lowerContext
        ) ||
            reverseServiceContextPattern.containsMatchIn(
                lowerContext
            ) ||
            serviceIntervalContextPattern.containsMatchIn(
                lowerContext
            )

    if (
        serviceContext &&
        !currentMileageContextPattern.containsMatchIn(
            lowerContext
        )
    ) {
        return MileageCandidate(
            displayValue = formatMileage(numberPart, unit),
            numericValue = numericValue,
            start = match.range.first,
            confidenceScore = Int.MIN_VALUE,
            isDefinitelyNonOdometer = true
        )
    }

    /*
     * Historical wording should not become the current odometer
     * reading.
     */
    if (
        historicalMileageContextPattern
            .containsMatchIn(lowerContext) &&
        !currentMileageContextPattern.containsMatchIn(
            lowerContext
        )
    ) {
        return MileageCandidate(
            displayValue = formatMileage(numberPart, unit),
            numericValue = numericValue,
            start = match.range.first,
            confidenceScore = Int.MIN_VALUE,
            isDefinitelyNonOdometer = true
        )
    }

    /*
     * ---------------------------------------------------------
     * CONTEXT SCORING
     * ---------------------------------------------------------
     */

    var score = 0

    /*
     * Explicit current-mileage wording is the strongest positive
     * signal.
     */
    if (
        currentMileageContextPattern
            .containsMatchIn(lowerContext)
    ) {
        score += 100
    }

    /*
     * A standalone mileage statement is also strong.
     *
     * Examples:
     *
     * "Mileage: 92,000 miles"
     * "92,000 miles"
     * "Mileage 92k"
     */
    if (
        Regex(
            """(?i)\b(?:mileage|odometer|mileage\s+reading)\b"""
        ).containsMatchIn(lowerContext)
    ) {
        score += 45
    }

    /*
     * "miles" / "mi" / "mls" is more explicit than a bare "k".
     */
    when (unit) {
        "miles",
        "mile",
        "mi",
        "mls" -> score += 15

        "k" -> score += 8
    }

    /*
     * A realistic vehicle mileage is generally much larger than
     * performance figures. This is only a weak signal; we never
     * reject low mileage solely because it is low.
     *
     * This allows legitimate:
     *
     * 5,000 miles
     * 12,000 miles
     * 60 miles
     *
     * while contextual rules handle the dangerous false positives.
     */
    if (numericValue >= 10_000) {
        score += 8
    } else if (numericValue >= 1_000) {
        score += 4
    }

    /*
     * Explicit vehicle-sale wording gives a small positive signal.
     */
    if (
        Regex(
            """(?i)\b(?:for\s+sale|for\s+sale\s+is|advertised|vehicle|car)\b"""
        ).containsMatchIn(lowerContext)
    ) {
        score += 4
    }

    /*
     * Historical/service words which did not trigger the
     * definitive rejection still receive a penalty.
     */
    if (
        Regex(
            """(?i)\b(?:previous|prior|history|historical|service|serviced|maintenance|replaced|changed|fitted|repair|repaired)\b"""
        ).containsMatchIn(lowerContext)
    ) {
        score -= 35
    }

    return MileageCandidate(
        displayValue =
            formatMileage(
                numberPart,
                unit
            ),
        numericValue = numericValue,
        start = match.range.first,
        confidenceScore = score,
        isDefinitelyNonOdometer = false
    )
}

/*
 * Determines whether a mileage-shaped candidate is actually
 * describing speed/performance.
 *
 * This specifically prevents the original:
 *
 * "0 to 60 miles per hour"
 *
 * false positive.
 *
 * It also handles:
 *
 * "0-60 mph"
 * "0 – 60 mph"
 * "60 miles per hour"
 * "top speed 60 mph"
 * "maximum speed of 60 mph"
 * "60 km/h"
 */
private fun isSpeedCandidate(
    text: String,
    match: MatchResult
): Boolean {

    val start =
        match.range.first

    val end =
        match.range.last

    val before =
        text.substring(
            (start - 70).coerceAtLeast(0),
            start
        )

    val after =
        text.substring(
            end + 1,
            (end + 71).coerceAtMost(
                text.length
            )
        )

    val beforeLower =
        before.lowercase(Locale.ROOT)

    val afterLower =
        after.lowercase(Locale.ROOT)

    /*
     * Direct continuation:
     *
     * "60 miles per hour"
     * "60 miles/hour"
     * "60 mph"
     * "60 km/h"
     */
    val speedAfter =
        Regex(
            """(?i)^\s*(?:/|\s+)?(?:per\s+hour|/hour|mph|km/?h|kph)\b"""
        )

    if (
        speedAfter.containsMatchIn(afterLower)
    ) {
        return true
    }

    /*
     * The mileage match itself may be "60 miles", followed by
     * "per hour". Check both normal and punctuation-separated
     * forms.
     */
    if (
        Regex(
            """(?i)\b(?:miles?|mi)\s*(?:/|\bper\b)\s*hour\b"""
        ).containsMatchIn(
            "${match.value} $afterLower"
        )
    ) {
        return true
    }

    /*
     * "0 to 60 miles..."
     * "0-60 miles..."
     * "0 – 60 mph..."
     *
     * We only need a relatively small window immediately before
     * the candidate.
     */
    if (
        Regex(
            """(?i)\b0\s*(?:to|-|–|—)\s*$"""
        ).containsMatchIn(
            beforeLower
        )
    ) {
        return true
    }

    /*
     * Performance wording immediately before the candidate.
     *
     * Examples:
     *
     * acceleration to 60 mph
     * reaches 60 mph
     * top speed 60 mph
     * maximum speed 60 mph
     * capable of 60 mph
     * achieves 60 mph
     */
    val performancePrefix =
        Regex(
            """(?i)\b(?:0\s*(?:to|-|–|—)\s*|""" +
                """acceleration\s+(?:to|of)?\s*|""" +
                """accelerates\s+(?:to|from)?\s*|""" +
                """reaches?\s+|""" +
                """top\s+speed\s+(?:of\s+)?|""" +
                """maximum\s+speed\s+(?:of\s+)?|""" +
                """max(?:imum)?\s+speed\s+(?:of\s+)?|""" +
                """capable\s+of\s+|""" +
                """achieves?\s+|""" +
                """does\s+)\s*$"""
        )

    if (
        performancePrefix.containsMatchIn(
            beforeLower
        )
    ) {
        return true
    }

    /*
     * Search a wider local context for unmistakable performance
     * phrases where the number may not be directly adjacent.
     */
    val widerContext =
        "${beforeLower.takeLast(90)} ${afterLower.take(90)}"

    val performancePhrase =
        Regex(
            """(?i)\b(?:0\s*(?:to|-|–|—)\s*\d+|""" +
                """top\s+speed|""" +
                """maximum\s+speed|""" +
                """acceleration|""" +
                """accelerates?|""" +
                """performance)\b"""
        )

    if (
        performancePhrase.containsMatchIn(
            widerContext
        ) &&
        Regex(
            """(?i)\b(?:miles?\s+per\s+hour|mph|km/?h|kph)\b"""
        ).containsMatchIn(
            widerContext
        )
    ) {
        return true
    }

    /*
     * Generic "X mph" is always speed, even when there is no
     * obvious performance phrase.
     */
    if (
        Regex(
            """(?i)\b\d+(?:\.\d+)?\s*mph\b"""
        ).containsMatchIn(
            "${beforeLower} ${match.value} ${afterLower}"
        )
    ) {
        return true
    }

    /*
     * Generic "X km/h" and "X kph" are speed.
     */
    if (
        Regex(
            """(?i)\b\d+(?:\.\d+)?\s*(?:km/?h|kph)\b"""
        ).containsMatchIn(
            "${beforeLower} ${match.value} ${afterLower}"
        )
    ) {
        return true
    }

    return false
}

/*
 * Returns a local text window around a mileage candidate.
 *
 * Keeping this bounded prevents an unrelated sentence several
 * paragraphs away from contaminating the candidate's context.
 */
private fun mileageContext(
    text: String,
    start: Int,
    end: Int
): String {

    val contextStart =
        (start - 90).coerceAtLeast(0)

    val contextEnd =
        (end + 90).coerceAtMost(
            text.length
        )

    return text.substring(
        contextStart,
        contextEnd
    )
}

/*
 * Converts the captured number/unit into a numeric mileage.
 */
private fun parseMileageNumber(
    numberPart: String,
    unit: String
): Int? {

    val number =
        numberPart
            .replace(",", "")
            .toDoubleOrNull()
            ?: return null

    return when (
        unit.lowercase(Locale.ROOT)
    ) {

        "k" ->
            (number * 1000.0)
                .toInt()

        else ->
            number.toInt()
    }
}

/*
 * Preserves the original advertised wording format.
 */
private fun formatMileage(
    numberPart: String,
    unit: String
): String {

    return "$numberPart ${unit.uppercase(Locale.ROOT)}"
}

private data class MileageCandidate(
    val displayValue: String,
    val numericValue: Int,
    val start: Int,
    val confidenceScore: Int,
    val isDefinitelyNonOdometer: Boolean
)

/*
 * =============================================================
 * PRICE EXTRACTION
 * =============================================================
 */

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
            .filter {
                !isNonVehiclePrice(
                    text,
                    it.start,
                    it.end
                )
            }
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
                compareByDescending<Pair<PriceCandidate, Int>> {
                    it.second
                }.thenBy {
                    it.first.start
                }
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

    return immediateNonVehiclePattern.containsMatchIn(
        before
    ) ||
        immediateNonVehiclePattern.containsMatchIn(
            after
        )
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
        text.substring(
            contextStart,
            contextEnd
        ).lowercase(Locale.ROOT)

    var score = 0

    if (
        Regex("\\basking\\s+price\\b")
            .containsMatchIn(context)
    ) {
        score += 12
    }

    if (
        Regex("\\bprice\\b")
            .containsMatchIn(context)
    ) {
        score += 8
    }

    if (
        Regex("\\bcash\\s+price\\b")
            .containsMatchIn(context)
    ) {
        score += 7
    }

    if (
        Regex("\\bono\\b")
            .containsMatchIn(context)
    ) {
        score += 7
    }

    if (
        Regex("\\bfor\\s+sale\\b")
            .containsMatchIn(context)
    ) {
        score += 4
    }

    if (
        Regex("\\bdeposit\\b")
            .containsMatchIn(context)
    ) {
        score -= 20
    }

    if (
        Regex("\\bfinance\\b")
            .containsMatchIn(context)
    ) {
        score -= 15
    }

    return score
}

private fun formatPriceNumber(
    value: Double
): String {

    return if (
        value % 1.0 == 0.0
    ) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}

/*
 * =============================================================
 * BHP
 * =============================================================
 */

fun extractBhp(text: String): String? {

    if (text.isBlank()) {
        return null
    }

    return bhpPattern
        .find(text)
        ?.groupValues
        ?.getOrNull(1)
}

/*
 * =============================================================
 * MILEAGE NORMALISATION
 * =============================================================
 *
 * This method is retained for compatibility with the existing
 * callers.
 *
 * It now also protects against accidentally normalising a
 * performance/service phrase directly if one reaches this method.
 */

fun normalizeMileage(
    value: String
): Int {

    if (value.isBlank()) {
        return 0
    }

    /*
     * Never treat obvious speed expressions as odometer mileage.
     *
     * This protects callers which invoke normalizeMileage()
     * directly rather than going through extractMileage().
     */
    if (
        isStandaloneNonOdometerMileage(
            value
        )
    ) {
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
        )
            .find(cleaned)
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

private fun isStandaloneNonOdometerMileage(
    value: String
): Boolean {

    val normalized =
        value
            .lowercase(Locale.ROOT)
            .trim()

    /*
     * Speed.
     */
    if (
        Regex(
            """(?i)\b\d+(?:\.\d+)?\s*(?:miles?\s+per\s+hour|mph|km/?h|kph)\b"""
        ).containsMatchIn(normalized)
    ) {
        return true
    }

    /*
     * Performance acceleration.
     */
    if (
        Regex(
            """(?i)\b0\s*(?:to|-|–|—)\s*\d+(?:\.\d+)?\s*(?:miles?|mph|km/?h|kph)\b"""
        ).containsMatchIn(normalized)
    ) {
        return true
    }

    /*
     * Non-odometer distance uses.
     */
    if (
        nonOdometerDistancePattern
            .containsMatchIn(normalized)
    ) {
        return true
    }

    /*
     * Service/maintenance mileage.
     */
    if (
        serviceContextPattern
            .containsMatchIn(normalized) ||
            reverseServiceContextPattern
                .containsMatchIn(normalized) ||
            serviceIntervalContextPattern
                .containsMatchIn(normalized)
    ) {
        return true
    }

    /*
     * Historical mileage.
     */
    if (
        historicalMileageContextPattern
            .containsMatchIn(normalized)
    ) {
        return true
    }

    return false
}

/*
 * =============================================================
 * PRICE NORMALISATION
 * =============================================================
 */

fun normalizePrice(
    value: String
): Double? {

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