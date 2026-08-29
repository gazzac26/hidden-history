package com.hiddenhistory.engine.advert.crosscheck

import com.hiddenhistory.engine.ParsedVehicleAdvert

/**
 * Cross-checks seller claims against evidence that is actually
 * available to the application.
 *
 * Important:
 *
 * MOT / DVLA / DVSA evidence must not be treated as proof of:
 * - full service history
 * - HPI status
 * - outstanding finance status
 * - general vehicle provenance beyond the fields actually supplied
 *
 * A seller claim without corresponding authoritative evidence is
 * a verification requirement, NOT a warning that the seller is lying.
 */
internal fun crossCheckExternalEvidenceClaims(
    advert: ParsedVehicleAdvert,
    verificationItems: MutableList<String>
) {
    val text = advertText(advert)

    /*
     * -------------------------------------------------------------
     * SERVICE HISTORY
     * -------------------------------------------------------------
     *
     * MOT/DVLA/DVSA data does not constitute a service history.
     */
    if (
        containsAny(
            text,
            "full service history",
            "full service history available",
            "full service history present",
            "fsh",
            "full service record"
        )
    ) {
        verificationItems.add(
            "The advert claims a full service history. MOT/DVLA/DVSA data does not by itself prove a complete service history. Request the service book or digital service record together with supporting invoices and check dates and mileage."
        )
    }

    /*
     * -------------------------------------------------------------
     * HPI / VEHICLE HISTORY CLAIM
     * -------------------------------------------------------------
     *
     * Do not convert a seller's HPI statement into an official
     * confirmation.
     *
     * "Clean history" is deliberately NOT treated as an HPI claim
     * here because it is too ambiguous to establish exactly what
     * the seller means.
     */
    if (
        containsAny(
            text,
            "hpi clear",
            "hpi checked",
            "hpi clean"
        )
    ) {
        verificationItems.add(
            "The advert makes an HPI or vehicle-history claim. MOT, DVLA and DVSA data do not by themselves prove an HPI result. Request the actual history-check evidence or obtain an independent vehicle-history check."
        )
    }

    /*
     * -------------------------------------------------------------
     * FINANCE CLAIM
     * -------------------------------------------------------------
     *
     * Finance status must never be inferred from MOT/DVLA/DVSA
     * information.
     */
    if (
        containsAny(
            text,
            "no outstanding finance",
            "no finance",
            "finance clear",
            "clear finance",
            "finance free",
            "no outstanding loan"
        )
    ) {
        verificationItems.add(
            "The advert makes a finance-status claim. Finance status must not be inferred from MOT, DVLA or DVSA data unless an authoritative finance source is available. Obtain independent finance-status evidence before purchase."
        )
    }

    /*
     * -------------------------------------------------------------
     * OWNERSHIP CLAIMS
     * -------------------------------------------------------------
     *
     * Ownership/keeper claims are compared separately when an
     * official keeper count is actually available.
     */
    if (
        containsAny(
            text,
            "one owner",
            "1 owner",
            "one careful owner",
            "one owner from new",
            "two owners",
            "2 owners",
            "three owners",
            "3 owners"
        )
    ) {
        verificationItems.add(
            "The advert makes an ownership/keeper-count claim. Verify the stated number against the available official vehicle record and inspect the V5C."
        )
    }
}

/**
 * Compares an explicit advert owner/keeper count with the official
 * vehicle record when both values are available.
 *
 * No conclusion is made when either value is unavailable.
 */
internal fun crossCheckOwnerClaim(
    advertValue: Any?,
    advertText: String,
    officialOwners: Int?,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {
    val advertOwnerNumber =
        extractInteger(
            cleanText(advertValue)
        )

    /*
     * -------------------------------------------------------------
     * EXPLICIT NUMERIC VALUE
     * -------------------------------------------------------------
     */
    if (
        advertOwnerNumber != null &&
        officialOwners != null
    ) {
        if (advertOwnerNumber == officialOwners) {

            confirmations.add(
                "The advert owner/keeper count is consistent with the available official vehicle record."
            )

        } else {

            warnings.add(
                "The advert owner/keeper count of $advertOwnerNumber does not match the available official vehicle record of $officialOwners."
            )

            verificationItems.add(
                "Ask the seller to explain the ownership/keeper difference and verify the V5C and available vehicle history."
            )
        }

        /*
         * A numeric comparison has already been performed.
         *
         * Do not run a second textual one-owner contradiction below.
         */
        return
    }

    /*
     * -------------------------------------------------------------
     * TEXTUAL ONE-OWNER CLAIM
     * -------------------------------------------------------------
     *
     * Only flag this when the official record actually gives us
     * a comparable number.
     */
    if (
        containsAny(
            advertText,
            "one owner",
            "1 owner",
            "one careful owner",
            "one owner from new"
        ) &&
        officialOwners != null &&
        officialOwners > 1
    ) {
        warnings.add(
            "The advert claims one owner, but the available official vehicle record reports $officialOwners previous owner/keeper record(s)."
        )

        verificationItems.add(
            "Ask the seller to explain the ownership history and verify the V5C and keeper history."
        )
    }
}

/**
 * Cross-checks advert write-off/provenance claims against an
 * officially supplied salvage category.
 *
 * IMPORTANT:
 *
 * null / unknown / not-stated does NOT mean the vehicle is clean.
 *
 * Only an actual positive salvage category creates a contradiction
 * with a "never written off" claim.
 */
internal fun crossCheckSalvageClaim(
    advertText: String,
    officialSalvageCategory: String?,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {
    val category =
        cleanText(officialSalvageCategory)
            ?.uppercase()
            ?.trim()

    /*
     * -------------------------------------------------------------
     * SELLER WRITE-OFF CLAIM
     * -------------------------------------------------------------
     */
    val writtenOffClaim =
        containsAny(
            advertText,
            "never been written off",
            "never written off",
            "not written off",
            "never a write off",
            "never a write-off",
            "not a write off",
            "not a write-off"
        )

    /*
     * -------------------------------------------------------------
     * NO OFFICIAL RESULT
     * -------------------------------------------------------------
     *
     * Missing evidence is not contradictory evidence.
     */
    if (
        category == null ||
        category in setOf(
            "NOT STATED",
            "UNKNOWN",
            "NULL",
            "N/A",
            "NA"
        )
    ) {
        if (writtenOffClaim) {
            verificationItems.add(
                "The advert claims the vehicle has never been written off, but no explicit official salvage result is available to independently confirm that claim."
            )
        }

        return
    }

    /*
     * -------------------------------------------------------------
     * PENDING / UNRESOLVED OFFICIAL RESULT
     * -------------------------------------------------------------
     *
     * PENDING is not confirmation of a write-off. It means the
     * available field cannot currently support a clean conclusion.
     */
        if (
        category in setOf(
            "PENDING",
            "PENDING VERIFICATION",
            "UNDER REVIEW"
        )
    ) {
        if (writtenOffClaim) {
            warnings.add(
                "Official vehicle data contains a salvage/write-off status marker of 'PENDING'. Please purchase a Pro Report for complete verification."
            )
            verificationItems.add(
                "Purchase a Pro Report to verify the pending salvage/write-off status using an independent vehicle-history source before purchase."
            )
        } else {
            warnings.add(
                "Official vehicle data contains a salvage/write-off status marker of 'PENDING'. Please purchase a Pro Report to review full details."
            )
            verificationItems.add(
                "Purchase a Pro Report to obtain independent vehicle-history evidence for the pending salvage status before purchase."
            )
        }

        return
    }


    /*
     * -------------------------------------------------------------
     * EXPLICIT NEGATIVE OFFICIAL RESULT
     * -------------------------------------------------------------
     */
    if (
        category in setOf(
            "NONE",
            "NO"
        )
    ) {
        if (writtenOffClaim) {
            confirmations.add(
                "The available official vehicle record does not currently contain a recorded salvage category indicating a write-off."
            )
        }

        return
    }

    /*
     * -------------------------------------------------------------
     * POSITIVE OFFICIAL SALVAGE EVIDENCE
     * -------------------------------------------------------------
     *
     * Anything outside the explicit negative values above is
     * treated as a supplied salvage/category marker.
     */
    if (writtenOffClaim) {

        warnings.add(
            "The advert claims the vehicle has never been written off, but the available official vehicle data contains salvage category '$category'."
        )

    } else {

        warnings.add(
            "The available official vehicle data contains salvage category '$category'."
        )
    }

    verificationItems.add(
        "Verify the recorded salvage category and obtain supporting vehicle-history evidence before purchase."
    )
}