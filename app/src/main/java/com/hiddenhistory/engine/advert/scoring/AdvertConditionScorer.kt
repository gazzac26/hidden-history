package com.hiddenhistory.engine.advert.scoring

class AdvertConditionScorer {

    fun calculateConditionScore(
        lowerText: String,
        riskFlags: List<String>,
        missingInformation: List<String>,
        claims: List<String>
    ): Int {

        /*
         * This is an ADVERT QUALITY / CONFIDENCE INDEX.
         *
         * It is NOT a mechanical condition score.
         *
         * The score represents how much useful, internally coherent
         * and reassuring information the advert provides before the
         * vehicle is independently verified.
         *
         * 50 is the neutral starting point.
         */

        var score = 50

        // ---------------------------------------------------------
        // 1. POSITIVE DOCUMENTATION / TRANSPARENCY EVIDENCE
        // ---------------------------------------------------------

        val positiveEvidence = linkedMapOf(

            "full service history" to 8,
            "full dealer service history" to 8,
            "service history" to 4,
            "service records" to 4,
            "service record" to 4,

            "timing belt replaced" to 5,
            "timing belt and water pump" to 6,
            "water pump replaced" to 3,

            "2 keys" to 4,
            "both keys" to 4,
            "spare key" to 3,

            "one owner" to 4,
            "1 owner" to 4,

            "v5 present" to 3,
            "logbook present" to 3,
            "full logbook" to 3,

            "recent mot" to 3,
            "fresh mot" to 3
        )

        positiveEvidence.forEach { (phrase, points) ->

            if (containsPhrase(lowerText, phrase)) {
                score += points
            }
        }

        // ---------------------------------------------------------
        // 2. SPECIFIC, VERIFIABLE INFORMATION
        // ---------------------------------------------------------

        /*
         * A detailed advert gives the buyer more information to
         * cross-check. These are intentionally modest additions.
         */

        if (containsMileage(lowerText)) {
            score += 3
        }

        if (containsPrice(lowerText)) {
            score += 2
        }

        if (containsYear(lowerText)) {
            score += 2
        }

        if (containsMotInformation(lowerText)) {
            score += 3
        }

        if (
            containsPhrase(lowerText, "registration") ||
            containsPhrase(lowerText, "vrm") ||
            containsPhrase(lowerText, "reg ")
        ) {
            score += 2
        }

        if (
            containsPhrase(lowerText, "engine") ||
            containsPhrase(lowerText, "1.0") ||
            containsPhrase(lowerText, "1.2") ||
            containsPhrase(lowerText, "1.4") ||
            containsPhrase(lowerText, "1.5") ||
            containsPhrase(lowerText, "1.6") ||
            containsPhrase(lowerText, "1.8") ||
            containsPhrase(lowerText, "2.0") ||
            containsPhrase(lowerText, "2.2") ||
            containsPhrase(lowerText, "2.5") ||
            containsPhrase(lowerText, "3.0")
        ) {
            score += 2
        }

        if (
            containsPhrase(lowerText, "manual") ||
            containsPhrase(lowerText, "automatic")
        ) {
            score += 2
        }

        if (
            containsPhrase(lowerText, "diesel") ||
            containsPhrase(lowerText, "petrol") ||
            containsPhrase(lowerText, "hybrid") ||
            containsPhrase(lowerText, "electric")
        ) {
            score += 2
        }

        // ---------------------------------------------------------
        // 3. SELLER TRANSPARENCY
        // ---------------------------------------------------------

        /*
         * Explicitly acknowledging faults is not automatically a
         * negative signal.
         *
         * An advert that openly identifies a known issue is more
         * transparent than one claiming perfection while hiding it.
         */

        val disclosedFaultPhrases = listOf(
            "fault",
            "issue",
            "problem",
            "repair",
            "needs work",
            "requires work",
            "damage",
            "damaged",
            "warning light",
            "oil leak",
            "coolant leak",
            "clutch slipping",
            "gearbox fault",
            "engine fault"
        )

        val disclosedFaultCount =
            disclosedFaultPhrases.count {
                containsPhrase(lowerText, it)
            }

        if (disclosedFaultCount > 0) {

            /*
             * Small neutrality adjustment rather than an automatic
             * large penalty. The actual risk detector is responsible
             * for identifying the seriousness of the issue.
             */
            score -= (disclosedFaultCount.coerceAtMost(3) * 2)
        }

        // ---------------------------------------------------------
        // 4. HIGH-RISK VEHICLE / ADVERT SIGNALS
        // ---------------------------------------------------------

        val severeRiskPhrases = linkedMapOf(

            "spares or repair" to 15,
            "non runner" to 15,
            "non-runner" to 15,

            "cat a" to 20,
            "cat b" to 20,
            "cat c" to 12,
            "cat d" to 12,
            "cat s" to 10,
            "cat n" to 8,

            "write off" to 15,
            "write-off" to 15,

            "engine failure" to 15,
            "gearbox failure" to 15,
            "head gasket" to 12,
            "overheating" to 10,
            "limp mode" to 10
        )

        severeRiskPhrases.forEach { (phrase, penalty) ->

            if (containsPhrase(lowerText, phrase)) {
                score -= penalty
            }
        }

        // ---------------------------------------------------------
        // 5. RISK FLAGS ALREADY PRODUCED BY THE RISK ENGINE
        // ---------------------------------------------------------

        /*
         * Do not blindly subtract a fixed amount for every risk flag.
         *
         * Risk flags can represent very different levels of concern.
         * Instead classify the returned deterministic descriptions.
         */

        riskFlags.forEach { flag ->

            val flagText =
                flag.lowercase()

            when {

                flagText.contains("category a") ||
                        flagText.contains("category b") ||
                        flagText.contains("non-runner") ||
                        flagText.contains("non runner") ||
                        flagText.contains("engine failure") ||
                        flagText.contains("gearbox failure") -> {

                    score -= 12
                }

                flagText.contains("write-off") ||
                        flagText.contains("write off") ||
                        flagText.contains("category c") ||
                        flagText.contains("category d") ||
                        flagText.contains("category s") -> {

                    score -= 8
                }

                flagText.contains("fault") ||
                        flagText.contains("repair") ||
                        flagText.contains("damaged") ||
                        flagText.contains("damage") ||
                        flagText.contains("broken") -> {

                    score -= 5
                }

                flagText.contains("immediate deposit") ||
                        flagText.contains("discourages or restricts independent inspection") -> {

                    score -= 6
                }

                flagText.contains("payment pressure is combined with urgency") -> {

                    score -= 4
                }

                flagText.contains("modified") -> {

                    score -= 3
                }
            }
        }

        // ---------------------------------------------------------
        // 6. CONTRADICTION / OVER-PROMISING LANGUAGE
        // ---------------------------------------------------------

        if (
            containsPhrase(lowerText, "no issues") &&
            containsAny(
                lowerText,
                "fault",
                "problem",
                "repair",
                "needs work",
                "damage"
            )
        ) {
            score -= 8
        }

        if (
            containsPhrase(lowerText, "no faults") &&
            containsAny(
                lowerText,
                "fault",
                "repair",
                "warning light",
                "engine fault",
                "gearbox fault"
            )
        ) {
            score -= 8
        }

        if (
            containsPhrase(lowerText, "perfect") &&
            containsAny(
                lowerText,
                "fault",
                "damage",
                "repair",
                "warning light"
            )
        ) {
            score -= 6
        }

        if (
            containsPhrase(lowerText, "mint condition") &&
            containsAny(
                lowerText,
                "damage",
                "repair",
                "fault"
            )
        ) {
            score -= 5
        }

        // ---------------------------------------------------------
        // 7. PRESSURE / URGENCY LANGUAGE
        // ---------------------------------------------------------

        val pressurePhrases = listOf(
            "quick sale",
            "must go",
            "need gone",
            "no time wasters",
            "first to see will buy",
            "first to see",
            "bargain"
        )

        val pressureCount =
            pressurePhrases.count {
                containsPhrase(lowerText, it)
            }

        score -= (pressureCount.coerceAtMost(3) * 2)

        // ---------------------------------------------------------
        // 8. EXCESSIVE SUBJECTIVE MARKETING LANGUAGE
        // ---------------------------------------------------------

        val subjectiveClaims = listOf(
            "immaculate",
            "perfect",
            "mint condition",
            "drives like a dream",
            "mechanically sound",
            "excellent condition",
            "rare",
            "bargain",
            "cheap"
        )

        val subjectiveCount =
            subjectiveClaims.count {
                containsPhrase(lowerText, it)
            }

        /*
         * Subjective language is not proof of fraud.
         * The deduction is therefore deliberately small.
         */

        if (subjectiveCount >= 4) {
            score -= 5
        } else if (subjectiveCount >= 2) {
            score -= 2
        }

        // ---------------------------------------------------------
        // 9. MISSING INFORMATION
        // ---------------------------------------------------------

        /*
         * Missing information is one of the strongest advert-quality
         * signals because it directly reduces what can be independently
         * checked from the listing.
         */

        when {

            missingInformation.size >= 12 -> {
                score -= 18
            }

            missingInformation.size >= 9 -> {
                score -= 14
            }

            missingInformation.size >= 6 -> {
                score -= 9
            }

            missingInformation.size >= 4 -> {
                score -= 5
            }

            missingInformation.size >= 2 -> {
                score -= 2
            }
        }

        // ---------------------------------------------------------
        // 10. PARTICULARLY IMPORTANT MISSING INFORMATION
        // ---------------------------------------------------------

        missingInformation.forEach { missing ->

            val missingText =
                missing.lowercase()

            when {

                missingText.contains("mileage") -> {
                    score -= 3
                }

                missingText.contains("asking price") ||
                        missingText.contains("price") -> {
                    score -= 3
                }

                missingText.contains("mot expiry") ||
                        missingText.contains("mot") -> {
                    score -= 3
                }

                missingText.contains("service history") -> {
                    score -= 3
                }

                missingText.contains("v5c") ||
                        missingText.contains("logbook") -> {
                    score -= 3
                }

                missingText.contains("make and model") -> {
                    score -= 3
                }
            }
        }

        // ---------------------------------------------------------
        // 11. CLAIM EVIDENCE
        // ---------------------------------------------------------

        /*
         * Claims are useful because they tell the rest of the engine
         * exactly what the seller is asserting.
         *
         * They do NOT automatically increase the score.
         *
         * A claim is not evidence merely because the seller made it.
         *
         * We only reward claims that contain objectively useful
         * supporting information.
         */

        val evidenceBackedClaimPhrases = listOf(
            "full service history",
            "full dealer service history",
            "2 keys",
            "both keys",
            "v5 present",
            "logbook present"
        )

        val supportedClaimCount =
            claims.count { claim ->

                evidenceBackedClaimPhrases.any { phrase ->
                    claim.contains(
                        phrase,
                        ignoreCase = true
                    )
                }
            }

        score += supportedClaimCount.coerceAtMost(4)

        // ---------------------------------------------------------
        // 12. LIMIT AND NORMALISE
        // ---------------------------------------------------------

        return score.coerceIn(0, 100)
    }

    // -------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------

    private fun containsPhrase(
        text: String,
        phrase: String
    ): Boolean {

        val escaped =
            Regex.escape(phrase)

        return Regex(
            """(?<![a-z0-9])$escaped(?![a-z0-9])""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(text)
    }

    private fun containsAny(
        text: String,
        vararg phrases: String
    ): Boolean {

        return phrases.any {
            containsPhrase(
                text = text,
                phrase = it
            )
        }
    }

    private fun containsMileage(
        text: String
    ): Boolean {

        return Regex(
            """\b\d{1,3}(?:,\d{3})*(?:\.\d+)?\s?(?:k|miles?|mls)\b""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(text)
    }

    private fun containsPrice(
        text: String
    ): Boolean {

        return Regex(
            """£\s?\d{1,3}(?:,\d{3})*(?:\.\d+)?""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(text)
    }

    private fun containsYear(
        text: String
    ): Boolean {

        return Regex(
            """\b(?:19[89]\d|20[0-2]\d)\b"""
        ).containsMatchIn(text)
    }

    private fun containsMotInformation(
        text: String
    ): Boolean {

        return Regex(
            """\b\d+\s+months?\s+mot\b""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(text) ||
                containsPhrase(text, "mot until") ||
                containsPhrase(text, "mot expires")
    }
}