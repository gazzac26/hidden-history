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

    /*
     * The score measures the quality and completeness of the
     * information contained within the advertisement.
     *
     * It does NOT represent:
     * - mechanical condition
     * - roadworthiness
     * - MOT status
     * - vehicle history
     * - seller reliability
     * - mileage accuracy
     * - ownership history
     *
     * Keep this distinction explicit because official vehicle
     * data is assessed separately by the cross-check/report engines.
     */
    val informationAssessment =
        when {
            conditionScore >= 80 ->
                "The advert contains a relatively detailed amount of vehicle and seller information."

            conditionScore >= 65 ->
                "The advert contains a useful amount of vehicle and seller information, although some statements and details require verification."

            conditionScore >= 50 ->
                "The advert contains a mixture of useful information and incomplete or unverified details."

            conditionScore >= 35 ->
                "The advert contains limited information, with several details requiring verification before purchase."

            else ->
                "The advert contains relatively limited information and should be treated as an initial seller description rather than verified vehicle evidence."
        }

    val riskAssessment =
        when {
            risks.isEmpty() ->
                "No specific wording-based risk markers were identified within the advert."

            risks.size == 1 ->
                "One wording-based point was identified for further consideration."

            risks.size <= 3 ->
                "${risks.size} wording-based points were identified for further consideration."

            else ->
                "${risks.size} wording-based points were identified and should be reviewed carefully against supporting evidence."
        }

    val claimAssessment =
        when {
            claims.isEmpty() ->
                "The advert contains few explicit seller claims requiring separate verification."

            claims.size == 1 ->
                "One explicit seller claim was identified and should be treated as unverified unless supporting evidence is provided."

            else ->
                "${claims.size} explicit seller claims were identified. These should be treated as seller statements rather than established facts unless supporting evidence is provided."
        }

    val vehicleSection =
        if (vehicleDescription.isNotBlank()) {
            "The advert describes a $vehicleDescription vehicle."
        } else {
            "The advert does not contain enough structured information to provide a detailed vehicle description."
        }

    val priceSection =
        price?.takeIf {
            it.isNotBlank()
        }?.let {
            "The advertised asking price is $it."
        }

    val mileageSection =
        mileage?.takeIf {
            it.isNotBlank()
        }?.let {
            "The advert states a mileage of $it."
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

        /*
         * Keep the index clearly tied to the advert.
         */
        append(
            "Advert information index: $conditionScore/100. "
        )

        append(informationAssessment)
        append(" ")

        append(claimAssessment)
        append(" ")

        append(riskAssessment)

        if (positiveInformation.isNotBlank()) {
            append(" ")
            append(positiveInformation)
        }

        append(" ")

        /*
         * Important separation:
         *
         * This summary describes the advertisement only.
         * Official data, MOT history and other vehicle records
         * are assessed elsewhere in the report.
         */
        append(
            "This summary describes information supplied within the advertisement and does not independently verify the seller's statements."
        )

        append(" ")

        append(
            "It should not be taken as confirmation of the vehicle's mechanical condition, roadworthiness, MOT history, mileage accuracy, ownership history, accident history, finance status, insurance history or other official vehicle records."
        )

        append(" ")

        append(
            "Where official vehicle information or inspection findings are available, those should be considered separately alongside the advert information."
        )

        append(" ")

        append(
            "Seller claims should be supported by appropriate documentation and, where relevant, confirmed through official records or physical inspection before purchase."
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
            add(it)
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
                    lowerClaim.contains("two keys") ||
                    lowerClaim.contains("both keys") ||
                    lowerClaim.contains("v5") ||
                    lowerClaim.contains("logbook") ||
                    lowerClaim.contains("one owner") ||
                    lowerClaim.contains("two owners")
        }

    return when {

        positiveClaims.isEmpty() ->
            ""

        positiveClaims.size == 1 ->
            "The advert also contains one potentially useful documentation, ownership or maintenance claim, although this remains subject to verification."

        else ->
            "The advert contains ${positiveClaims.size} potentially useful documentation, ownership or maintenance claims, although these remain subject to verification."
    }
}

}