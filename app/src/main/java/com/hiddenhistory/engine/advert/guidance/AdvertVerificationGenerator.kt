package com.hiddenhistory.engine.advert.guidance

class AdvertVerificationGenerator {

    fun generateVerificationChecks(
        lowerText: String,
        mileage: String?,
        riskFlags: List<String>
    ): List<String> {

        val checks = mutableListOf<String>()

        // ---------------------------------------------------------
        // 1. MILEAGE
        // ---------------------------------------------------------
        //
        // IMPORTANT:
        //
        // Mileage by itself is NOT a verification requirement.
        //
        // The Advert ↔ Official Cross-Check engine is responsible
        // for determining whether an actual mileage discrepancy
        // exists.
        //
        // Therefore we deliberately DO NOT add:
        //
        // "Compare the stated mileage..."
        //
        // merely because the advert contains a mileage value.
        //
        // If the advert contains no mileage, nothing is generated
        // here either.
        //
        // If an actual discrepancy is found, the cross-check engine
        // supplies the appropriate verification action.
        // ---------------------------------------------------------

        if (
            containsPositivePhrase(
                lowerText,
                "warranted mileage",
                "warranted miles",
                "genuine mileage",
                "genuine miles",
                "verified mileage"
            )
        ) {
            checks.add(
                "Any claim that the mileage is genuine, warranted or verified should be supported by service records, previous MOT readings or other reliable documentation."
            )
        }

        if (
            containsPositivePhrase(
                lowerText,
                "motorway miles",
                "mostly motorway miles",
                "mainly motorway miles"
            )
        ) {
            checks.add(
                "A claim that the vehicle has mainly covered motorway miles should be treated as a seller claim and supported where possible by service history and previous mileage records."
            )
        }

        // ---------------------------------------------------------
        // 2. HISTORY / FINANCE / WRITE-OFF CLAIMS
        // ---------------------------------------------------------

        if (
            containsPositivePhrase(
                lowerText,
                "hpi clear",
                "hpi checked",
                "clean history",
                "clean car",
                "never written off",
                "never been written off",
                "never stolen",
                "never been stolen",
                "no outstanding finance",
                "no finance"
            )
        ) {
            checks.add(
                "Any claim about a clean history, finance status, theft history or write-off status should be independently verified using a current vehicle history check."
            )
        }

        // ---------------------------------------------------------
        // 3. CONDITION CLAIMS
        // ---------------------------------------------------------

        if (
            containsPositivePhrase(
                lowerText,
                "good condition",
                "excellent condition",
                "mint condition",
                "immaculate condition",
                "pristine condition",
                "perfect condition"
            )
        ) {
            checks.add(
                "Subjective condition claims should be treated as seller statements and verified through a physical inspection, test drive and, where appropriate, an independent inspection."
            )
        }

        // ---------------------------------------------------------
        // 4. MECHANICAL CONDITION CLAIMS
        // ---------------------------------------------------------

        if (
            containsPositivePhrase(
                lowerText,
                "drives perfectly",
                "drives like new",
                "starts and drives like new",
                "runs and drives perfectly",
                "runs and drives as it should",
                "no issues",
                "no problems",
                "no faults",
                "no mechanical issues",
                "no known mechanical issues",
                "mechanically perfect"
            )
        ) {
            checks.add(
                "Claims about mechanical condition or the absence of faults should be verified during a thorough test drive and, where appropriate, an independent mechanical inspection."
            )
        }

        // ---------------------------------------------------------
        // 5. SPECIFIC MECHANICAL CLAIMS
        // ---------------------------------------------------------

        if (
            containsPositivePhrase(
                lowerText,
                "engine is smooth",
                "engine is smooth and quiet",
                "engine runs perfectly",
                "gearbox is perfect",
                "gearbox is excellent",
                "clutch is excellent",
                "clutch is perfect",
                "brakes are excellent",
                "brakes are perfect",
                "suspension is excellent",
                "suspension is perfect"
            )
        ) {
            checks.add(
                "Specific mechanical condition claims should be independently assessed during inspection and test drive rather than treated as verified facts."
            )
        }

        // ---------------------------------------------------------
        // 6. BODYWORK / ACCIDENT CLAIMS
        // ---------------------------------------------------------

        if (
            containsPositivePhrase(
                lowerText,
                "no accident damage",
                "never crashed",
                "never been crashed",
                "no previous repairs",
                "all panels original",
                "original panels",
                "no dents",
                "no body damage"
            )
        ) {
            checks.add(
                "Claims about accident history, original panels or previous repairs should be checked through physical inspection and, where available, an independent vehicle history report."
            )
        }

        // ---------------------------------------------------------
        // 7. V5C / DOCUMENTATION
        // ---------------------------------------------------------

        if (
            containsPositivePhrase(
                lowerText,
                "full logbook",
                "logbook present",
                "v5 present",
                "v5c present",
                "v5 included",
                "logbook included"
            )
        ) {
            checks.add(
                "The V5C logbook should be physically inspected and its details checked against the vehicle and seller before purchase."
            )
        }

        // ---------------------------------------------------------
        // 8. SERVICE HISTORY
        // ---------------------------------------------------------

        if (
            containsPositivePhrase(
                lowerText,
                "full service history",
                "fsh",
                "fully serviced",
                "complete service history",
                "main dealer history"
            )
        ) {
            checks.add(
                "Any service-history claim should be supported by service records, invoices or digital service records where available."
            )
        }

        // ---------------------------------------------------------
        // 9. MOT CLAIMS
        // ---------------------------------------------------------

        if (
            Regex(
                """\b(?:mot|MOT)\s+(?:until|to)\s+[a-z]+\s+(?:19|20)\d{2}\b""",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(lowerText) ||
            Regex(
                """\b(?:mot|MOT)\s+(?:until|to)\s+\d{1,2}[\/\-]\d{1,2}[\/\-](?:19|20)\d{2}\b""",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(lowerText)
        ) {
            checks.add(
                "The advertised MOT expiry date should be checked against the official MOT record for the exact vehicle registration."
            )
        }

        if (
            containsPositivePhrase(
                lowerText,
                "no advisories",
                "no advisories on the last mot",
                "clean mot",
                "passed mot"
            )
        ) {
            checks.add(
                "The claimed MOT result should be compared with the official MOT history, including failures and advisories recorded against previous tests."
            )
        }

        // ---------------------------------------------------------
        // 10. KEYS
        // ---------------------------------------------------------

        if (
            containsPositivePhrase(
                lowerText,
                "two keys",
                "2 keys",
                "both keys",
                "spare key",
                "spare keys",
                "two original keys"
            )
        ) {
            checks.add(
                "The stated number of keys should be confirmed when viewing the vehicle and any spare keys should be tested."
            )
        }

        // ---------------------------------------------------------
        // 11. MODIFICATIONS / EXTRAS
        // ---------------------------------------------------------

        if (
            containsPositivePhrase(
                lowerText,
                "modified",
                "modified vehicle",
                "remapped",
                "remap",
                "stage 1",
                "stage 2",
                "performance exhaust",
                "aftermarket exhaust",
                "lowered",
                "lowering springs"
            )
        ) {
            checks.add(
                "Any modifications should be identified, inspected and confirmed as properly fitted and legally declared where required."
            )
        }

        // ---------------------------------------------------------
        // 12. ADVERTISED VALUE CLAIMS
        // ---------------------------------------------------------

        if (
            containsPositivePhrase(
                lowerText,
                "alloys worth",
                "wheels worth",
                "worth £"
            )
        ) {
            checks.add(
                "Any stated monetary value for aftermarket parts or accessories should be treated as a seller valuation and checked independently."
            )
        }

        // ---------------------------------------------------------
        // 13. SELLER URGENCY / PRESSURE
        // ---------------------------------------------------------

        if (
            containsPositivePhrase(
                lowerText,
                "quick sale",
                "must go",
                "need gone",
                "needs to go",
                "no time wasters",
                "first come first served",
                "cash only",
                "bank transfer only"
            )
        ) {
            checks.add(
                "Urgency, pressure or restrictive payment language should not replace normal vehicle checks. Buyers should avoid sending money before establishing the vehicle's identity, condition and seller details."
            )
        }

        // ---------------------------------------------------------
        // 14. LOCATION / VIEWING
        // ---------------------------------------------------------

        if (
            containsPositivePhrase(
                lowerText,
                "viewing welcome",
                "viewing and test drive welcome",
                "test drive welcome",
                "come and view"
            )
        ) {
            checks.add(
                "The vehicle should be viewed at a genuine physical location and the buyer should confirm that the person selling it has the right to sell it."
            )
        }

        // ---------------------------------------------------------
        // 15. GENERAL RISK FOLLOW-UP
        // ---------------------------------------------------------

        if (riskFlags.isNotEmpty()) {
            checks.add(
                "The risk indicators identified in the advert should be independently investigated before purchase. An advert analysis identifies statements and warning signs; it does not verify the seller's claims."
            )
        }

        return checks
            .filter { it.isNotBlank() }
            .distinct()
    }

    // -------------------------------------------------------------
    // POSITIVE PHRASE DETECTION
    //
    // Prevents negated statements from being treated as positive
    // claims.
    //
    // Example:
    //
    // "no overheating"      -> NOT a positive overheating claim
    // "engine overheating"  -> positive mechanical claim
    // -------------------------------------------------------------

    private fun containsPositivePhrase(
        text: String,
        vararg phrases: String
    ): Boolean {

        return phrases.any { phrase ->

            val escapedPhrase =
                Regex.escape(phrase)

            val pattern =
                Regex(
                    """(?<![a-z0-9])$escapedPhrase(?![a-z0-9])""",
                    RegexOption.IGNORE_CASE
                )

            val match =
                pattern.find(text)
                    ?: return@any false

            !isNegated(
                text = text,
                startIndex = match.range.first
            )
        }
    }

    private fun isNegated(
        text: String,
        startIndex: Int
    ): Boolean {

        val precedingText =
            text.substring(
                0,
                startIndex
            )

        val words =
            precedingText
                .trim()
                .split(Regex("\\s+"))
                .takeLast(6)

        val negationWords =
            setOf(
                "no",
                "not",
                "never",
                "without",
                "none",
                "nothing",
                "neither",
                "nor"
            )

        return words.any {
            it.lowercase() in negationWords
        }
    }
}