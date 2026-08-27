package com.hiddenhistory.engine

/**
 * Complete deterministic result produced by AdvertParserEngine.
 *
 * This model represents analysis of the ADVERT itself.
 *
 * Important:
 * - Values extracted from an advert are not automatically verified facts.
 * - Seller claims remain claims until independently verified.
 * - riskFlags identify things requiring attention, not proof of wrongdoing.
 * - conditionScore represents the quality/information/risk profile of the
 *   advertisement, NOT the mechanical condition of the vehicle.
 * - Official vehicle/MOT data should remain separate and be cross-referenced
 *   by the appropriate official-data engines.
 */
data class ParsedVehicleAdvert(

    // ---------------------------------------------------------
    // ORIGINAL ADVERT
    // ---------------------------------------------------------

    /**
     * Original advert text supplied to the analysis engine.
     */
    val rawText: String,

    /**
     * Normalised individual tokens extracted from the advert.
     * Used by deterministic analysis components.
     */
    val normalizedTokens: List<String>,

    // ---------------------------------------------------------
    // VEHICLE INFORMATION EXTRACTED FROM THE ADVERT
    // ---------------------------------------------------------

    val detectedDialect: String,

    val make: String?,

    val model: String?,

    val year: Int?,

    val mileage: String?,

    val price: String?,

    val transmission: String?,

    val engineSize: String?,

    val fuelType: String?,

    // ---------------------------------------------------------
    // ADVERT QUALITY / CONDITION INDEX
    // ---------------------------------------------------------

    /**
     * Deterministic 0-100 advert condition/information index.
     *
     * This does NOT represent the mechanical condition of the vehicle.
     */
    val conditionScore: Int,

    // ---------------------------------------------------------
    // HUMAN-READABLE ANALYSIS
    // ---------------------------------------------------------

    /**
     * Professional human-readable summary of the advertisement.
     */
    val professionalSummary: String,

    /**
     * Positive or neutral information identified in the advert.
     */
    val keyInsights: List<String>,

    /**
     * Risk indicators requiring further investigation.
     *
     * These are indicators only and must not be presented as proof
     * that a seller or vehicle is fraudulent.
     */
    val riskFlags: List<String>,

    // ---------------------------------------------------------
    // RAW NORMALISED ATTRIBUTES
    // ---------------------------------------------------------

    /**
     * Additional deterministic values extracted from the advert.
     *
     * Examples:
     * - mileage_normalized
     * - price_numeric
     * - bhp
     */
    val rawExtractedAttributes: Map<String, String>,

    // ---------------------------------------------------------
    // SELLER CLAIM ANALYSIS
    // ---------------------------------------------------------

    /**
     * Statements presented by the seller as claims.
     *
     * Example:
     * "Full service history."
     * "HPI clear."
     * "One owner from new."
     */
    val claimsMadeBySeller: List<String> = emptyList(),

    /**
     * Strong, subjective or potentially significant wording
     * identified within the advertisement.
     *
     * Example:
     * "Perfect"
     * "Immaculate"
     * "No faults"
     */
    val notableWording: List<String> = emptyList(),

    // ---------------------------------------------------------
    // INFORMATION QUALITY
    // ---------------------------------------------------------

    /**
     * Important information that the advert does not provide.
     */
    val missingInformation: List<String> = emptyList(),

    /**
     * Contradictions or conflicting statements identified within
     * the advertisement itself.
     */
    val inconsistencies: List<String> = emptyList(),

    // ---------------------------------------------------------
    // BUYER GUIDANCE
    // ---------------------------------------------------------

    /**
     * Claims, statements or vehicle details that should be
     * independently verified before purchase.
     */
    val thingsWorthVerifying: List<String> = emptyList(),

    /**
     * Specific questions the buyer should consider asking
     * the seller before proceeding.
     */
    val questionsTheBuyerShouldAsk: List<String> = emptyList(),

    // ---------------------------------------------------------
    // FINAL SUMMARY
    // ---------------------------------------------------------

    /**
     * Final concise summary suitable for displaying directly
     * to the user.
     */
    val overallSummary: String = ""
)

fun ParsedVehicleAdvert.toUiFormattedString(): String = buildString {
    appendLine("Free Advert Analysis")
    appendLine("--------------------------------------------------")
    appendLine("Advert Summary")
    appendLine(professionalSummary)
    appendLine()
    appendLine("Advert Information")
    appendLine("• Advert Health Index: $conditionScore%")
    appendLine("• Detected Dialect: $detectedDialect")
    appendLine("• Advert Year: ${year ?: "Unknown"}")
    appendLine("• Advert Mileage: ${mileage ?: "Not Specified"}")
    appendLine("• Advert Asking Price: ${price ?: "Not Specified"}")
    appendLine("• Advert Transmission: ${transmission ?: "Unknown"}")
    appendLine("• Advert Engine & Fuel: ${engineSize ?: "Unknown"} ${fuelType?.let { "($it)" } ?: ""}")
    
    if (riskFlags.isNotEmpty()) {
        appendLine()
        appendLine("⚠️ Advert Risk Warnings")
        riskFlags.forEach { appendLine("• $it") }
    }
    
    if (keyInsights.isNotEmpty()) {
        appendLine()
        appendLine("✅ Advert Highlights")
        keyInsights.forEach { appendLine("• $it") }
    }
}
