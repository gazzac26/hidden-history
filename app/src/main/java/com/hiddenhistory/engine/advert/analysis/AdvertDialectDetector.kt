package com.hiddenhistory.engine.advert.analysis

class AdvertDialectDetector {

    fun detectDialect(lowerText: String): String {

        val text = lowerText.trim()

        if (text.isEmpty()) {
            return "Unknown"
        }

        // ---------------------------------------------------------
        // SCOTTISH REGIONAL LANGUAGE
        // ---------------------------------------------------------

        val scottishIndicators = listOf(
            "wee car",
            "wee motor",
            "wee runaround",
            "oot",
            "outwith",
            "braw",
            "aye",
            "dinnae",
            "cannae",
            "nae",
            "bonnie",
            "pure dead brilliant",
            "pure dead good"
        )

        val scottishMatches =
            scottishIndicators.count { indicator ->
                containsPhrase(text, indicator)
            }

        // ---------------------------------------------------------
        // IRISH MARKETPLACE LANGUAGE
        // ---------------------------------------------------------

        val irishIndicators = listOf(
            "nct",
            "tax book",
            "irish car",
            "irish reg",
            "irish registration"
        )

        val irishMatches =
            irishIndicators.count { indicator ->
                containsPhrase(text, indicator)
            }

        // ---------------------------------------------------------
        // NORTHERN IRISH / UK MARKETPLACE
        // ---------------------------------------------------------

        val northernIrishIndicators = listOf(
            "northern ireland",
            "northern irish",
            "ni reg",
            "ni registration"
        )

        val northernIrishMatches =
            northernIrishIndicators.count { indicator ->
                containsPhrase(text, indicator)
            }

        // ---------------------------------------------------------
        // GENERAL UK AUTOMOTIVE LANGUAGE
        //
        // These are not dialect indicators by themselves.
        // They simply establish that the advert uses UK
        // automotive terminology.
        // ---------------------------------------------------------

        val ukAutomotiveIndicators = listOf(
            "mot",
            "v5",
            "v5c",
            "hpi",
            "fsh",
            "mileage",
            "miles",
            "registration",
            "number plate",
            "logbook",
            "cambelt",
            "tax",
            "ulez"
        )

        val ukMatches =
            ukAutomotiveIndicators.count { indicator ->
                containsPhrase(text, indicator)
            }

        // ---------------------------------------------------------
        // DIALECT DECISION
        //
        // Specific regional language takes priority over generic
        // UK automotive terminology.
        // ---------------------------------------------------------

        if (scottishMatches >= 2) {
            return "Scottish Regional"
        }

        if (irishMatches >= 1) {
            return "Irish Marketplace"
        }

        if (northernIrishMatches >= 1) {
            return "Northern Irish Marketplace"
        }

        if (scottishMatches == 1) {
            return "Possible Scottish Regional"
        }

        if (ukMatches >= 2) {
            return "Standard UK Automotive English"
        }

        return "Universal / Global Marketplace"
    }

    private fun containsPhrase(
        text: String,
        phrase: String
    ): Boolean {

        val escaped = Regex.escape(phrase)

        return Regex(
            """(?<![a-z0-9])$escaped(?![a-z0-9])""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(text)
    }
}