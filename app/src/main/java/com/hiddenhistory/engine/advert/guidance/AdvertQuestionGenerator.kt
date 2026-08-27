package com.hiddenhistory.engine.advert.guidance

class AdvertQuestionGenerator {

    fun generateBuyerQuestions(
        lowerText: String,
        missingInformation: List<String>,
        mileage: String?,
        riskFlags: List<String>
    ): List<String> {

        val questions = mutableListOf<String>()

        // ---------------------------------------------------------
        // 1. VEHICLE IDENTITY
        // ---------------------------------------------------------

        if (
            missingInformation.any {
                it.contains("make", ignoreCase = true) ||
                        it.contains("model", ignoreCase = true) ||
                        it.contains("year", ignoreCase = true)
            }
        ) {
            questions.add(
                "Can you confirm the vehicle's full make, model, registration and year?"
            )
        }

        // ---------------------------------------------------------
        // 2. ENGINE / FUEL / TRANSMISSION
        // ---------------------------------------------------------

        if (
            missingInformation.any {
                it.contains("engine", ignoreCase = true) ||
                        it.contains("fuel", ignoreCase = true) ||
                        it.contains("transmission", ignoreCase = true)
            }
        ) {
            questions.add(
                "Can you confirm the engine size, fuel type and transmission?"
            )
        }

        // ---------------------------------------------------------
        // 3. MILEAGE
        // ---------------------------------------------------------

        if (mileage != null) {

            questions.add(
                "Can you provide evidence supporting the stated mileage, such as service records or previous MOT readings?"
            )

            if (
                containsPhrase(lowerText, "warranted mileage") ||
                containsPhrase(lowerText, "warranted miles") ||
                containsPhrase(lowerText, "genuine mileage") ||
                containsPhrase(lowerText, "genuine miles")
            ) {
                questions.add(
                    "What evidence supports the claim that the mileage is genuine or warranted?"
                )
            }

        } else {

            questions.add(
                "What is the vehicle's current mileage?"
            )
        }

        // ---------------------------------------------------------
        // 4. SERVICE HISTORY
        // ---------------------------------------------------------

        if (
            containsPhrase(lowerText, "full service history") ||
            containsPhrase(lowerText, "full history") ||
            containsPhrase(lowerText, "service history") ||
            containsPhrase(lowerText, "fully serviced") ||
            containsPhrase(lowerText, "serviced")
        ) {

            questions.add(
                "Can you provide the service history and supporting invoices or service records?"
            )

        } else {

            questions.add(
                "Can you provide details of the vehicle's service history and most recent service?"
            )
        }

        // ---------------------------------------------------------
        // 5. TIMING BELT / CAMBELT
        // ---------------------------------------------------------

        if (
            containsPhrase(lowerText, "timing belt") ||
            containsPhrase(lowerText, "cambelt") ||
            containsPhrase(lowerText, "cam belt") ||
            containsPhrase(lowerText, "belt changed")
        ) {

            questions.add(
                "When was the timing belt or cambelt last replaced, and is there documentation to support this?"
            )
        }

        // ---------------------------------------------------------
        // 6. HISTORY / HPI / WRITE-OFF CLAIMS
        // ---------------------------------------------------------

        if (
            containsPhrase(lowerText, "hpi clear") ||
            containsPhrase(lowerText, "hpi checked") ||
            containsPhrase(lowerText, "clean history") ||
            containsPhrase(lowerText, "clear history") ||
            containsPhrase(lowerText, "never written off") ||
            containsPhrase(lowerText, "never been written off") ||
            containsPhrase(lowerText, "not written off") ||
            containsPhrase(lowerText, "never stolen") ||
            containsPhrase(lowerText, "not stolen") ||
            containsPhrase(lowerText, "no outstanding finance")
        ) {

            questions.add(
                "Can you provide evidence supporting the vehicle's claimed history, including finance, theft and insurance/write-off status?"
            )
        }

        // ---------------------------------------------------------
        // 7. V5C / LOGBOOK
        // ---------------------------------------------------------

        if (
            containsPhrase(lowerText, "v5") ||
            containsPhrase(lowerText, "v5c") ||
            containsPhrase(lowerText, "logbook") ||
            containsPhrase(lowerText, "full logbook")
        ) {

            questions.add(
                "Can I inspect the V5C and check that its details match the vehicle and seller?"
            )

        } else {

            questions.add(
                "Is the V5C logbook present and available to inspect?"
            )
        }

        // ---------------------------------------------------------
        // 8. OWNERSHIP
        // ---------------------------------------------------------

        if (
            containsPhrase(lowerText, "one owner") ||
            containsPhrase(lowerText, "one careful owner") ||
            containsPhrase(lowerText, "single owner") ||
            containsPhrase(lowerText, "first owner")
        ) {

            questions.add(
                "Can you provide evidence supporting the claimed ownership history?"
            )
        }

        // ---------------------------------------------------------
        // 9. CONDITION CLAIMS
        // ---------------------------------------------------------

        if (
            containsPhrase(lowerText, "excellent condition") ||
            containsPhrase(lowerText, "mint condition") ||
            containsPhrase(lowerText, "immaculate") ||
            containsPhrase(lowerText, "perfect condition") ||
            containsPhrase(lowerText, "pristine") ||
            containsPhrase(lowerText, "as new")
        ) {

            questions.add(
                "Are there any faults, damage or defects that are not mentioned in the advert?"
            )

            questions.add(
                "Would you agree to an independent inspection before purchase?"
            )
        }

        // ---------------------------------------------------------
        // 10. MECHANICAL CLAIMS
        // ---------------------------------------------------------

        if (
            containsPhrase(lowerText, "no faults") ||
            containsPhrase(lowerText, "no fault") ||
            containsPhrase(lowerText, "no issues") ||
            containsPhrase(lowerText, "no known faults") ||
            containsPhrase(lowerText, "no mechanical issues") ||
            containsPhrase(lowerText, "drives perfectly") ||
            containsPhrase(lowerText, "runs perfectly") ||
            containsPhrase(lowerText, "engine is perfect") ||
            containsPhrase(lowerText, "gearbox is perfect") ||
            containsPhrase(lowerText, "clutch is excellent")
        ) {

            questions.add(
                "Are there any known mechanical, electrical or warning-light issues that are not mentioned in the advert?"
            )

            questions.add(
                "Would you be happy for the vehicle to undergo an independent mechanical inspection?"
            )
        }

        // ---------------------------------------------------------
        // 11. ACCIDENT / DAMAGE / REPAIR CLAIMS
        // ---------------------------------------------------------

        if (
            containsPhrase(lowerText, "no accident") ||
            containsPhrase(lowerText, "no accident damage") ||
            containsPhrase(lowerText, "never crashed") ||
            containsPhrase(lowerText, "no previous repairs") ||
            containsPhrase(lowerText, "original panels") ||
            containsPhrase(lowerText, "all original")
        ) {

            questions.add(
                "Can you confirm whether the vehicle has ever been involved in an accident or had any bodywork repairs?"
            )
        }

        // ---------------------------------------------------------
        // 12. MODIFICATIONS
        // ---------------------------------------------------------

        if (
            containsPhrase(lowerText, "modified") ||
            containsPhrase(lowerText, "modification") ||
            containsPhrase(lowerText, "remapped") ||
            containsPhrase(lowerText, "remap") ||
            containsPhrase(lowerText, "lowered") ||
            containsPhrase(lowerText, "upgraded")
        ) {

            questions.add(
                "Can you provide details and documentation for any modifications or aftermarket work carried out on the vehicle?"
            )
        }

        // ---------------------------------------------------------
        // 13. MOT
        // ---------------------------------------------------------

        if (
            containsPhrase(lowerText, "mot") ||
            containsPhrase(lowerText, "m.o.t")
        ) {

            questions.add(
                "Can you confirm the current MOT expiry date and provide details of any recent advisories or failures?"
            )
        }

        // ---------------------------------------------------------
        // 14. KEYS
        // ---------------------------------------------------------

        if (
            containsPhrase(lowerText, "spare key") ||
            containsPhrase(lowerText, "spare keys") ||
            containsPhrase(lowerText, "two keys") ||
            containsPhrase(lowerText, "2 keys")
        ) {

            questions.add(
                "How many keys are included with the vehicle, and are both fully functional?"
            )
        } else {

            questions.add(
                "How many keys are supplied with the vehicle?"
            )
        }

        // ---------------------------------------------------------
        // 15. LOCATION / VIEWING
        // ---------------------------------------------------------

        if (
            !containsPhrase(lowerText, "viewing") &&
            !containsPhrase(lowerText, "viewings") &&
            !containsPhrase(lowerText, "location") &&
            !containsPhrase(lowerText, "located")
        ) {

            questions.add(
                "Where is the vehicle located for viewing and inspection?"
            )
        }

        // ---------------------------------------------------------
        // 16. TEST DRIVE
        // ---------------------------------------------------------

        if (
            !containsPhrase(lowerText, "test drive") &&
            !containsPhrase(lowerText, "test-drive")
        ) {

            questions.add(
                "Will I be able to test drive the vehicle before deciding whether to purchase it?"
            )
        }

        // ---------------------------------------------------------
        // 17. URGENCY / PRESSURE
        // ---------------------------------------------------------

        if (
            containsPhrase(lowerText, "quick sale") ||
            containsPhrase(lowerText, "must go") ||
            containsPhrase(lowerText, "need gone") ||
            containsPhrase(lowerText, "need it gone") ||
            containsPhrase(lowerText, "no time wasters") ||
            containsPhrase(lowerText, "first come first served")
        ) {

            questions.add(
                "What is the reason for the urgency or quick sale?"
            )
        }

        // ---------------------------------------------------------
        // 18. PAYMENT
        // ---------------------------------------------------------

        if (
            containsPhrase(lowerText, "cash only") ||
            containsPhrase(lowerText, "cash") ||
            containsPhrase(lowerText, "bank transfer") ||
            containsPhrase(lowerText, "bank transfer only")
        ) {

            questions.add(
                "What payment method will you accept, and will you provide a proper receipt or purchase agreement?"
            )
        }

        // ---------------------------------------------------------
        // 19. RISK-SPECIFIC FOLLOW-UP
        // ---------------------------------------------------------

        if (riskFlags.isNotEmpty()) {

            questions.add(
                "Can you provide documentation or evidence relating to the specific issue identified in the advert?"
            )
        }

        // ---------------------------------------------------------
        // 20. FINAL SAFETY QUESTION
        // ---------------------------------------------------------

        questions.add(
            "Would you be happy for me to verify the vehicle's details independently before purchase?"
        )

        // ---------------------------------------------------------
        // FINAL CLEAN-UP
        // ---------------------------------------------------------

        return questions
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
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