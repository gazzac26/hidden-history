package com.hiddenhistory.engine

import com.hiddenhistory.engine.advert.analysis.AdvertClaimDetector
import com.hiddenhistory.engine.advert.analysis.AdvertDialectDetector
import com.hiddenhistory.engine.advert.analysis.AdvertInconsistencyDetector
import com.hiddenhistory.engine.advert.analysis.AdvertMissingInfoDetector
import com.hiddenhistory.engine.advert.analysis.AdvertRiskDetector
import com.hiddenhistory.engine.advert.extraction.AdvertBasicExtractor
import com.hiddenhistory.engine.advert.extraction.AdvertRegistrationExtractor
import com.hiddenhistory.engine.advert.extraction.AdvertVehicleExtractor
import com.hiddenhistory.engine.advert.guidance.AdvertQuestionGenerator
import com.hiddenhistory.engine.advert.guidance.AdvertVerificationGenerator
import com.hiddenhistory.engine.advert.scoring.AdvertConditionScorer
import com.hiddenhistory.engine.advert.scoring.AdvertSummaryGenerator
import java.util.Locale

/**
 * Main deterministic advert-analysis pipeline.
 *
 * This class is deliberately responsible for orchestration only.
 *
 * It:
 *  - cleans and normalises advert text
 *  - extracts vehicle facts
 *  - identifies seller claims
 *  - analyses wording and dialect
 *  - detects risk indicators
 *  - identifies missing information
 *  - detects internal inconsistencies
 *  - generates verification requirements
 *  - generates buyer questions
 *  - calculates the advert information/condition score
 *  - generates the professional human-readable summary
 *
 * Individual intelligence rules remain isolated inside their
 * respective extractor, detector, generator and scoring classes.
 *
 * Official vehicle/MOT cross-checking remains a separate layer and
 * must not be duplicated here.
 */
class AdvertParserEngine {

    private val basicExtractor =
        AdvertBasicExtractor()

    private val registrationExtractor =
        AdvertRegistrationExtractor()

    private val vehicleExtractor =
        AdvertVehicleExtractor()

    private val dialectDetector =
        AdvertDialectDetector()

    private val claimDetector =
        AdvertClaimDetector()

    private val riskDetector =
        AdvertRiskDetector()

    private val missingInfoDetector =
        AdvertMissingInfoDetector()

    private val inconsistencyDetector =
        AdvertInconsistencyDetector()

    private val verificationGenerator =
        AdvertVerificationGenerator()

    private val questionGenerator =
        AdvertQuestionGenerator()

    private val conditionScorer =
        AdvertConditionScorer()

    private val summaryGenerator =
        AdvertSummaryGenerator()

    /**
     * Analyse a complete vehicle advertisement.
     *
     * The method is deterministic:
     * the same input produces the same ParsedVehicleAdvert.
     */
    fun parse(rawText: String): ParsedVehicleAdvert {

        val cleanText =
            rawText
                .replace("\u0000", "")
                .trim()

        if (cleanText.isBlank()) {
            return emptyResult()
        }

        val lowerText =
            cleanText.lowercase(Locale.ROOT)

        val normalizedTokens =
            normaliseTokens(cleanText)

        // ---------------------------------------------------------
        // 1. HARD VEHICLE FACT EXTRACTION
        // ---------------------------------------------------------

        val make =
            vehicleExtractor.extractMake(cleanText)

        val model =
            vehicleExtractor.extractModel(cleanText)

        val year =
            basicExtractor.extractYear(
                text = cleanText,
                registrationExtractor = registrationExtractor
            )

        val mileage =
            basicExtractor.extractMileage(cleanText)

        val price =
            basicExtractor.extractPrice(cleanText)

        val rawEngineSize =
            vehicleExtractor.extractEngineSize(cleanText)

        val engineSize =
            normaliseEngineSize(rawEngineSize)

        val fuelType =
            vehicleExtractor.extractFuelType(lowerText)

        val transmission =
            vehicleExtractor.extractTransmission(lowerText)

        val bhp =
            basicExtractor.extractBhp(cleanText)

        // ---------------------------------------------------------
        // 2. LANGUAGE / DIALECT ANALYSIS
        // ---------------------------------------------------------

        val detectedDialect =
            dialectDetector.detectDialect(lowerText)

        // ---------------------------------------------------------
        // 3. SELLER CLAIM ANALYSIS
        // ---------------------------------------------------------

        val claims =
            uniqueNonBlank(
                claimDetector.detectClaims(
                    lowerText = lowerText,
                    mileage = mileage,
                    price = price
                )
            )

        // ---------------------------------------------------------
        // 4. NOTABLE / PERSUASIVE WORDING
        // ---------------------------------------------------------

        val notableWording =
            uniqueNonBlank(
                claimDetector.detectNotableWording(lowerText)
            )

        // ---------------------------------------------------------
        // 5. RISK ANALYSIS
        // ---------------------------------------------------------

        val riskFlags =
            uniqueNonBlank(
                riskDetector.detectRiskFlags(cleanText)
            )

        val keyInsights =
            uniqueNonBlank(
                riskDetector.generateKeyInsights(
                    cleanText = cleanText,
                    riskFlags = riskFlags
                )
            )

        // ---------------------------------------------------------
        // 6. MISSING INFORMATION
        // ---------------------------------------------------------

        val missingInformation =
            uniqueNonBlank(
                missingInfoDetector.identifyMissingInformation(
                    lowerText = lowerText,
                    year = year,
                    mileage = mileage,
                    price = price,
                    fuelType = fuelType,
                    transmission = transmission,
                    engineSize = engineSize
                )
            )

        // ---------------------------------------------------------
        // 7. INTERNAL ADVERT INCONSISTENCIES
        // ---------------------------------------------------------

        val inconsistencies =
            uniqueNonBlank(
                inconsistencyDetector.identifyInconsistencies(
                    cleanText = cleanText,
                    lowerText = lowerText
                )
            )

        // ---------------------------------------------------------
        // 8. BUYER VERIFICATION REQUIREMENTS
        // ---------------------------------------------------------

        val thingsWorthVerifying =
            uniqueNonBlank(
                verificationGenerator.generateVerificationChecks(
                    lowerText = lowerText,
                    mileage = mileage,
                    riskFlags = riskFlags
                )
            )

        // ---------------------------------------------------------
        // 9. QUESTIONS FOR THE SELLER
        // ---------------------------------------------------------

        val buyerQuestions =
            uniqueNonBlank(
                questionGenerator.generateBuyerQuestions(
                    lowerText = lowerText,
                    missingInformation = missingInformation,
                    mileage = mileage,
                    riskFlags = riskFlags
                )
            )

        // ---------------------------------------------------------
        // 10. DETERMINISTIC ADVERT SCORE
        // ---------------------------------------------------------

        val conditionScore =
            conditionScorer.calculateConditionScore(
                lowerText = lowerText,
                riskFlags = riskFlags,
                missingInformation = missingInformation,
                claims = claims
            ).coerceIn(0, 100)

        // ---------------------------------------------------------
        // 11. PROFESSIONAL HUMAN-READABLE SUMMARY
        // ---------------------------------------------------------

        val generatedSummary =
            summaryGenerator.generateProfessionalSummary(
                year = year,
                engineSize = engineSize,
                fuel = fuelType,
                bhp = bhp,
                transmission = transmission,
                mileage = mileage,
                price = price,
                conditionScore = conditionScore,
                claims = claims,
                risks = riskFlags
            )

        /*
         * AdvertSummaryGenerator works from the specification values
         * supplied to it. Make/model are extracted here as hard advert
         * facts, so they must not be silently lost from the final
         * human-readable advert report.
         *
         * This does NOT replace official vehicle identity.
         * Official DVLA data remains the source of truth during the
         * later official cross-check/merge stage.
         */
        val summary =
            addAdvertIdentityToSummary(
                summary = generatedSummary,
                make = make,
                model = model
            )

        // ---------------------------------------------------------
        // 12. NORMALISED MACHINE-READABLE ATTRIBUTES
        // ---------------------------------------------------------

        val extraAttributes =
            linkedMapOf<String, String>()

        bhp
            ?.takeIf { it.isNotBlank() }
            ?.let {
                extraAttributes["bhp"] = it
            }

        mileage
            ?.takeIf { it.isNotBlank() }
            ?.let {
                extraAttributes["mileage_normalized"] =
                    basicExtractor
                        .normalizeMileage(it)
                        .toString()
            }

        price
            ?.takeIf { it.isNotBlank() }
            ?.let {
                basicExtractor
                    .normalizePrice(it)
                    ?.let { numericPrice ->
                        extraAttributes["price_numeric"] =
                            numericPrice.toString()
                    }
            }

        // ---------------------------------------------------------
        // 13. FINAL PARSED RESULT
        // ---------------------------------------------------------

        return ParsedVehicleAdvert(

            rawText = cleanText,

            normalizedTokens = normalizedTokens,

            detectedDialect = detectedDialect,

            make = make,

            model = model,

            year = year,

            mileage = mileage,

            price = price,

            transmission = transmission,

            engineSize = engineSize,

            fuelType = fuelType,

            conditionScore = conditionScore,

            professionalSummary = summary,

            keyInsights = keyInsights,

            riskFlags = riskFlags,

            rawExtractedAttributes = extraAttributes,

            claimsMadeBySeller = claims,

            notableWording = notableWording,

            missingInformation = missingInformation,

            inconsistencies = inconsistencies,

            thingsWorthVerifying = thingsWorthVerifying,

            questionsTheBuyerShouldAsk = buyerQuestions,

            overallSummary = summary
        )
    }

    // -------------------------------------------------------------
    // NORMALISATION HELPERS
    // -------------------------------------------------------------

    private fun normaliseTokens(
        text: String
    ): List<String> {

        return text
            .split(Regex("\\s+"))
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.lowercase(Locale.ROOT) }
            .toList()
    }

    private fun normaliseEngineSize(
        rawEngineSize: String?
    ): String? {

        if (rawEngineSize.isNullOrBlank()) {
            return null
        }

        val value =
            rawEngineSize.trim()

        /*
         * AdvertVehicleExtractor already normalises engine sizes
         * such as:
         *
         *     1.6 L
         *     2.0 L
         *     1598 cc
         *
         * Do not append another "L" when the extracted value
         * already contains a litre unit.
         */
        return if (
            value.contains(
                "cc",
                ignoreCase = true
            ) ||
                    value.contains(
                        "l",
                        ignoreCase = true
                    )
        ) {
            value
        } else {
            "$value L"
        }
    }

    // -------------------------------------------------------------
    // ADVERT SUMMARY IDENTITY
    // -------------------------------------------------------------

    private fun addAdvertIdentityToSummary(
        summary: String,
        make: String?,
        model: String?
    ): String {

        if (summary.isBlank()) {
            return summary
        }

        val identityParts =
            listOfNotNull(
                make?.trim()?.takeIf {
                    it.isNotBlank()
                },
                model?.trim()?.takeIf {
                    it.isNotBlank()
                }
            )

        if (identityParts.isEmpty()) {
            return summary
        }

        val identity =
            identityParts
                .joinToString(" ")

        val marker =
            "Vehicle information:"

        val markerIndex =
            summary.indexOf(
                marker,
                ignoreCase = true
            )

        if (markerIndex < 0) {
            return summary
        }

        val valueStart =
            markerIndex + marker.length

        val remainder =
            summary.substring(valueStart)

        /*
         * If the generated summary already begins with the advert
         * identity, do not duplicate it.
         */
        val firstSentenceEnd =
            remainder.indexOf('.')

        val firstSentence =
            if (firstSentenceEnd >= 0) {
                remainder.substring(
                    0,
                    firstSentenceEnd
                )
            } else {
                remainder
            }

        if (
            firstSentence.contains(
                identity,
                ignoreCase = true
            )
        ) {
            return summary
        }

        val trimmedRemainder =
            remainder.trimStart()

        return summary.substring(
            0,
            valueStart
        ) +
                " $identity" +
                if (trimmedRemainder.isNotBlank()) {
                    " $trimmedRemainder"
                } else {
                    ""
                }
    }

    private fun uniqueNonBlank(
        values: List<String>
    ): List<String> {

        return values
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy {
                it.lowercase(Locale.ROOT)
            }
            .toList()
    }

    // -------------------------------------------------------------
    // EMPTY RESULT
    // -------------------------------------------------------------

    private fun emptyResult(): ParsedVehicleAdvert {

        return ParsedVehicleAdvert(

            rawText = "",

            normalizedTokens = emptyList(),

            detectedDialect = "Unknown",

            make = null,

            model = null,

            year = null,

            mileage = null,

            price = null,

            transmission = null,

            engineSize = null,

            fuelType = null,

            conditionScore = 0,

            professionalSummary =
                "No advert text was provided.",

            keyInsights = emptyList(),

            riskFlags = emptyList(),

            rawExtractedAttributes = emptyMap(),

            claimsMadeBySeller = emptyList(),

            notableWording = emptyList(),

            missingInformation = emptyList(),

            inconsistencies = emptyList(),

            thingsWorthVerifying = emptyList(),

            questionsTheBuyerShouldAsk = emptyList(),

            overallSummary =
                "No advert text was provided."
        )
    }
}