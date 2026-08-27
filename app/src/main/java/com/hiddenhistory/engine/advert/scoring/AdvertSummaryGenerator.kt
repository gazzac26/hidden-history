package com.hiddenhistory.engine.advert.scoring

class AdvertSummaryGenerator {

    fun generateProfessionalSummary(
        year: Int?,
        engineSize: String?,
        fuel: String?,
        bhp: String?,
        transmission: String?,
        mileage: String?,
        price: String?,
        conditionScore: Int,
        claims: List<String>,
        risks: List<String>
    ): String {

        val vehicleDescription =
            buildVehicleDescription(
                year = year,
                engineSize = engineSize,
                fuel = fuel,
                bhp = bhp,
                transmission = transmission,
                mileage = mileage
            )

        val scoreAssessment =
            when {
                conditionScore >= 80 ->
                    "The advert provides a strong level of useful and reasonably transparent information."

                conditionScore >= 65 ->
                    "The advert provides a generally useful level of information, although some areas should still be verified."

                conditionScore >= 50 ->
                    "The advert provides a mixed level of information and should not be relied upon without further verification."

                conditionScore >= 35 ->
                    "The advert provides limited or concerning information and requires careful verification before purchase."

                else ->
                    "The advert provides a poor level of reliable information and warrants significant caution before purchase."
            }

        val riskAssessment =
            when {
                risks.isEmpty() ->
                    "No direct advert risk markers were identified by the advert risk analysis."

                risks.size == 1 ->
                    "One identifiable risk marker was detected and should be independently verified."

                risks.size <= 3 ->
                    "${risks.size} identifiable risk markers were detected and should be independently verified."

                else ->
                    "${risks.size} identifiable risk markers were detected, indicating that the advert requires particularly careful verification."
            }

        val claimAssessment =
            when {
                claims.isEmpty() ->
                    "The seller makes very few explicit claims that can be assessed from the advert."

                claims.size == 1 ->
                    "One explicit seller claim was identified. It should be treated as an unverified statement until supporting evidence is provided."

                else ->
                    "${claims.size} explicit seller claims were identified. These should be treated as unverified statements until supporting evidence is provided."
            }

        val vehicleSection =
            if (vehicleDescription.isNotBlank()) {
                "Vehicle information: $vehicleDescription."
            } else {
                "Vehicle information: The advert does not contain enough structured information to identify the vehicle fully."
            }

        val priceSection =
            price?.let {
                "Advertised asking price: $it."
            }

        val mileageSection =
            mileage?.let {
                "Advertised mileage: $it."
            }

        val positiveInformation =
            buildPositiveInformation(
                claims = claims
            )

        return buildString {

            append(vehicleSection)
            append(" ")

            priceSection?.let {
                append(it)
                append(" ")
            }

            mileageSection?.let {
                append(it)
                append(" ")
            }

            append("Advert information index: $conditionScore/100. ")
            append(scoreAssessment)
            append(" ")

            append(claimAssessment)
            append(" ")

            append(riskAssessment)

            if (positiveInformation.isNotBlank()) {
                append(" ")
                append(positiveInformation)
            }

            append(" ")

            append(
                "This assessment is based on the information contained in the advertisement itself. "
            )

            append(
                "It does not establish the vehicle's mechanical condition, ownership status, mileage accuracy, "
            )

            append(
                "finance status, insurance history or other official vehicle history. "
            )

            append(
                "Those matters should be verified against the appropriate official records and supporting documentation before purchase."
            )
        }.trim()
    }

    private fun buildVehicleDescription(
        year: Int?,
        engineSize: String?,
        fuel: String?,
        bhp: String?,
        transmission: String?,
        mileage: String?
    ): String {

        return buildList {

            year?.let {
                add(it.toString())
            }

            engineSize?.let {
                add(it)
            }

            fuel?.let {
                add(it)
            }

            bhp?.let {
                add("$it BHP")
            }

            transmission?.let {
                add(transmission)
            }

            mileage?.let {
                add(it)
            }

        }.joinToString(" ")
    }

    private fun buildPositiveInformation(
        claims: List<String>
    ): String {

        val positiveClaims =
            claims.filter { claim ->

                val lowerClaim =
                    claim.lowercase()

                lowerClaim.contains("service history") ||
                        lowerClaim.contains("service record") ||
                        lowerClaim.contains("2 keys") ||
                        lowerClaim.contains("both keys") ||
                        lowerClaim.contains("v5") ||
                        lowerClaim.contains("logbook") ||
                        lowerClaim.contains("one owner") ||
                        lowerClaim.contains("two owners")
            }

        return when {

            positiveClaims.isEmpty() -> ""

            positiveClaims.size == 1 ->
                "The advert also contains one potentially useful ownership, documentation or maintenance claim."

            else ->
                "The advert contains ${positiveClaims.size} potentially useful ownership, documentation or maintenance claims."
        }
    }
}