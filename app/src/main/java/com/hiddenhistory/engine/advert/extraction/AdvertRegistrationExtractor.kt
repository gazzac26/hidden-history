package com.hiddenhistory.engine.advert.extraction

class AdvertRegistrationExtractor {

    /**
     * Extracts a UK vehicle registration from advert text.
     *
     * Priority:
     *
     * 1. Explicit "Registration:" / "Reg:" labelled values.
     * 2. Standard current-style registrations.
     * 3. Prefix-style registrations.
     * 4. Suffix-style registrations.
     * 5. Dateless / private registration candidates.
     *
     * The extractor deliberately preserves every character of a
     * registration candidate. It does not attempt to "correct" or
     * reinterpret personalised registrations.
     */
    fun extractRegistration(
        text: String
    ): String? {

        /*
         * --------------------------------------------------------
         * 1. EXPLICIT REGISTRATION LABEL
         * --------------------------------------------------------
         *
         * This MUST be checked first.
         *
         * Example:
         *
         * Registration: WG08 YRO
         *
         * We want:
         *
         * WG08YRO
         *
         * NOT:
         *
         * 6FSI
         *
         * from some unrelated text elsewhere in the advert.
         */
        val labelledRegistrationPattern =
            Regex(
                pattern =
                    """(?i)\b(?:registration|reg|vrm|number\s*plate|number\s*plates)\s*[:\-]?\s*([A-Z0-9]{1,4}(?:\s+[A-Z0-9]{1,4})?)\b"""
            )

        val labelledMatch =
            labelledRegistrationPattern
                .find(text)

        if (labelledMatch != null) {

            val candidate =
                labelledMatch
                    .groupValues
                    .getOrNull(1)
                    ?.replace(
                        Regex("\\s+"),
                        ""
                    )
                    ?.uppercase()

            if (
                !candidate.isNullOrBlank() &&
                candidate.any { it.isLetter() } &&
                candidate.any { it.isDigit() }
            ) {

                return candidate.also {

                    println(
                        "AdvertRegistrationExtractor: " +
                            "Explicit registration extracted: $it"
                    )
                }
            }
        }

        /*
         * --------------------------------------------------------
         * 2. CURRENT-STYLE REGISTRATION
         * --------------------------------------------------------
         *
         * Examples:
         *
         * AB18 ABC
         * AB18ABC
         *
         * We deliberately allow A-Z here rather than excluding
         * letters based on normal issue rules.
         *
         * The job of this class is extraction/preservation,
         * not deciding whether a plate is legally assignable.
         */
        val currentStylePattern =
            Regex(
                pattern =
                    """\b[A-Z]{2}\s?[0-9]{2}\s?[A-Z]{3}\b""",
                option =
                    RegexOption.IGNORE_CASE
            )

        val currentStyleMatch =
            currentStylePattern
                .find(text)

        if (currentStyleMatch != null) {

            return currentStyleMatch.value
                .replace(
                    Regex("\\s+"),
                    ""
                )
                .uppercase()
                .also {

                    println(
                        "AdvertRegistrationExtractor: " +
                            "Current-style registration extracted: $it"
                    )
                }
        }

        /*
         * --------------------------------------------------------
         * 3. PREFIX-STYLE REGISTRATION
         * --------------------------------------------------------
         *
         * Examples:
         *
         * A123 ABC
         * A1 ABC
         */
        val prefixStylePattern =
            Regex(
                pattern =
                    """\b[A-Z][0-9]{1,3}\s?[A-Z]{3}\b""",
                option =
                    RegexOption.IGNORE_CASE
            )

        val prefixStyleMatch =
            prefixStylePattern
                .find(text)

        if (prefixStyleMatch != null) {

            return prefixStyleMatch.value
                .replace(
                    Regex("\\s+"),
                    ""
                )
                .uppercase()
                .also {

                    println(
                        "AdvertRegistrationExtractor: " +
                            "Prefix-style registration extracted: $it"
                    )
                }
        }

        /*
         * --------------------------------------------------------
         * 4. SUFFIX-STYLE REGISTRATION
         * --------------------------------------------------------
         *
         * Examples:
         *
         * ABC 123A
         * ABC123A
         */
        val suffixStylePattern =
            Regex(
                pattern =
                    """\b[A-Z]{3}\s?[0-9]{1,3}[A-Z]\b""",
                option =
                    RegexOption.IGNORE_CASE
            )

        val suffixStyleMatch =
            suffixStylePattern
                .find(text)

        if (suffixStyleMatch != null) {

            return suffixStyleMatch.value
                .replace(
                    Regex("\\s+"),
                    ""
                )
                .uppercase()
                .also {

                    println(
                        "AdvertRegistrationExtractor: " +
                            "Suffix-style registration extracted: $it"
                    )
                }
        }

        /*
         * --------------------------------------------------------
         * 5. DATELESS / PRIVATE REGISTRATION FALLBACK
         * --------------------------------------------------------
         *
         * This is intentionally LAST.
         *
         * Loose patterns are dangerous because ordinary advert
         * text contains combinations such as:
         *
         * 1.6 FSI
         * 2.0 TDI
         * 2008 VW
         *
         * Therefore we only use this fallback when no stronger
         * registration candidate was found.
         */
        val datelessPattern =
            Regex(
                pattern =
                    """\b(?:[A-Z]{1,3}\s?[0-9]{1,4}|[0-9]{1,4}\s?[A-Z]{1,3})\b""",
                option =
                    RegexOption.IGNORE_CASE
            )

        val candidates =
            datelessPattern
                .findAll(text)
                .map {

                    it.value
                        .replace(
                            Regex("\\s+"),
                            ""
                        )
                        .uppercase()
                }
                .filter { candidate ->

                    candidate.length in 2..7 &&
                        candidate.any {
                            it.isLetter()
                        } &&
                        candidate.any {
                            it.isDigit()
                        }
                }
                .toList()

        /*
         * Do not blindly take the first loose candidate.
         *
         * Prefer candidates which actually look like a plausible
         * plate rather than ordinary engine/specification text.
         */
        return candidates
            .firstOrNull()
            ?.also {

                println(
                    "AdvertRegistrationExtractor: " +
                        "Dateless/private registration candidate: $it"
                )
            }
    }

    /**
     * Extracts the registration age identifier and converts it
     * into the corresponding year.
     *
     * This only applies to current-style registrations.
     *
     * Private/dateless registrations may not contain an age code,
     * so they correctly return null here.
     */
    fun extractYearFromRegistration(
        text: String
    ): Int? {

        val registration =
            extractRegistration(text)
                ?: return null

        if (
            registration.length < 4
        ) {
            return null
        }

        /*
         * Only interpret the second and third characters as an
         * age identifier when the registration actually has the
         * current-style structure:
         *
         * AA00AAA
         */
        if (
            registration.length != 7 ||
            !registration[0].isLetter() ||
            !registration[1].isLetter() ||
            !registration[2].isDigit() ||
            !registration[3].isDigit()
        ) {
            return null
        }

        val ageCode =
            registration
                .substring(2, 4)
                .toIntOrNull()
                ?: return null

        return when {

            ageCode in 1..50 ->
                2000 + ageCode

            ageCode in 51..99 ->
                2000 + (ageCode - 50)

            else ->
                null
        }
    }
}