package com.hiddenhistory.engine.advert.analysis

import com.hiddenhistory.engine.UniversalVehicleDictionary

class AdvertRiskDetector {

    private val directRiskPhrases =
        linkedMapOf(

            "engine fault" to
                    "Engine fault is mentioned in the advert.",

            "engine problem" to
                    "An engine problem is mentioned in the advert.",

            "engine issue" to
                    "An engine issue is mentioned in the advert.",

            "gearbox fault" to
                    "Gearbox fault is mentioned in the advert.",

            "gearbox problem" to
                    "A gearbox problem is mentioned in the advert.",

            "gearbox issue" to
                    "A gearbox issue is mentioned in the advert.",

            "clutch slipping" to
                    "Clutch slipping is mentioned in the advert.",

            "clutch fault" to
                    "Clutch fault is mentioned in the advert.",

            "clutch problem" to
                    "A clutch problem is mentioned in the advert.",

            "head gasket" to
                    "A head gasket problem is mentioned in the advert.",

            "limp mode" to
                    "Limp mode is mentioned in the advert.",

            "oil leak" to
                    "An oil leak is mentioned in the advert.",

            "oil leaks" to
                    "An oil leak is mentioned in the advert.",

            "coolant leak" to
                    "A coolant leak is mentioned in the advert.",

            "coolant leaks" to
                    "A coolant leak is mentioned in the advert.",

            "water leak" to
                    "A water leak is mentioned in the advert.",

            "water leaks" to
                    "A water leak is mentioned in the advert.",

            "overheating" to
                    "Overheating is mentioned in the advert.",

            "overheat" to
                    "Overheating is mentioned in the advert.",

            "warning light" to
                    "A warning light is mentioned in the advert.",

            "warning lights" to
                    "A warning light is mentioned in the advert.",

            "engine management light" to
                    "An engine management light is mentioned in the advert.",

            "engine management lights" to
                    "An engine management light is mentioned in the advert.",

            "check engine light" to
                    "A check engine light is mentioned in the advert.",

            "check engine lights" to
                    "A check engine light is mentioned in the advert.",

            "abs light" to
                    "An ABS warning light is mentioned in the advert.",

            "abs lights" to
                    "An ABS warning light is mentioned in the advert.",

            "airbag light" to
                    "An airbag warning light is mentioned in the advert.",

            "airbag lights" to
                    "An airbag warning light is mentioned in the advert.",

            "non runner" to
                    "The vehicle is described as a non-runner.",

            "non-runner" to
                    "The vehicle is described as a non-runner.",

            "spares or repair" to
                    "The vehicle is advertised as spares or repair.",

            "spares and repair" to
                    "The vehicle is advertised as spares or repair.",

            "for repair" to
                    "The vehicle is described as requiring repair.",

            "needs repair" to
                    "The vehicle is described as needing repair.",

            "needs repairs" to
                    "The vehicle is described as needing repairs.",

            "damaged" to
                    "Vehicle damage is mentioned in the advert.",

            "accident damage" to
                    "Accident damage is mentioned in the advert.",

            "crash damage" to
                    "Crash damage is mentioned in the advert.",

            "written off" to
                    "A previous write-off is mentioned in the advert.",

            "write off" to
                    "A previous write-off is mentioned in the advert.",

            "category n" to
                    "Category N write-off status is mentioned in the advert.",

            "category s" to
                    "Category S write-off status is mentioned in the advert.",

            "category c" to
                    "Category C write-off status is mentioned in the advert.",

            "category d" to
                    "Category D write-off status is mentioned in the advert.",

            "modified" to
                    "Vehicle modification is mentioned in the advert.",

            "heavily modified" to
                    "Heavy vehicle modification is mentioned in the advert.",

            "smoke" to
                    "Smoke from the vehicle is mentioned in the advert.",

            "smoking" to
                    "Smoking from the vehicle is mentioned in the advert.",

            "knocking" to
                    "A knocking noise is mentioned in the advert.",

            "knock" to
                    "A knocking noise is mentioned in the advert.",

            "rattle" to
                    "A rattling noise is mentioned in the advert.",

            "rattling" to
                    "A rattling noise is mentioned in the advert.",

            "misfire" to
                    "An engine misfire is mentioned in the advert.",

            "misfiring" to
                    "Engine misfiring is mentioned in the advert.",

            "blown head gasket" to
                    "A blown head gasket is mentioned in the advert.",

            "timing chain fault" to
                    "A timing chain fault is mentioned in the advert.",

            "timing belt fault" to
                    "A timing belt fault is mentioned in the advert."
        )

    /*
     * These expressions are used to determine whether a particular
     * occurrence of a risk phrase is being denied.
     *
     * They are deliberately evaluated against the LOCAL CONTEXT
     * surrounding the occurrence rather than against the advert as
     * one large string.
     */
    private val negationPhrases =
        listOf(
            "no",
            "no known",
            "no reported",
            "no apparent",
            "without",
            "without any",
            "free from",
            "free of",
            "never",
            "does not",
            "doesn't",
            "doesnt",
            "not",
            "none",
            "zero"
        )

    /*
     * Historical wording is kept separate from current mechanical
     * risk wording.
     *
     * Example:
     *
     * "previous repairs"
     *     -> historical finding
     *
     * "no previous repairs"
     *     -> nothing
     *
     * "previous repairs, but no current faults"
     *     -> historical repair finding only
     */
    private val historicalRiskPhrases =
        linkedMapOf(

            "previously written off" to
                    "The advert states that the vehicle was previously written off.",

            "previous write off" to
                    "A previous vehicle write-off is mentioned in the advert.",

            "previous accident" to
                    "A previous accident is mentioned in the advert.",

            "previous accident damage" to
                    "Previous accident damage is mentioned in the advert.",

            "previous repair" to
                    "Previous repair work is mentioned in the advert.",

            "previous repairs" to
                    "Previous repair work is mentioned in the advert."
        )

    /*
     * Number of words inspected before a phrase occurrence.
     *
     * This gives the classifier enough local context to understand
     * normal advert wording without allowing a "no" appearing several
     * sentences earlier to affect an unrelated phrase.
     */
    private val contextWordsBefore = 8

    /*
     * Number of words inspected after a phrase occurrence.
     *
     * This is useful for structures such as:
     *
     * "fault is not present"
     * "damage is not apparent"
     */
    private val contextWordsAfter = 6

    fun detectRiskFlags(
        cleanText: String
    ): List<String> {

        if (cleanText.isBlank()) {
            return emptyList()
        }

        val riskFlags =
            mutableListOf<String>()

        /*
         * ---------------------------------------------------------
         * 1. DIRECT RISK PHRASES
         * ---------------------------------------------------------
         */

        directRiskPhrases.forEach { (phrase, description) ->

            if (
                hasNonNegatedOccurrence(
                    text = cleanText,
                    phrase = phrase
                )
            ) {
                riskFlags.add(description)
            }
        }

        /*
         * ---------------------------------------------------------
         * 2. DICTIONARY-BASED RISKS
         * ---------------------------------------------------------
         *
         * The ORIGINAL dictionary phrase must exist in the advert.
         *
         * Its translated result is only promoted to a risk when:
         *
         *     a) the dictionary result is actually a risk
         *     b) at least one occurrence is not negated
         */

        UniversalVehicleDictionary.phraseMap.forEach { (phrase, translated) ->

            if (!containsPhrase(cleanText, phrase)) {
                return@forEach
            }

            if (!isDictionaryRiskResult(translated)) {
                return@forEach
            }

            if (
                !hasNonNegatedOccurrence(
                    text = cleanText,
                    phrase = phrase
                )
            ) {
                return@forEach
            }

            riskFlags.add(translated)
        }

        /*
         * ---------------------------------------------------------
         * 3. HISTORICAL RISKS
         * ---------------------------------------------------------
         *
         * Historical phrases now use EXACTLY the same contextual
         * classification system.
         *
         * This fixes:
         *
         *     "No previous repairs."
         *
         * without breaking:
         *
         *     "Previous repairs carried out in 2022."
         *
         * or:
         *
         *     "The vehicle had previous repairs but has no faults."
         */

        historicalRiskPhrases.forEach { (phrase, description) ->

            if (
                hasNonNegatedOccurrence(
                    text = cleanText,
                    phrase = phrase
                )
            ) {
                riskFlags.add(description)
            }
        }

        /*
         * ---------------------------------------------------------
         * 4. SALES PRESSURE / PAYMENT RESTRICTIONS
         * ---------------------------------------------------------
         *
         * These are not treated as proof of fraud. They are risk
         * indicators because a buyer is being pressured to transfer
         * money and/or prevented from carrying out normal checks.
         */

        val hasDepositDemand =
            containsPhrase(cleanText, "deposit") &&
                (containsPhrase(cleanText, "transfer") ||
                        containsPhrase(cleanText, "pay") ||
                        containsPhrase(cleanText, "today") ||
                        containsPhrase(cleanText, "immediately"))

        val inspectionDiscouraged =
            containsPhrase(cleanText, "don't waste my time with inspections") ||
                containsPhrase(cleanText, "do not waste my time with inspections") ||
                containsPhrase(cleanText, "no inspections") ||
                containsPhrase(cleanText, "no inspection") ||
                containsPhrase(cleanText, "don't want inspections") ||
                containsPhrase(cleanText, "do not want inspections")

        if (hasDepositDemand) {
            riskFlags.add(
                "Immediate deposit/payment pressure is present: the advert asks the buyer to transfer money before purchase."
            )
        }

        if (inspectionDiscouraged) {
            riskFlags.add(
                "The seller discourages or restricts independent inspection of the vehicle."
            )
        }

        val urgencyCount =
            listOf(
                "quick sale",
                "must go",
                "need gone",
                "no time wasters",
                "first to see will buy",
                "first to see"
            ).count { containsPhrase(cleanText, it) }

        if (hasDepositDemand && urgencyCount >= 1) {
            riskFlags.add(
                "Payment pressure is combined with urgency language, increasing the need for independent verification before any money is transferred."
            )
        }

        return riskFlags.distinct()
    }

    fun generateKeyInsights(
        cleanText: String,
        riskFlags: List<String>
    ): List<String> {

        if (cleanText.isBlank()) {
            return emptyList()
        }

        val translatedFlags =
            UniversalVehicleDictionary
                .translatePhrase(cleanText)
                .distinct()

        return translatedFlags
            .filterNot { translated ->
                riskFlags.any { risk ->
                    risk.equals(
                        translated,
                        ignoreCase = true
                    )
                }
            }
            .distinct()
    }

    /*
     * -------------------------------------------------------------
     * CONTEXTUAL OCCURRENCE CLASSIFICATION
     * -------------------------------------------------------------
     *
     * This is now the central gatekeeper for risk detection.
     *
     * A phrase is NOT automatically considered a risk simply because
     * the characters occur in the advert.
     *
     * Every occurrence is evaluated independently.
     *
     * Example:
     *
     * "No previous repairs. Previous repair work was carried out."
     *
     * First occurrence:
     *     negated -> ignored
     *
     * Second occurrence:
     *     positive -> retained
     *
     * Therefore the final result correctly contains the historical
     * repair finding.
     */

    private fun hasNonNegatedOccurrence(
        text: String,
        phrase: String
    ): Boolean {

        val lowerText =
            text.lowercase()

        val lowerPhrase =
            phrase.lowercase()

        var searchStart = 0

        while (true) {

            val index =
                lowerText.indexOf(
                    lowerPhrase,
                    searchStart
                )

            if (index == -1) {
                return false
            }

            if (
                isOccurrenceNegated(
                    text = lowerText,
                    phraseStart = index,
                    phraseLength = lowerPhrase.length
                )
            ) {

                searchStart =
                    index +
                        lowerPhrase.length

                continue
            }

            /*
             * This specific occurrence is not negated.
             *
             * Therefore the phrase is a genuine occurrence and may
             * be promoted to a risk by the caller.
             */
            return true
        }
    }

    /*
     * -------------------------------------------------------------
     * OCCURRENCE NEGATION
     * -------------------------------------------------------------
     *
     * IMPORTANT:
     *
     * This does NOT ask:
     *
     *     "Does the advert contain the word 'no'?"
     *
     * It asks:
     *
     *     "Does the local sentence/context around THIS particular
     *      occurrence indicate that THIS occurrence is being denied?"
     *
     * This prevents unrelated negations elsewhere in the advert from
     * suppressing genuine findings.
     */

    private fun isOccurrenceNegated(
        text: String,
        phraseStart: Int,
        phraseLength: Int
    ): Boolean {

        val context =
            buildLocalContext(
                text = text,
                phraseStart = phraseStart,
                phraseLength = phraseLength
            )

        if (context.isBlank()) {
            return false
        }

        /*
         * ---------------------------------------------------------
         * 1. DIRECT NEGATION BEFORE THE PHRASE
         * ---------------------------------------------------------
         *
         * Examples:
         *
         * "no previous repairs"
         * "no oil leaks"
         * "without any damage"
         * "never written off"
         */

        if (
            hasNegationBeforePhrase(
                context = context,
                phraseStartInContext =
                    context.indexOf(
                        text.substring(
                            phraseStart,
                            phraseStart + phraseLength
                        ).lowercase()
                    )
            )
        ) {
            return true
        }

        /*
         * ---------------------------------------------------------
         * 2. NEGATION AFTER THE PHRASE
         * ---------------------------------------------------------
         *
         * Examples:
         *
         * "damage is not present"
         * "fault is not apparent"
         * "overheating is not occurring"
         */

        if (
            hasNegationAfterPhrase(
                context = context,
                phrase = text.substring(
                    phraseStart,
                    phraseStart + phraseLength
                ).lowercase()
            )
        ) {
            return true
        }

        return false
    }

    /*
     * -------------------------------------------------------------
     * LOCAL CONTEXT BUILDER
     * -------------------------------------------------------------
     */

    private fun buildLocalContext(
        text: String,
        phraseStart: Int,
        phraseLength: Int
    ): String {

        val lowerText =
            text.lowercase()

        val phraseEnd =
            phraseStart +
                phraseLength

        /*
         * Prefer the current sentence rather than allowing a
         * negation from a completely unrelated sentence to influence
         * this phrase.
         */
        val sentenceStart =
            maxOf(
                lowerText.lastIndexOf(
                    '.',
                    phraseStart - 1
                ),
                lowerText.lastIndexOf(
                    '!',
                    phraseStart - 1
                ),
                lowerText.lastIndexOf(
                    '?',
                    phraseStart - 1
                ),
                lowerText.lastIndexOf(
                    '\n',
                    phraseStart - 1
                )
            ) + 1

        val sentenceEndCandidates =
            listOf(
                lowerText.indexOf(
                    '.',
                    phraseEnd
                ),
                lowerText.indexOf(
                    '!',
                    phraseEnd
                ),
                lowerText.indexOf(
                    '?',
                    phraseEnd
                ),
                lowerText.indexOf(
                    '\n',
                    phraseEnd
                )
            )
                .filter {
                    it >= 0
                }

        val sentenceEnd =
            sentenceEndCandidates.minOrNull()
                ?: lowerText.length

        val sentence =
            lowerText.substring(
                sentenceStart,
                sentenceEnd
            ).trim()

        /*
         * If the sentence is unusually long, limit the context using
         * word boundaries as a secondary protection.
         */
        val words =
            sentence
                .split(
                    Regex("\\s+")
                )
                .filter {
                    it.isNotBlank()
                }

        if (words.isEmpty()) {
            return ""
        }

        val phraseWords =
            text.substring(
                phraseStart,
                phraseEnd
            )
                .lowercase()
                .split(
                    Regex("\\s+")
                )
                .filter {
                    it.isNotBlank()
                }

        val phraseWordCount =
            phraseWords.size

        var occurrenceWordIndex =
            findPhraseWordIndex(
                words = words,
                phraseWords = phraseWords
            )

        if (occurrenceWordIndex == -1) {
            /*
             * Fallback to the complete sentence if the phrase cannot
             * be mapped cleanly to the tokenised representation.
             */
            return sentence
        }

        val start =
            maxOf(
                0,
                occurrenceWordIndex -
                    contextWordsBefore
            )

        val end =
            minOf(
                words.size,
                occurrenceWordIndex +
                    phraseWordCount +
                    contextWordsAfter
            )

        return words
            .subList(
                start,
                end
            )
            .joinToString(" ")
    }

    /*
     * -------------------------------------------------------------
     * NEGATION BEFORE PHRASE
     * -------------------------------------------------------------
     */

    private fun hasNegationBeforePhrase(
        context: String,
        phraseStartInContext: Int
    ): Boolean {

        if (phraseStartInContext <= 0) {
            return false
        }

        val before =
            context
                .substring(
                    0,
                    phraseStartInContext
                )
                .trim()

        if (before.isBlank()) {
            return false
        }

        val words =
            before
                .split(
                    Regex("\\s+")
                )
                .filter {
                    it.isNotBlank()
                }

        /*
         * Only inspect the immediately preceding contextual words.
         *
         * This prevents:
         *
         * "No faults. Previous repairs..."
         *
         * from treating "previous repairs" as negated merely because
         * "no" appeared in the previous sentence.
         */
        val relevantWords =
            words.takeLast(
                contextWordsBefore
            )

        val relevantContext =
            relevantWords
                .joinToString(" ")

        return negationPhrases.any { negation ->

            Regex(
                """(?<![a-z0-9])${Regex.escape(negation)}(?![a-z0-9])"""
            )
                .containsMatchIn(
                    relevantContext
                )
        }
    }

    /*
     * -------------------------------------------------------------
     * NEGATION AFTER PHRASE
     * -------------------------------------------------------------
     */

    private fun hasNegationAfterPhrase(
        context: String,
        phrase: String
    ): Boolean {

        val phraseIndex =
            context.indexOf(
                phrase
            )

        if (phraseIndex == -1) {
            return false
        }

        val afterStart =
            phraseIndex +
                phrase.length

        if (afterStart >= context.length) {
            return false
        }

        val after =
            context
                .substring(
                    afterStart
                )
                .trim()

        if (after.isBlank()) {
            return false
        }

        /*
         * We specifically recognise grammatical structures where the
         * negation follows the detected phrase.
         */
        val postNegationPatterns =
            listOf(
                Regex(
                    """^(is|are|was|were|has|have|had|appears|appeared|seems|seemed)\s+(not|never)\b"""
                ),
                Regex(
                    """^(is|are|was|were)\s+(not|never)\s+(present|apparent|known|reported|occurring|there)\b"""
                ),
                Regex(
                    """^(?:is|are|was|were)\s+not\s+to\b"""
                )
            )

        return postNegationPatterns.any { pattern ->
            pattern.containsMatchIn(after)
        }
    }

    /*
     * -------------------------------------------------------------
     * PHRASE WORD LOCATION
     * -------------------------------------------------------------
     */

    private fun findPhraseWordIndex(
        words: List<String>,
        phraseWords: List<String>
    ): Int {

        if (phraseWords.isEmpty()) {
            return -1
        }

        if (phraseWords.size > words.size) {
            return -1
        }

        for (index in 0..words.size - phraseWords.size) {

            var matches =
                true

            for (offset in phraseWords.indices) {

                val actual =
                    words[index + offset]
                        .trim(
                            ',',
                            ':',
                            ';',
                            '(',
                            ')',
                            '[',
                            ']',
                            '"',
                            '\''
                        )

                val expected =
                    phraseWords[offset]
                        .trim(
                            ',',
                            ':',
                            ';',
                            '(',
                            ')',
                            '[',
                            ']',
                            '"',
                            '\''
                        )

                if (
                    actual != expected
                ) {
                    matches = false
                    break
                }
            }

            if (matches) {
                return index
            }
        }

        return -1
    }

    /*
     * -------------------------------------------------------------
     * DICTIONARY RISK CLASSIFICATION
     * -------------------------------------------------------------
     */

    private fun isDictionaryRiskResult(
        translated: String
    ): Boolean {

        val result =
            translated.lowercase()

        return result.contains("fault") ||
                result.contains("broken") ||
                result.contains("damaged") ||
                result.contains("damage") ||
                result.contains("repair") ||
                result.contains("write-off") ||
                result.contains("written off") ||
                result.contains("non-runner") ||
                result.contains("non runner") ||
                result.contains("modified") ||
                result.contains("limp mode") ||
                result.contains("clutch worn") ||
                result.contains("cat deleted") ||
                result.contains("severe engine")
    }

    /*
     * -------------------------------------------------------------
     * WHOLE-PHRASE MATCHING
     * -------------------------------------------------------------
     */

    private fun containsPhrase(
        text: String,
        phrase: String
    ): Boolean {

        val escaped =
            Regex.escape(phrase)

        return Regex(
            """(?<![a-z0-9])$escaped(?![a-z0-9])""",
            RegexOption.IGNORE_CASE
        )
            .containsMatchIn(text)
    }
}