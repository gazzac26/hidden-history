package com.hiddenhistory.viewmodel

import android.util.Log
import com.hiddenhistory.engine.AdvertParserEngine
import com.hiddenhistory.engine.AdvertToVehicleMapper
import com.hiddenhistory.engine.ParsedVehicleAdvert
import com.hiddenhistory.models.Vehicle

class VehicleSearchAdvertProcessor {

    /*
     * Local deterministic advert parser.
     *
     * This is completely independent of the remote vehicle lookup.
     */
    private val advertParser =
        AdvertParserEngine()

    data class AdvertProcessResult(
        val parsed: ParsedVehicleAdvert,
        val advertVehicle: Vehicle?,
        val extractedRegistration: String?
    )

    fun processAdvert(text: String): AdvertProcessResult {

        /*
         * Run the local deterministic parser.
         */
        val parsed =
            advertParser.parse(text)

        /*
         * Map advert information into the existing Vehicle model.
         */
        val advertVehicle =
            runCatching {
                AdvertToVehicleMapper.mapAdvertToVehicle(
                    parsed
                )
            }.getOrNull()

        /*
         * Extract registration independently from the advert parser.
         */
        val extractedRegistration =
            extractRegistration(text)

        if (extractedRegistration != null) {

            Log.d(
                "VehicleSearch",
                "Registration extracted from advert: $extractedRegistration"
            )
        }

        return AdvertProcessResult(
            parsed =
                parsed,

            advertVehicle =
                advertVehicle,

            extractedRegistration =
                extractedRegistration
        )
    }

    /*
     * ------------------------------------------------------------
     * REGISTRATION EXTRACTION
     * ------------------------------------------------------------
     *
     * IMPORTANT:
     *
     * Registration extraction is deliberately ordered from the
     * most reliable source to the least reliable.
     *
     * 1. Explicit registration field
     * 2. Current UK registration format
     * 3. Prefix registration
     * 4. Suffix registration
     *
     * We DO NOT run a broad dateless-registration regex across the
     * entire advert.
     *
     * Doing that creates false positives such as:
     *
     *     6 FSI
     *     2 TDI
     *     5 SPEED
     *
     * Private registrations are still supported when the advert
     * explicitly identifies them as the registration.
     */
    fun extractRegistration(
        text: String
    ): String? {

        if (text.isBlank()) {
            return null
        }

        /*
         * --------------------------------------------------------
         * 1. EXPLICIT REGISTRATION FIELD
         * --------------------------------------------------------
         *
         * Examples:
         *
         * Registration: WG08 YRO
         * Registration: WG08YRO
         * Reg: ABC 123
         * VRM: A1 ABC
         * Vehicle Registration: PRIVATE1
         *
         * This is the most important check.
         *
         * We deliberately do NOT try to validate the contents using
         * a UK registration-format regex here.
         *
         * That is what allows private plates to survive intact.
         */
        val explicitRegistrationPattern =
            Regex(
                pattern =
                    """(?im)^\s*(?:registration|reg|vrm|vehicle\s+registration|vehicle\s+reg(?:istration)?)\s*[:\-]\s*([A-Za-z0-9][A-Za-z0-9\s\-]{0,15})\s*$"""
            )

        val explicitMatch =
            explicitRegistrationPattern
                .find(text)

        if (explicitMatch != null) {

            val candidate =
                normaliseExplicitRegistration(
                    explicitMatch
                        .groupValues
                        .getOrNull(1)
                        ?: ""
                )

            if (isUsableExplicitRegistration(candidate)) {

                return candidate
                    .also {

                        Log.d(
                            "VehicleSearch",
                            "Explicit registration extracted: $it"
                        )
                    }
            }
        }

        /*
         * --------------------------------------------------------
         * 2. CURRENT-STYLE UK REGISTRATION
         * --------------------------------------------------------
         *
         * Examples:
         *
         * WG08 YRO
         * WG08YRO
         *
         * Format:
         *
         * 2 letters
         * 2 numbers
         * 3 letters
         *
         * I, O and Q are excluded from normal UK registration
         * positions.
         */
        val currentStylePattern =
            Regex(
                pattern =
                    """(?<![A-Za-z0-9])[A-HJ-NPR-Z]{2}\s?[0-9]{2}\s?[A-HJ-NPR-Z]{3}(?![A-Za-z0-9])""",
                option =
                    RegexOption.IGNORE_CASE
            )

        val currentStyleMatch =
            currentStylePattern
                .find(text)

        if (currentStyleMatch != null) {

            return normaliseRegistration(
                currentStyleMatch.value
            ).also {

                Log.d(
                    "VehicleSearch",
                    "Current-style registration extracted: $it"
                )
            }
        }

        /*
         * --------------------------------------------------------
         * 3. PREFIX-STYLE UK REGISTRATION
         * --------------------------------------------------------
         *
         * Examples:
         *
         * A123 ABC
         * A1 ABC
         *
         * Format:
         *
         * 1 letter
         * 1-3 numbers
         * 3 letters
         */
        val prefixStylePattern =
            Regex(
                pattern =
                    """(?<![A-Za-z0-9])[A-HJ-NPR-Z][0-9]{1,3}\s?[A-HJ-NPR-Z]{3}(?![A-Za-z0-9])""",
                option =
                    RegexOption.IGNORE_CASE
            )

        val prefixStyleMatch =
            prefixStylePattern
                .find(text)

        if (prefixStyleMatch != null) {

            return normaliseRegistration(
                prefixStyleMatch.value
            ).also {

                Log.d(
                    "VehicleSearch",
                    "Prefix-style registration extracted: $it"
                )
            }
        }

        /*
         * --------------------------------------------------------
         * 4. SUFFIX-STYLE UK REGISTRATION
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
                    """(?<![A-Za-z0-9])[A-HJ-NPR-Z]{3}\s?[0-9]{1,3}[A-HJ-NPR-Z](?![A-Za-z0-9])""",
                option =
                    RegexOption.IGNORE_CASE
            )

        val suffixStyleMatch =
            suffixStylePattern
                .find(text)

        if (suffixStyleMatch != null) {

            return normaliseRegistration(
                suffixStyleMatch.value
            ).also {

                Log.d(
                    "VehicleSearch",
                    "Suffix-style registration extracted: $it"
                )
            }
        }

        /*
         * --------------------------------------------------------
         * NO REGISTRATION FOUND
         * --------------------------------------------------------
         *
         * Deliberately return null rather than guessing.
         */
        return null
    }

    /*
     * ------------------------------------------------------------
     * NORMALISE STANDARD REGISTRATION
     * ------------------------------------------------------------
     */
    private fun normaliseRegistration(
        value: String
    ): String {

        return value
            .filter {
                it.isLetterOrDigit()
            }
            .uppercase()
    }

    /*
     * ------------------------------------------------------------
     * NORMALISE EXPLICIT REGISTRATION
     * ------------------------------------------------------------
     *
     * For an explicitly labelled registration we preserve every
     * alphanumeric character.
     *
     * Spaces and hyphens are formatting only.
     *
     * Example:
     *
     *     WG08 YRO -> WG08YRO
     *     ABC 123 -> ABC123
     *     PRIVATE 1 -> PRIVATE1
     */
    private fun normaliseExplicitRegistration(
        value: String
    ): String {

        return value
            .filter {
                it.isLetterOrDigit()
            }
            .uppercase()
    }

    /*
     * ------------------------------------------------------------
     * EXPLICIT REGISTRATION VALIDATION
     * ------------------------------------------------------------
     *
     * We intentionally keep this permissive.
     *
     * The whole purpose of this branch is to support registrations
     * that do not conform to the normal modern UK pattern.
     *
     * We only reject obviously unusable values.
     */
    private fun isUsableExplicitRegistration(
        value: String
    ): Boolean {

        if (value.isBlank()) {
            return false
        }

        /*
         * Registration marks consist of letters and/or numbers.
         *
         * We require at least one alphanumeric character.
         */
        if (!value.any { it.isLetterOrDigit() }) {
            return false
        }

        /*
         * Prevent an entire sentence accidentally being captured.
         */
        if (value.length > 16) {
            return false
        }

        return true
    }
}