package com.hiddenhistory.engine

object UniversalVehicleDictionary {

    val phraseMap = mapOf(
        // Major Maintenance / High Value Perks
        "timing belt and water pump" to "Brand New Timing Belt & Water Pump",
        "timing belt replaced" to "Timing Belt Replaced",
        "cambelt done" to "Timing Belt Replaced",
        "full dealer service history" to "Full Dealer Service History (FDSH)",
        "full service history" to "Full Service History (FSH)",
        "fsh" to "Full Service History (FSH)",
        "fhsh" to "Full Honda Service History",
        "partial service history" to "Partial Service History",
        "mot until" to "Valid MOT Test",
        "mot" to "Valid MOT Test",
        "nct until" to "Valid NCT Test (Irish)",
        "hpi clear" to "HPI Clear (No Finance/Theft)",
        "v5 present" to "Logbook Present",
        "v5" to "Logbook Present",
        "2 keys" to "Both Original Keys Included",
        "1 previous owner" to "Single Previous Owner",

        // Insurance Write-Off Categories (CRITICAL)
        "cat n" to "Insurance Category N (Non-Structural Damage)",
        "cat s" to "Insurance Category S (Structural Damage Repaired)",
        "cat c" to "Insurance Category C (Severe Structural Damage - Older)",
        "cat d" to "Insurance Category D (Minor Damage - Older)",
        "previous cat" to "Recorded Insurance Write-Off",

        // Condition & Slang
        "mint condition" to "Pristine Condition",
        "mint" to "Excellent Condition",
        "clean for its age" to "Well Preserved for Age",
        "tidy wee car" to "Well Maintained (Regional)",
        "tidy" to "Good Condition",
        "runs and drives as it should" to "Mechanically Verified (Runs Smoothly)",
        "drives like a dream" to "Mechanically Sound",
        "age related marks" to "Expected Cosmetic Wear for Age",
        "banjaxed" to "Broken / Damaged (Irish Slang)",
        "shed" to "Heavy Wear / Poor Condition",

        // Modifications & Mechanical Faults (Risk)
        "head gasket" to "Severe Engine Fault (Head Gasket)",
        "clutch slipping" to "Transmission Fault (Clutch Worn)",
        "limp mode" to "Engine Electronic Fault / DPF Issue",
        "spares or repair" to "Non-Runner / Major Repair Needed",
        "straight piped" to "Exhaust Modified (Cat Deleted)",
        "mapped" to "ECU Remapped / Tuned"
    )

    fun translatePhrase(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val found = mutableListOf<String>()

        phraseMap.forEach { (key, value) ->
            if (containsPhrase(text, key)) {
                found.add(value)
            }
        }

        return found.distinct()
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