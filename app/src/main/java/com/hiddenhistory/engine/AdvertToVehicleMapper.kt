package com.hiddenhistory.engine

import com.hiddenhistory.models.Vehicle

object AdvertToVehicleMapper {

    /**
     * Maps deterministic advert-analysis results into the existing Vehicle model.
     *
     * Important:
     * This mapper does not invent vehicle facts.
     *
     * Information which cannot be reliably established from the advert
     * remains null/unknown and must be populated by the appropriate
     * official-data or vehicle-history cross-check layer.
     */
    fun mapAdvertToVehicle(
        advert: ParsedVehicleAdvert
    ): Vehicle {

        val numericMileage =
            advert.mileage
                ?.let(::extractMileageNumber)

        val numericPrice =
            advert.price
                ?.let(::extractPriceNumber)

        val engineCc =
            advert.engineSize
                ?.let(::extractEngineCapacityCc)

        val fuelType =
            advert.fuelType
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        /*
         * Salvage/write-off status is deliberately NOT inferred from
         * advert wording or risk flags.
         *
         * A seller mentioning "HPI clear", "never written off", etc.
         * is a claim, not verified vehicle-history evidence.
         *
         * The authoritative value must come from the official/
         * approved vehicle-history cross-check layer.
         */
        val salvageCategory =
            null

        return Vehicle(
            vin = null,

            registrationDate = null,

            engineCapacity = engineCc,

            fuelType = fuelType,

            price = numericPrice,

            mileage = numericMileage,

            salvageCategory = salvageCategory,

            previousKeepers = null,

            taxStatus = null,

            hasOutstandingRecall = null,

            activeSymptoms = emptyList(),

            seats = null,

            maxTowWeight = null,

            co2Emissions = null
        )
    }


    // -------------------------------------------------------------
    // NUMERIC EXTRACTION
    // -------------------------------------------------------------

    /**
     * Converts advert mileage strings such as:
     *
     * "67,500 MILES"
     * "67500 miles"
     * "67.5k"
     *
     * into an integer mileage value where safely possible.
     *
     * The method intentionally does not attempt to guess units
     * where the input is ambiguous.
     */
    private fun extractMileageNumber(
        mileage: String
    ): Int? {

        val clean =
            mileage
                .trim()
                .lowercase()

        val thousandMatch =
            Regex(
                """(\d+(?:\.\d+)?)\s*k\b"""
            ).find(clean)

        if (thousandMatch != null) {

            val value =
                thousandMatch
                    .groupValues[1]
                    .toDoubleOrNull()

            if (value != null) {
                return (value * 1000.0)
                    .toInt()
            }
        }

        val numericMatch =
            Regex(
                """\d[\d,]*(?:\.\d+)?"""
            ).find(clean)

        return numericMatch
            ?.value
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?.toInt()
    }

    /**
     * Converts a price string into a numeric GBP value.
     *
     * Examples:
     * £8,495
     * £8495
     * £8,495.00
     */
    private fun extractPriceNumber(
        price: String
    ): Double? {

        val match =
            Regex(
                """£\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""
            ).find(price)

        if (match != null) {

            return match
                .groupValues[1]
                .replace(",", "")
                .toDoubleOrNull()
        }

        return price
            .replace(",", "")
            .replace(Regex("[^0-9.]"), "")
            .toDoubleOrNull()
    }

    /**
     * Converts engine-size representations into cubic centimetres.
     *
     * Examples:
     *
     * "1.5 L"  -> 1500
     * "2.0L"   -> 2000
     * "1995cc" -> 1995
     *
     * No model-badge assumptions are made.
     */
    private fun extractEngineCapacityCc(
        engineSize: String
    ): Int? {

        val clean =
            engineSize
                .trim()
                .lowercase()

        val ccMatch =
            Regex(
                """(\d+(?:\.\d+)?)\s*cc\b"""
            ).find(clean)

        if (ccMatch != null) {

            return ccMatch
                .groupValues[1]
                .toDoubleOrNull()
                ?.toInt()
        }

        val litreMatch =
            Regex(
                """(\d+(?:\.\d+)?)\s*l\b"""
            ).find(clean)

        if (litreMatch != null) {

            val litres =
                litreMatch
                    .groupValues[1]
                    .toDoubleOrNull()

            if (litres != null) {
                return (litres * 1000.0)
                    .toInt()
            }
        }

        return null
    }

    // -------------------------------------------------------------
    // TEXT HELPERS
    // -------------------------------------------------------------

    private fun containsAny(
        values: List<String>,
        vararg terms: String
    ): Boolean {

        return terms.any { term ->
            values.any { value ->
                value.contains(
                    term,
                    ignoreCase = true
                )
            }
        }
    }
}