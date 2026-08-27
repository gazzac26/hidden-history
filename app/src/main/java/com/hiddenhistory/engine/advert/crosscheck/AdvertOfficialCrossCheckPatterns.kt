package com.hiddenhistory.engine.advert.crosscheck

import com.hiddenhistory.models.MotTest
import java.time.LocalDateTime

internal fun analyseMotDefectPatterns(
    motTests: List<MotTest>,
    advertText: String,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {
    if (motTests.isEmpty()) return

    val defects: List<DefectEvidence> = motTests
        .flatMap { test: MotTest ->
            test.defects.map { defect ->
                DefectEvidence(
                    test = test,
                    text = defect.text
                        ?.trim()
                        ?.takeIf { value -> value.isNotBlank() },
                    type = defect.type
                        ?.trim()
                        ?.uppercase(),
                    dangerous = defect.dangerous
                )
            }
        }
        .filter { evidence: DefectEvidence ->
            evidence.text != null
        }

    if (defects.isEmpty()) return

    /*
     * -------------------------------------------------------------
     * DANGEROUS DEFECTS
     * -------------------------------------------------------------
     *
     * Keep this exactly as an analytical warning.
     *
     * We still count dangerous defects because this is meaningful
     * intelligence that the user should be told about.
     *
     * Individual MOT records remain visible in the official MOT
     * history section.
     */

    val dangerousCount = defects.count { evidence: DefectEvidence ->
        evidence.dangerous
    }

    if (dangerousCount > 0) {
        warnings.add(
            "Official MOT history contains $dangerousCount dangerous defect${if (dangerousCount == 1) "" else "s"}."
        )

        verificationItems.add(
            "Review every dangerous MOT defect and confirm the associated repair work before purchase."
        )
    }

    /*
     * -------------------------------------------------------------
     * REPEATED DEFECT PATTERNS
     * -------------------------------------------------------------
     *
     * The engine continues analysing every individual MOT defect
     * internally.
     *
     * The user-facing result is deliberately aggregated into defect
     * families rather than displaying every individual occurrence.
     *
     * Example:
     *
     *     5 separate headlamp defects
     *
     * becomes:
     *
     *     "Repeated MOT defect pattern detected:
     *      headlamp/alignment appears in 5 MOT record(s)."
     *
     * We therefore retain the full analytical capability without
     * flooding the user with individual historical entries.
     */

    val families: Map<String, List<DefectEvidence>> =
        buildDefectFamilies(defects)

    families
        .filter { entry: Map.Entry<String, List<DefectEvidence>> ->
            entry.value.size >= MIN_REPEATED_DEFECT_OCCURRENCES
        }
        .forEach { entry: Map.Entry<String, List<DefectEvidence>> ->
            val family = entry.key
            val evidence = entry.value

            warnings.add(
                "Repeated MOT defect pattern detected: $family appears in ${evidence.size} MOT record(s)."
            )

            verificationItems.add(
                inspectionAdviceForDefectFamily(family)
            )
        }

    /*
     * -------------------------------------------------------------
     * ADVERT CLAIM:
     * "NO ADVISORIES"
     * -------------------------------------------------------------
     *
     * This remains a direct advert-vs-official-data comparison.
     */

    if (
        containsAny(
            advertText,
            "no advisories",
            "no advisory",
            "no advisories on the last mot"
        )
    ) {
        val latest = motTests.maxByOrNull { test: MotTest ->
            parseDateTime(test.completedDate)?.dateTime
                ?: LocalDateTime.MIN
        }

        val latestAdvisories: Int =
            latest?.defects
                ?.count { defect ->
                    defect.type
                        ?.trim()
                        ?.equals("ADVISORY", ignoreCase = true) == true
                }
                ?: 0

        if (latestAdvisories > 0) {
            warnings.add(
                "The advert claims there were no advisories on the last MOT, " +
                    "but the latest available MOT record contains " +
                    "$latestAdvisories advisory item(s)."
            )

            verificationItems.add(
                "Ask the seller to explain the discrepancy between the advert's " +
                    "'no advisories' claim and the official MOT record."
            )
        }
    }

    /*
     * -------------------------------------------------------------
     * ADVERT CLAIM:
     * "NO FAULTS"
     * -------------------------------------------------------------
     *
     * This remains unchanged.
     */

    if (
        containsAny(
            advertText,
            "no faults whatsoever",
            "no faults",
            "no known mechanical issues"
        )
    ) {
        val relevant: List<DefectEvidence> =
            defects.filter { evidence: DefectEvidence ->
                isMechanicallyRelevant(evidence.text.orEmpty())
            }

        if (relevant.isNotEmpty()) {
            warnings.add(
                "The advert makes a broad no-fault claim, but official MOT history " +
                    "contains mechanically relevant defect records."
            )

            verificationItems.add(
                "Review the mechanically relevant MOT defects and ask the seller " +
                    "what repair work was performed. Inspect the affected systems " +
                    "during the viewing/test drive."
            )
        }
    }

    /*
     * -------------------------------------------------------------
     * FAILED MOT COUNT
     * -------------------------------------------------------------
     *
     * We retain the overall count.
     *
     * We deliberately do NOT output individual failure/retest dates.
     * Those records are already available to the user through the
     * official MOT history section.
     */

    val failedTests: Int =
        motTests.count { test: MotTest ->
            isFailedTest(test.testResult)
        }

    if (failedTests > 0) {
        confirmations.add(
            "The official MOT history contains $failedTests failed MOT test" +
                "${if (failedTests == 1) "" else "s"}. " +
                "Individual failure items should be reviewed rather than treating " +
                "the latest MOT result in isolation."
        )
    }
}

/*
 * -------------------------------------------------------------
 * DEFECT FAMILY BUILDING
 * -------------------------------------------------------------
 */

private fun buildDefectFamilies(
    defects: List<DefectEvidence>
): Map<String, List<DefectEvidence>> {

    val families: MutableMap<String, MutableList<DefectEvidence>> =
        mutableMapOf()

    defects.forEach { defect: DefectEvidence ->

        val family: String? =
            classifyDefectFamily(
                defect.text.orEmpty()
            )

        if (family != null) {

            families
                .getOrPut(family) {
                    mutableListOf<DefectEvidence>()
                }
                .add(defect)
        }
    }

    return families
}

/*
 * -------------------------------------------------------------
 * DEFECT FAMILY CLASSIFICATION
 * -------------------------------------------------------------
 */

private fun classifyDefectFamily(
    text: String
): String? {

    val lower = text.lowercase()

    return when {

        containsAny(
            lower,
            "headlamp",
            "headlamp aim",
            "headlight",
            "headlight aim"
        ) ->
            "headlamp/alignment"

        containsAny(
            lower,
            "suspension",
            "wishbone",
            "ball joint",
            "bush",
            "damper",
            "shock absorber",
            "spring"
        ) ->
            "suspension"

        containsAny(
            lower,
            "oil leak",
            "engine oil leak",
            "oil leakage",
            "fluid leak"
        ) ->
            "oil/fluid leakage"

        containsAny(
            lower,
            "brake",
            "braking",
            "brake disc",
            "brake pad",
            "brake efficiency"
        ) ->
            "braking system"

        containsAny(
            lower,
            "steering",
            "track rod",
            "tie rod",
            "steering rack"
        ) ->
            "steering"

        containsAny(
            lower,
            "tyre",
            "tire",
            "uneven wear"
        ) ->
            "tyres/wheel alignment"

        containsAny(
            lower,
            "exhaust",
            "emission",
            "emissions"
        ) ->
            "exhaust/emissions"

        containsAny(
            lower,
            "engine",
            "engine mount",
            "mounting"
        ) ->
            "engine"

        containsAny(
            lower,
            "gearbox",
            "transmission",
            "clutch"
        ) ->
            "transmission/clutch"

        containsAny(
            lower,
            "body",
            "panel",
            "door",
            "bonnet",
            "boot lid",
            "wing",
            "bumper",
            "structural"
        ) ->
            "body/impact-related"

        else ->
            null
    }
}

/*
 * -------------------------------------------------------------
 * INSPECTION ADVICE
 * -------------------------------------------------------------
 */

private fun inspectionAdviceForDefectFamily(
    family: String
): String =
    when (family) {

        "headlamp/alignment" ->
            "Inspect the headlamp housings, mounting points, alignment and surrounding bodywork. Repeated alignment defects can have several causes and should not be assumed to prove collision damage."

        "suspension" ->
            "Inspect suspension components, bushes, ball joints, springs and dampers. During the test drive listen for knocks and check for uneven tyre wear or alignment issues."

        "oil/fluid leakage" ->
            "Inspect the engine bay and underside for fresh oil/fluid, staining, seepage and evidence of previous cleaning or repair. Ask the seller what work was carried out."

        "braking system" ->
            "Inspect discs, pads, calipers and braking performance. Check for vibration, pulling or unusual noises during the test drive."

        "steering" ->
            "Inspect steering components and wheel alignment. During the test drive check for play, pulling, wandering, knocking or unusual steering behaviour."

        "tyres/wheel alignment" ->
            "Inspect all tyres for uneven wear and check wheel alignment. Uneven wear can have multiple causes and should be investigated rather than attributed to one specific fault."

        "exhaust/emissions" ->
            "Inspect the exhaust/emissions system and ask for evidence of any emissions-related repairs or warning-light work."

        "engine" ->
            "Inspect the engine for leaks, unusual noise, vibration, mounting movement and evidence of previous repairs. Match any seller explanation against service invoices."

        "transmission/clutch" ->
            "During the test drive check clutch engagement, slipping, gear selection, noises and transmission behaviour."

        "body/impact-related" ->
            "Inspect body panels, panel gaps, paint finish, fasteners, lamp mounting, bumper alignment and evidence of previous repair. These findings alone do not prove an accident."

        else ->
            "Review the repeated MOT defect entries and inspect the affected system before purchase."
    }

/*
 * -------------------------------------------------------------
 * POTENTIAL IMPACT PATTERN
 * -------------------------------------------------------------
 *
 * This remains completely intact.
 */

internal fun analysePotentialImpactPattern(
    motTests: List<MotTest>,
    advertText: String,
    officialSalvageCategory: String?,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {

    val allDefectText: List<String> =
        motTests
            .flatMap { test: MotTest ->
                test.defects
            }
            .mapNotNull { defect ->
                defect.text
                    ?.trim()
                    ?.takeIf { value -> value.isNotBlank() }
            }
            .map { text ->
                text.lowercase()
            }

    if (allDefectText.isEmpty()) return

    val frontEndSignals: Int =
        allDefectText.count { text: String ->

            containsAny(
                text,
                "headlamp",
                "headlight",
                "front suspension",
                "front suspension component",
                "steering",
                "wishbone",
                "ball joint",
                "bumper",
                "wing",
                "front brake"
            )
        }

    val bodyClaim: Boolean =
        containsAny(
            advertText,
            "no accident damage",
            "never accident damaged",
            "never been in an accident",
            "no previous repairs",
            "all panels original",
            "original bodywork",
            "bodywork is original"
        )

    val salvageMarker: Boolean =
        !officialSalvageCategory.isNullOrBlank()

    if (
        frontEndSignals >= MIN_IMPACT_PATTERN_SIGNALS &&
        bodyClaim
    ) {

        warnings.add(
            "The official MOT history contains repeated front-end, steering or " +
                "suspension-related evidence while the advert makes a strong claim " +
                "that the vehicle has no accident damage or previous repair."
        )

        verificationItems.add(
            "Perform a careful physical inspection for previous impact or repair: " +
                "compare panel gaps, paint finish, bumper and lamp alignment, " +
                "mounting points, fasteners, suspension components and wheel alignment. " +
                "The MOT pattern does not by itself prove collision damage; it identifies " +
                "an area that deserves investigation."
        )
    }

    if (salvageMarker) {

        warnings.add(
            "Official vehicle data contains a salvage/write-off category marker: " +
                "$officialSalvageCategory."
        )

        verificationItems.add(
            "Verify the salvage category and obtain supporting vehicle history " +
                "before relying on any advert claim that the vehicle has never been written off."
        )
    }
}