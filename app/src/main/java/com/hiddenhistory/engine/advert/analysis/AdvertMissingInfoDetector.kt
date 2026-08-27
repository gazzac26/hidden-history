package com.hiddenhistory.engine.advert.analysis

class AdvertMissingInfoDetector {

    fun identifyMissingInformation(
        lowerText: String,
        year: Int?,
        mileage: String?,
        price: String?,
        fuelType: String?,
        transmission: String?,
        engineSize: String?
    ): List<String> {

        val missing = mutableListOf<String>()

        // ---------------------------------------------------------
        // VEHICLE IDENTITY
        // ---------------------------------------------------------

        if (!containsMake(lowerText)) {
            missing.add(
                "The vehicle make."
            )
        }

        if (!containsModel(lowerText)) {
            missing.add(
                "The vehicle model."
            )
        }

        if (year == null) {
            missing.add(
                "The year of manufacture."
            )
        }

        // ---------------------------------------------------------
        // CORE VEHICLE SPECIFICATION
        // ---------------------------------------------------------

        if (engineSize == null) {
            missing.add(
                "The engine size."
            )
        }

        if (fuelType == null) {
            missing.add(
                "The specific fuel type."
            )
        }

        if (transmission == null) {
            missing.add(
                "The transmission type."
            )
        }

        // ---------------------------------------------------------
        // PRICE / MILEAGE
        // ---------------------------------------------------------

        if (mileage == null) {
            missing.add(
                "The vehicle's stated mileage."
            )
        }

        if (price == null) {
            missing.add(
                "The asking price."
            )
        }

        // ---------------------------------------------------------
        // REGISTRATION
        //
        // A registration is one of the most important identifiers
        // for subsequent official vehicle verification.
        // ---------------------------------------------------------

        if (!containsRegistration(lowerText)) {
            missing.add(
                "The vehicle registration number."
            )
        }

        // ---------------------------------------------------------
        // MOT
        //
        // Accept both explicit expiry dates and relative wording.
        // ---------------------------------------------------------

        if (!containsMotInformation(lowerText)) {
            missing.add(
                "The exact MOT expiry date."
            )
        }

        // ---------------------------------------------------------
        // SERVICE HISTORY
        // ---------------------------------------------------------

        if (!containsServiceHistory(lowerText)) {
            missing.add(
                "Details of the vehicle's service history."
            )
        }

        // ---------------------------------------------------------
        // OWNERSHIP / KEEPER INFORMATION
        // ---------------------------------------------------------

        if (!containsOwnerInformation(lowerText)) {
            missing.add(
                "The number of previous owners or keepers."
            )
        }

        // ---------------------------------------------------------
        // V5C
        // ---------------------------------------------------------

        if (!containsV5Information(lowerText)) {
            missing.add(
                "Confirmation that the V5C logbook is present."
            )
        }

        // ---------------------------------------------------------
        // KEYS
        // ---------------------------------------------------------

        if (!containsKeyInformation(lowerText)) {
            missing.add(
                "Information regarding the number of keys supplied."
            )
        }

        // ---------------------------------------------------------
        // TYRES
        // ---------------------------------------------------------

        if (!containsTyreInformation(lowerText)) {
            missing.add(
                "Information regarding tyre condition."
            )
        }

        // ---------------------------------------------------------
        // VEHICLE LOCATION
        // ---------------------------------------------------------

        if (!containsLocationInformation(lowerText)) {
            missing.add(
                "The location of the vehicle for viewing."
            )
        }

        // ---------------------------------------------------------
        // SELLER CONTACT
        // ---------------------------------------------------------

        if (!containsContactInformation(lowerText)) {
            missing.add(
                "Seller contact information."
            )
        }

        // ---------------------------------------------------------
        // CONDITION / FAULT DISCLOSURE
        //
        // We do not require a seller to use a particular phrase.
        // Instead, look for evidence that mechanical condition has
        // actually been addressed.
        // ---------------------------------------------------------

        if (!containsConditionInformation(lowerText)) {
            missing.add(
                "Information about the vehicle's current mechanical and physical condition."
            )
        }

        // ---------------------------------------------------------
        // ACCIDENT / DAMAGE HISTORY
        // ---------------------------------------------------------

        if (!containsDamageHistoryInformation(lowerText)) {
            missing.add(
                "Information regarding previous accident, damage or repair history."
            )
        }

        // ---------------------------------------------------------
        // MODIFICATIONS
        // ---------------------------------------------------------

        if (!containsModificationInformation(lowerText)) {
            missing.add(
                "Confirmation of whether the vehicle has been modified."
            )
        }

        // ---------------------------------------------------------
        // DISTINCT OUTPUT
        // ---------------------------------------------------------

        return missing.distinct()
    }

    // -------------------------------------------------------------
    // VEHICLE IDENTITY HELPERS
    // -------------------------------------------------------------

    private fun containsMake(
        text: String
    ): Boolean {

        val makes = listOf(
            "abarth",
            "alfa romeo",
            "audi",
            "bmw",
            "citroen",
            "dacia",
            "ds",
            "fiat",
            "ford",
            "honda",
            "hyundai",
            "jaguar",
            "jeep",
            "kia",
            "land rover",
            "lexus",
            "mazda",
            "mercedes",
            "mg",
            "mini",
            "mitsubishi",
            "nissan",
            "peugeot",
            "porsche",
            "renault",
            "seat",
            "skoda",
            "smart",
            "subaru",
            "suzuki",
            "tesla",
            "toyota",
            "vauxhall",
            "volkswagen",
            "volvo"
        )

        return makes.any {
            containsPhrase(
                text,
                it
            )
        }
    }

    private fun containsModel(
        text: String
    ): Boolean {

        /*
         * Model detection is deliberately broad enough to recognise
         * normal UK advert wording without assuming that every
         * numerical token is a model.
         *
         * The dedicated AdvertVehicleExtractor remains responsible
         * for the structured model value.
         */

        val modelIndicators = listOf(
            "focus",
            "fiesta",
            "mondeo",
            "puma",
            "kuga",
            "ka",
            "mustang",
            "ecosport",

            "108",
            "208",
            "2008",
            "308",
            "3008",
            "408",
            "508",
            "5008",

            "golf",
            "polo",
            "passat",
            "jetta",
            "tiguan",
            "touareg",
            "touran",
            "arteon",
            "up",

            "a1",
            "a2",
            "a3",
            "a4",
            "a5",
            "a6",
            "a7",
            "a8",
            "q2",
            "q3",
            "q5",
            "q7",

            "1 series",
            "2 series",
            "3 series",
            "4 series",
            "5 series",
            "6 series",
            "7 series",

            "x1",
            "x2",
            "x3",
            "x4",
            "x5",
            "x6",

            "a-class",
            "a class",
            "b-class",
            "b class",
            "c-class",
            "c class",
            "e-class",
            "e class",
            "s-class",
            "s class",
            "gla",
            "glc",
            "gle",
            "gls",

            "corsa",
            "astra",
            "insignia",
            "mokka",
            "crossland",
            "crossland x",
            "grandland",
            "grandland x",

            "qashqai",
            "juke",
            "micra",
            "x-trail",
            "x trail",
            "note",

            "aygo",
            "auris",
            "corolla",
            "yaris",
            "c-hr",
            "c hr",
            "rav4",
            "prius",

            "jazz",
            "civic",
            "accord",
            "cr-v",
            "cr v",

            "clio",
            "megane",
            "captur",
            "kadjar",
            "scenic",

            "i10",
            "i20",
            "i30",
            "tucson",
            "kona",

            "picanto",
            "rio",
            "ceed",
            "sportage",
            "sorento",

            "fabia",
            "octavia",
            "superb",
            "kodiaq",
            "karoq",

            "ibiza",
            "leon",
            "ateca",
            "arona",

            "mazda2",
            "mazda3",
            "mazda6",
            "cx-3",
            "cx-5",

            "c30",
            "s40",
            "s60",
            "s90",
            "v40",
            "v60",
            "v90",
            "xc40",
            "xc60",
            "xc90"
        )

        val safeIndicators =
            modelIndicators.filterNot {
                it == "up"
            }

        if (safeIndicators.any {
                containsPhrase(text, it)
            }) {
            return true
        }

        /*
         * "up" is a genuine Volkswagen model, but it is also an
         * extremely common ordinary advert word (for example
         * "pick up from"). Only accept it when it appears with
         * Volkswagen context.
         */
        return containsPhrase(text, "up!") ||
                Regex(
                    """\b(?:volkswagen|vw)\s+up\b|\bup\s+(?:volkswagen|vw)\b""",
                    RegexOption.IGNORE_CASE
                ).containsMatchIn(text)
    }

    // -------------------------------------------------------------
    // REGISTRATION
    // -------------------------------------------------------------

    private fun containsRegistration(
        text: String
    ): Boolean {

        /*
         * Registration presence is deliberately stricter than the
         * registration extractor's private/dateless fallback.
         *
         * This detector is answering:
         *
         *     "Did the seller actually provide a recognisable VRM?"
         *
         * It must NOT answer yes merely because an advert contains
         * ordinary specification text such as:
         *
         *     1.6 FSI
         *     2.0 TDI
         *     2008 VW
         *
         * Strong UK plate formats are therefore used here.
         */
        val labelledRegistration =
            Regex(
                """\b(?:registration|reg|vrm|number\s*plate|number\s*plates)\s*[:\-]?\s*([A-Z0-9]{1,4}(?:\s+[A-Z0-9]{1,4})?)\b""",
                RegexOption.IGNORE_CASE
            )

        val labelledMatch =
            labelledRegistration.find(text)

        if (labelledMatch != null) {
            val candidate =
                labelledMatch.groupValues
                    .getOrNull(1)
                    ?.replace(Regex("\\s+"), "")
                    ?.uppercase()

            if (
                !candidate.isNullOrBlank() &&
                candidate.any { it.isLetter() } &&
                candidate.any { it.isDigit() }
            ) {
                return true
            }
        }

        val currentStyleRegistration =
            Regex(
                """\b[A-Z]{2}[0-9]{2}\s?[A-Z]{3}\b""",
                RegexOption.IGNORE_CASE
            )

        val prefixStyleRegistration =
            Regex(
                """\b[A-Z][0-9]{1,3}\s?[A-Z]{3}\b""",
                RegexOption.IGNORE_CASE
            )

        val suffixStyleRegistration =
            Regex(
                """\b[A-Z]{3}\s?[0-9]{1,3}[A-Z]\b""",
                RegexOption.IGNORE_CASE
            )

        return labelledRegistration.containsMatchIn(text) ||
                currentStyleRegistration.containsMatchIn(text) ||
                prefixStyleRegistration.containsMatchIn(text) ||
                suffixStyleRegistration.containsMatchIn(text)
    }

    // -------------------------------------------------------------
    // MOT
    // -------------------------------------------------------------

    private fun containsMotInformation(
        text: String
    ): Boolean {

        val motInformationPatterns = listOf(

            // Explicit expiry/status wording.
            Regex(
                """\bmot\s+(?:until|expires?|expiry|to|till)\b""",
                RegexOption.IGNORE_CASE
            ),

            // Relative duration, e.g. "12 months MOT" or
            // "6 months' MOT".
            Regex(
                """\b\d{1,2}\s+months?['’]?\s+mot\b""",
                RegexOption.IGNORE_CASE
            ),

            // Year stated directly after MOT.
            Regex(
                """\bmot\s+(?:20[2-9]\d|203\d)\b""",
                RegexOption.IGNORE_CASE
            ),

            // Common numeric date formats after MOT wording.
            Regex(
                """\bmot(?:\s+until|\s+expires?|\s+expiry)?\s*[:\-]?\s*\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\b""",
                RegexOption.IGNORE_CASE
            ),

            // Month/year wording, e.g. "MOT until March 2027".
            Regex(
                """\bmot(?:\s+until|\s+expires?|\s+expiry)?\s*[:\-]?\s*(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+20[2-9]\d\b""",
                RegexOption.IGNORE_CASE
            ),

            // Common listing wording such as "MOT'd until 2027".
            Regex(
                """\bmot(?:'d|’d)\s+(?:until|to|till)?\s*(?:20[2-9]\d|(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+20[2-9]\d)\b""",
                RegexOption.IGNORE_CASE
            )
        )

        return motInformationPatterns.any {
            it.containsMatchIn(text)
        }
    }

    // -------------------------------------------------------------
    // SERVICE HISTORY
    // -------------------------------------------------------------

    private fun containsServiceHistory(
        text: String
    ): Boolean {

        val servicePhrases = listOf(
            "service history",
            "service record",
            "service records",
            "full service",
            "full service history",
            "partial service",
            "service book",
            "service booklet",
            "service stamps",
            "stamped service book",
            "main dealer history",
            "main dealer service",
            "dealer service history",
            "dealer history",
            "recently serviced",
            "just serviced",
            "freshly serviced",
            "serviced",
            "fsh",
            "fs h",
            "full dealer history"
        )

        return servicePhrases.any {
            containsPhrase(text, it)
        }
    }

    // -------------------------------------------------------------
    // OWNERSHIP
    // -------------------------------------------------------------

    private fun containsOwnerInformation(
        text: String
    ): Boolean {

        val ownerPatterns = listOf(
            "one owner",
            "1 owner",
            "one previous owner",
            "1 previous owner",
            "two owners",
            "2 owners",
            "two previous owners",
            "2 previous owners",
            "three owners",
            "3 owners",
            "three previous owners",
            "3 previous owners",
            "four owners",
            "4 owners",
            "previous owner",
            "previous owners",
            "previous keeper",
            "previous keepers",
            "owner from new",
            "keeper from new",
            "keepers from new",
            "number of owners",
            "number of keepers",
            "owner history",
            "keeper history"
        )

        if (ownerPatterns.any { containsPhrase(text, it) }) {
            return true
        }

        // Also recognise compact numeric keeper statements such as
        // "2 keepers" / "3 previous keepers".
        return Regex(
            """\b\d{1,2}\s+(?:previous\s+)?keepers?\b""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(text)
    }

    // -------------------------------------------------------------
    // V5C
    // -------------------------------------------------------------

    private fun containsV5Information(
        text: String
    ): Boolean {

        return containsPhrase(text, "v5c") ||
                containsPhrase(text, "v5") ||
                containsPhrase(text, "v5 logbook") ||
                containsPhrase(text, "logbook") ||
                containsPhrase(text, "registration document") ||
                containsPhrase(text, "v5 document") ||
                containsPhrase(text, "log book")
    }

    // -------------------------------------------------------------
    // KEYS
    // -------------------------------------------------------------

    private fun containsKeyInformation(
        text: String
    ): Boolean {

        return containsPhrase(
            text,
            "key"
        ) ||
                containsPhrase(
                    text,
                    "keys"
                ) ||
                containsPhrase(
                    text,
                    "spare key"
                ) ||
                containsPhrase(
                    text,
                    "spare keys"
                ) ||
                containsPhrase(
                    text,
                    "two keys"
                ) ||
                containsPhrase(
                    text,
                    "2 keys"
                ) ||
                containsPhrase(
                    text,
                    "both keys"
                )
    }

    // -------------------------------------------------------------
    // TYRES
    // -------------------------------------------------------------

    private fun containsTyreInformation(
        text: String
    ): Boolean {

        return containsPhrase(
            text,
            "tyre"
        ) ||
                containsPhrase(
                    text,
                    "tyres"
                ) ||
                containsPhrase(
                    text,
                    "tire"
                ) ||
                containsPhrase(
                    text,
                    "tires"
                ) ||
                containsPhrase(
                    text,
                    "new tyres"
                ) ||
                containsPhrase(
                    text,
                    "new tires"
                ) ||
                containsPhrase(
                    text,
                    "tyre condition"
                ) ||
                containsPhrase(
                    text,
                    "tread"
                )
    }

    // -------------------------------------------------------------
    // LOCATION
    // -------------------------------------------------------------

    private fun containsLocationInformation(
        text: String
    ): Boolean {

        return containsPhrase(
            text,
            "location"
        ) ||
                containsPhrase(
                    text,
                    "located"
                ) ||
                containsPhrase(
                    text,
                    "based in"
                ) ||
                containsPhrase(
                    text,
                    "based at"
                ) ||
                containsPhrase(
                    text,
                    "viewing in"
                ) ||
                containsPhrase(
                    text,
                    "viewing at"
                ) ||
                containsPhrase(
                    text,
                    "collection from"
                ) ||
                containsPhrase(
                    text,
                    "collect from"
                ) ||
                containsPhrase(
                    text,
                    "pickup from"
                ) ||
                containsPhrase(
                    text,
                    "pick up from"
                )
    }

    // -------------------------------------------------------------
    // CONDITION
    // -------------------------------------------------------------

    private fun containsConditionInformation(
        text: String
    ): Boolean {

        val conditionPhrases = listOf(
            "good condition",
            "excellent condition",
            "mint condition",
            "immaculate condition",
            "immaculate",
            "clean condition",
            "very clean",
            "mechanically sound",
            "mechanically good",
            "mechanically excellent",
            "no faults",
            "no fault",
            "no issues",
            "no problems",
            "no known issues",
            "no known faults",
            "runs perfectly",
            "drives perfectly",
            "runs well",
            "drives well",
            "engine is",
            "gearbox is",
            "clutch is",
            "brakes are",
            "suspension is",
            "mechanical condition",
            "bodywork condition",
            "paintwork condition",
            "bodywork is",
            "paintwork is",
            "rust free",
            "no rust",
            "no dents",
            "no scratches",
            "minor scratches",
            "minor dents",
            "wear and tear",
            "needs work",
            "needs attention",
            "faultless"
        )

        return conditionPhrases.any {
            containsPhrase(text, it)
        }
    }

    // -------------------------------------------------------------
    // DAMAGE / ACCIDENT HISTORY
    // -------------------------------------------------------------

    private fun containsDamageHistoryInformation(
        text: String
    ): Boolean {

        val damagePhrases = listOf(
            "accident",
            "accident damage",
            "previous accident",
            "accident history",
            "damage history",
            "damaged",
            "written off",
            "write off",
            "write-off",
            "cat s",
            "cat n",
            "cat c",
            "cat d",
            "cat a",
            "cat b",
            "category s",
            "category n",
            "category c",
            "category d",
            "category a",
            "category b",
            "repaired",
            "previous repair",
            "previous repairs",
            "repair history",
            "body repair",
            "body repairs",
            "never crashed",
            "never been crashed",
            "never been written off",
            "no accident history",
            "no previous accidents",
            "accident free"
        )

        return damagePhrases.any {
            containsPhrase(text, it)
        }
    }

    // -------------------------------------------------------------
    // MODIFICATIONS
    // -------------------------------------------------------------

    private fun containsModificationInformation(
        text: String
    ): Boolean {

        val modificationPhrases = listOf(
            "modified",
            "modification",
            "modifications",
            "remapped",
            "remap",
            "stage 1",
            "stage 2",
            "tuned",
            "tuning",
            "aftermarket",
            "aftermarket parts",
            "upgraded",
            "upgrade",
            "lowered",
            "lowering springs",
            "coilovers",
            "sports exhaust",
            "performance exhaust",
            "modified engine",
            "standard specification",
            "standard spec",
            "unmodified"
        )

        return modificationPhrases.any {
            containsPhrase(
                text,
                it
            )
        }
    }

    // -------------------------------------------------------------
    // CONTACT INFORMATION
    // -------------------------------------------------------------

    private fun containsContactInformation(
        text: String
    ): Boolean {

        val phonePatterns = listOf(

            Regex(
                """(?:\+44\s?7\d{3}|\b07\d{3})\s?\d{3}\s?\d{3}\b"""
            ),

            Regex(
                """\b(?:01|02)\d{2,4}\s?\d{3}\s?\d{3,4}\b"""
            )
        )

        val emailPattern =
            Regex(
                """\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b""",
                RegexOption.IGNORE_CASE
            )

        return phonePatterns.any {
            it.containsMatchIn(text)
        } ||
                emailPattern.containsMatchIn(text)
    }

    // -------------------------------------------------------------
    // PHRASE MATCHING
    // -------------------------------------------------------------

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
        ).containsMatchIn(text)
    }
}