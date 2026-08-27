package com.hiddenhistory.engine.advert.analysis

import java.util.Locale

class AdvertClaimDetector {

    /*
     * Seller-condition and history claims.
     *
     * These are deliberately treated as CLAIMS rather than facts.
     * Hidden History must never convert seller wording into verification.
     */
    private val sellerClaims = linkedMapOf(

        "excellent condition" to
                "The vehicle is in excellent condition.",

        "good condition" to
                "The vehicle is in good condition.",

        "mint condition" to
                "The vehicle is described as being in mint condition.",

        "immaculate" to
                "The vehicle is described as immaculate.",

        "mechanically sound" to
                "The vehicle is claimed to be mechanically sound.",

        "starts and drives like new" to
                "The vehicle is claimed to start and drive like new.",

        "starts and drives as it should" to
                "The vehicle is claimed to start and drive as it should.",

        "runs and drives as it should" to
                "The vehicle is claimed to run and drive as it should.",

        "drives like a dream" to
                "The vehicle is claimed to drive exceptionally well.",

        "no issues" to
                "The vehicle is claimed to have no issues.",

        "no faults" to
                "The vehicle is claimed to have no faults.",

        "no problems" to
                "The vehicle is claimed to have no problems.",

        "no known faults" to
                "The vehicle is claimed to have no known faults.",

        "no known issues" to
                "The vehicle is claimed to have no known issues.",

        "no knocks or bangs" to
                "The vehicle is claimed to have no knocks or bangs.",

        "no warning lights" to
                "The vehicle is claimed to have no warning lights.",

        "no oil leaks" to
                "The vehicle is claimed to have no oil leaks.",

        "no overheating" to
                "The vehicle is claimed to have no overheating issues.",

        "no mechanical issues" to
                "The vehicle is claimed to have no known mechanical issues.",

        "full logbook" to
                "A full logbook is claimed to be present.",

        "logbook present" to
                "The logbook is claimed to be present.",

        "v5 present" to
                "The V5C logbook is claimed to be present.",

        "v5c present" to
                "The V5C logbook is claimed to be present.",

        "hpi clear" to
                "The vehicle is claimed to be HPI clear.",

        "clean history" to
                "The vehicle is claimed to have a clean history.",

        "full service history" to
                "The vehicle is claimed to have a full service history.",

        "full dealer service history" to
                "The vehicle is claimed to have a full dealer service history.",

        "partial service history" to
                "The vehicle is claimed to have partial service history.",

        "one owner" to
                "The vehicle is claimed to have had one owner.",

        "1 owner" to
                "The vehicle is claimed to have had one owner.",

        "one careful owner" to
                "The vehicle is claimed to have had one careful owner.",

        "one owner from new" to
                "The vehicle is claimed to have had one owner from new.",

        "two owners" to
                "The vehicle is claimed to have had two owners.",

        "2 owners" to
                "The vehicle is claimed to have had two owners.",

        "warranted mileage" to
                "The stated mileage is claimed to be warranted.",

        "warranted miles" to
                "The stated mileage is claimed to be warranted.",

        "motorway miles" to
                "The vehicle is claimed to have predominantly covered motorway miles.",

        "warranted motorway miles" to
                "The vehicle is claimed to have covered warranted motorway miles."
    )

    /*
     * Vehicle feature/equipment claims.
     *
     * These are still seller claims. They are not treated as verified
     * equipment specifications.
     */
    private val featureClaims = linkedMapOf(

        "central locking" to
                "The vehicle is claimed to have central locking.",

        "electric windows" to
                "The vehicle is claimed to have electric windows.",

        "electrical windows" to
                "The vehicle is claimed to have electric windows.",

        "climate control" to
                "The vehicle is claimed to have climate control.",

        "air conditioning" to
                "The vehicle is claimed to have air conditioning.",

        "cd player" to
                "The vehicle is claimed to have a CD player.",

        "alloy wheels" to
                "The vehicle is claimed to have alloy wheels.",

        "alloys" to
                "The vehicle is claimed to have alloy wheels.",

        "leather seats" to
                "The vehicle is claimed to have leather seats.",

        "heated seats" to
                "The vehicle is claimed to have heated seats.",

        "sat nav" to
                "The vehicle is claimed to have satellite navigation.",

        "satellite navigation" to
                "The vehicle is claimed to have satellite navigation.",

        "parking sensors" to
                "The vehicle is claimed to have parking sensors.",

        "reverse camera" to
                "The vehicle is claimed to have a reversing camera.",

        "reversing camera" to
                "The vehicle is claimed to have a reversing camera.",

        "cruise control" to
                "The vehicle is claimed to have cruise control."
    )

    /*
     * Marketing / persuasive wording.
     *
     * These are deliberately separated from factual claims.
     */
    private val notablePhrases = linkedMapOf(

        "excellent condition" to
                "Excellent condition",

        "good condition" to
                "Good condition",

        "mint condition" to
                "Mint condition",

        "immaculate" to
                "Immaculate",

        "perfect" to
                "Perfect",

        "starts and drives like new" to
                "Starts and drives like new",

        "drives like a dream" to
                "Drives like a dream",

        "mechanically sound" to
                "Mechanically sound",

        "no issues" to
                "No issues",

        "no faults" to
                "No faults",

        "no problems" to
                "No problems",

        "no warning lights" to
                "No warning lights",

        "no oil leaks" to
                "No oil leaks",

        "no overheating" to
                "No overheating",

        "warranted motorway miles" to
                "Warranted motorway miles",

        "warranted miles" to
                "Warranted mileage",

        "warranted mileage" to
                "Warranted mileage",

        "clean history" to
                "Clean history",

        "hpi clear" to
                "HPI clear",

        "alloys worth" to
                "Alloys worth",

        "one careful owner" to
                "One careful owner",

        "one owner from new" to
                "One owner from new",

        "no time wasters" to
                "No time wasters",

        "quick sale" to
                "Quick sale",

        "must go" to
                "Must go",

        "need gone" to
                "Need gone",

        "first to see will buy" to
                "First to see will buy",

        "first to see" to
                "First to see",

        "bargain" to
                "Bargain",

        "cheap" to
                "Cheap",

        "rare" to
                "Rare"
    )

    fun detectClaims(
        lowerText: String,
        mileage: String?,
        price: String?
    ): List<String> {

        val claims = mutableListOf<String>()

        val normalizedText =
            lowerText
                .lowercase(Locale.ROOT)
                .replace(Regex("\\s+"), " ")
                .trim()

        /*
         * Seller condition/history claims.
         */
        sellerClaims.forEach { (phrase, description) ->

            if (containsPhrase(normalizedText, phrase)) {
                claims.add(description)
            }
        }

        /*
         * Feature claims.
         */
        featureClaims.forEach { (phrase, description) ->

            if (containsPhrase(normalizedText, phrase)) {
                claims.add(description)
            }
        }

        /*
         * Advertised mileage.
         *
         * This is intentionally described as "stated mileage".
         */
        mileage?.takeIf { it.isNotBlank() }?.let {

            claims.add(
                "The vehicle is advertised with a stated mileage of $it."
            )
        }

        /*
         * Advertised asking price.
         */
        price?.takeIf { it.isNotBlank() }?.let {

            claims.add(
                "The advertised asking price is $it."
            )
        }

        /*
         * MOT-duration claim.
         *
         * This records what the seller has stated.
         * It does NOT verify the MOT.
         */
        val motMatch =
            Regex(
                """\b(\d+)\s+months?\s+(?:of\s+)?mot\b""",
                RegexOption.IGNORE_CASE
            ).find(normalizedText)

        motMatch
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.let { months ->

                claims.add(
                    "The seller states that the vehicle has $months months of MOT remaining."
                )
            }

        /*
         * Common direct MOT expiry wording.
         *
         * We deliberately do not attempt to calculate an exact date here.
         * The official MOT engine remains responsible for verification.
         */
        if (
            containsPhrase(normalizedText, "mot until") ||
            containsPhrase(normalizedText, "mot expires") ||
            containsPhrase(normalizedText, "mot expiry")
        ) {

            claims.add(
                "The advert contains a stated MOT expiry or MOT-duration claim."
            )
        }

        /*
         * Write-off / accident / theft / finance claims.
         *
         * These are especially important because sellers may describe
         * these as facts even though they require independent verification.
         */
        if (
            containsPhrase(normalizedText, "never been written off") ||
            containsPhrase(normalizedText, "never written off") ||
            containsPhrase(normalizedText, "not written off")
        ) {

            claims.add(
                "The seller claims that the vehicle has never been written off."
            )
        }

        if (
            containsPhrase(normalizedText, "never stolen") ||
            containsPhrase(normalizedText, "never been stolen") ||
            containsPhrase(normalizedText, "not stolen")
        ) {

            claims.add(
                "The seller claims that the vehicle has never been stolen."
            )
        }

        if (
            containsPhrase(normalizedText, "no outstanding finance") ||
            containsPhrase(normalizedText, "no finance outstanding") ||
            containsPhrase(normalizedText, "finance clear")
        ) {

            claims.add(
                "The seller claims that there is no outstanding finance on the vehicle."
            )
        }

        if (
            containsPhrase(normalizedText, "no accident damage") ||
            containsPhrase(normalizedText, "never been in an accident") ||
            containsPhrase(normalizedText, "never had an accident")
        ) {

            claims.add(
                "The seller claims that the vehicle has no accident history or accident damage."
            )
        }

        /*
         * Ownership / maintenance claims.
         */
        if (
            containsPhrase(normalizedText, "carefully maintained") ||
            containsPhrase(normalizedText, "very carefully maintained") ||
            containsPhrase(normalizedText, "well maintained")
        ) {

            claims.add(
                "The vehicle is claimed to have been well maintained."
            )
        }

        if (
            containsPhrase(normalizedText, "never been abused") ||
            containsPhrase(normalizedText, "not been abused")
        ) {

            claims.add(
                "The seller claims that the vehicle has never been abused."
            )
        }

        return claims
            .distinct()
            .filter { it.isNotBlank() }
    }

    fun detectNotableWording(
        lowerText: String
    ): List<String> {

        val normalizedText =
            lowerText
                .lowercase(Locale.ROOT)
                .replace(Regex("\\s+"), " ")
                .trim()

        return notablePhrases
            .filter { (phrase, _) ->
                containsPhrase(normalizedText, phrase)
            }
            .values
            .distinct()
    }

    /*
     * Whole-phrase matching.
     *
     * Prevents examples such as:
     *
     * "auto" matching "automatic"
     * "rare" matching part of another word
     * "v5" matching unrelated text containing v5
     */
    private fun containsPhrase(
        text: String,
        phrase: String
    ): Boolean {

        val normalizedPhrase =
            phrase
                .lowercase(Locale.ROOT)
                .replace(Regex("\\s+"), " ")
                .trim()

        val escaped =
            Regex.escape(normalizedPhrase)

        return Regex(
            """(?<![a-z0-9])$escaped(?![a-z0-9])""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(text)
    }
}