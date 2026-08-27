package com.hiddenhistory.engine.advert.crosscheck

import com.hiddenhistory.engine.ParsedVehicleAdvert
import com.hiddenhistory.models.Vehicle
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun crossCheckVehicleIdentity(
    advert: ParsedVehicleAdvert,
    vehicle: Vehicle,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {
    crossCheckTextClaim(
        advertValue = advertValue(
            advert,
            "registration",
            "registrationNumber",
            "reg",
            "vrm"
        ),
        officialValue = firstNonBlank(
            vehicle.registrationNumber,
            vehicle.registration
        ),
        displayName = "registration",
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )

    crossCheckTextClaim(
        advertValue = advertValue(
            advert,
            "make",
            "manufacturer",
            "vehicleMake"
        ),
        officialValue = vehicle.make,
        displayName = "make",
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )

    crossCheckTextClaim(
        advertValue = advertValue(
            advert,
            "model",
            "vehicleModel"
        ),
        officialValue = vehicle.model,
        displayName = "model",
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )

    crossCheckFuelType(
        advertValue = advertValue(
            advert,
            "fuelType",
            "fuel",
            "fuel_type"
        ),
        officialValue = vehicle.fuelType,
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )

    crossCheckEngineCapacity(
        advertValue = advertValue(
            advert,
            "engineCapacity",
            "engineSize",
            "engine",
            "engineCapacityCc",
            "engineCc"
        ),
        officialCc = vehicle.engineCapacity
            ?: parseEngineSizeToCc(vehicle.engineSize),
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )

    crossCheckYear(
        advertValue = advertValue(
            advert,
            "year",
            "yearOfManufacture",
            "vehicleYear"
        ),
        officialYear = vehicle.year,
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )

    /*
     * -------------------------------------------------------------
     * MILEAGE
     * -------------------------------------------------------------
     *
     * This is deliberately checked against the authoritative
     * mileage already present on the Vehicle record.
     *
     * The MOT-history engine separately checks the complete MOT
     * sequence. This check exists so that an advert mileage claim
     * is also compared directly with the current DVLA/DVSA vehicle
     * data supplied to this engine.
     *
     * No mileage discrepancy is reported when either side is
     * unavailable.
     */
    crossCheckMileageClaim(
        advertValue = advertValue(
            advert,
            "mileage",
            "mileageMiles",
            "mileage_normalized",
            "odometer",
            "odometerReading"
        ) ?: advert.mileage,
        officialValue = vehicle.mileage,
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )

    crossCheckIntegerClaim(
        label = "seats",
        advertValue = advertValue(
            advert,
            "seats",
            "seatCount"
        ),
        officialValue = vehicle.seats,
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )

    crossCheckNumericClaim(
        label = "maximum towing weight",
        advertValue = advertValue(
            advert,
            "maxTowWeight",
            "maximumTowWeight",
            "towWeight"
        ),
        officialValue = vehicle.maxTowWeight?.toDouble(),
        tolerance = 1.0,
        unitText = " kg",
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )

    crossCheckDateClaim(
        label = "MOT expiry",
        advertValue = advertValue(
            advert,
            "motExpiryDate",
            "motExpiry",
            "motUntil",
            "motDueDate"
        ),
        officialValue = vehicle.motExpiryDate,
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )

    crossCheckMotStatus(
        advertValue = advertValue(
            advert,
            "motStatus",
            "mot"
        ),
        officialValue = vehicle.motStatus,
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )

    crossCheckOwnerClaim(
        advertValue = advertValue(
            advert,
            "previousOwners",
            "previousKeepers",
            "owners",
            "keepers",
            "ownerCount",
            "keeperCount"
        ),
        advertText = advertText(advert),
        officialOwners = vehicle.previousOwners
            ?: vehicle.previousKeepers,
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )

    crossCheckSalvageClaim(
        advertText = advertText(advert),
        officialSalvageCategory = vehicle.salvageCategory,
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )

    if (vehicle.markedForExport == true) {
        warnings.add(
            "Official vehicle data indicates that the vehicle is marked for export. " +
                "This conflicts with treating the vehicle as an ordinary UK-market vehicle " +
                "without further verification."
        )

        verificationItems.add(
            "Verify the vehicle's current registration/export status and documentation before purchase."
        )
    } else if (vehicle.markedForExport == false) {
        confirmations.add(
            "Official vehicle data does not currently indicate that the vehicle is marked for export."
        )
    }

    if (hasPositiveMarker(vehicle.hasOutstandingRecall)) {
        warnings.add(
            "Official vehicle data indicates an outstanding safety recall marker."
        )

        verificationItems.add(
            "Verify the outstanding recall with the manufacturer or authorised repairer and establish " +
                "whether the recall work has been completed before purchase."
        )
    }

    crossCheckTaxStatus(
        advertValue = advertValue(
            advert,
            "taxStatus",
            "tax"
        ),
        officialValue = vehicle.taxStatus,
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )

    crossCheckVin(
        advertValue = advertValue(
            advert,
            "vin",
            "vehicleIdentificationNumber"
        ),
        officialValue = vehicle.vin,
        warnings = warnings,
        confirmations = confirmations,
        verificationItems = verificationItems
    )
}

internal fun addActiveSymptomsEvidence(
    vehicle: Vehicle,
    warnings: MutableList<String>,
    verificationItems: MutableList<String>
) {
    if (vehicle.activeSymptoms.isEmpty()) return

    val symptomCount = vehicle.activeSymptoms.count()

    warnings.add(
        "The vehicle record contains $symptomCount active reported symptom" +
            if (symptomCount == 1) "." else "s."
    )

    verificationItems.add(
        "Review the reported vehicle symptoms and confirm the seller's explanation and the current " +
            "physical condition of the vehicle before purchase."
    )
}

internal fun crossCheckTextClaim(
    advertValue: Any?,
    officialValue: String?,
    displayName: String,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {
    val advertText = cleanText(advertValue)
    val officialText = cleanText(officialValue)

    if (advertText == null || officialText == null) return

    if (
        normaliseComparableText(advertText) ==
        normaliseComparableText(officialText)
    ) {
        confirmations.add(
            "Advert $displayName '$advertText' matches the official vehicle record."
        )
    } else {
        if (displayName.equals("model", ignoreCase = true)) {
            warnings.add(
                "CRITICAL VEHICLE IDENTITY DISCREPANCY: the advert identifies the vehicle as '$advertText', while the official vehicle record identifies it as '$officialText'."
            )

            verificationItems.add(
                "Do not pay a deposit or proceed with purchase until the advertised model, registration, VIN, V5C and official vehicle record have been reconciled."
            )
        } else {
            warnings.add(
                "Advert $displayName '$advertText' does not match the official vehicle record '$officialText'."
            )

            verificationItems.add(
                "Verify the vehicle identity and ask the seller to explain the $displayName discrepancy before purchase."
            )
        }
    }
}

/**
 * Compares the advertised mileage directly with the authoritative
 * mileage supplied on the Vehicle record.
 *
 * This is intentionally separate from MOT-history regression
 * detection. A mileage discrepancy against the current official
 * vehicle record is not the same thing as proving a mileage rollback.
 *
 * Examples:
 *
 * Advert: 67,500 miles
 * Official: 72,341 miles
 * -> discrepancy warning
 *
 * Advert: 75,000 miles
 * Official: 72,341 miles
 * -> not a rollback warning; mileage may have increased since
 *    the official reading.
 *
 * Advert: 72,341 miles
 * Official: 72,341 miles
 * -> confirmation.
 */
internal fun crossCheckMileageClaim(
    advertValue: Any?,
    officialValue: Any?,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {
    val advertMileage =
        extractAdvertMileage(advertValue)
            ?: advertValue
                ?.let { cleanText(it) }
                ?.let { extractMileage(it) }
                ?.takeIf { it > 0 }
                ?.let {
                    AdvertMileage(
                        originalMileage = it,
                        originalUnit = MileageUnit.MILES,
                        miles = it
                    )
                }

    val officialMileage =
        extractMileage(officialValue)
            ?.takeIf { it > 0 }

    /*
     * We cannot compare what we do not have.
     */
    if (
        advertMileage == null ||
        officialMileage == null
    ) {
        return
    }

    val advertMiles =
        advertMileage.miles

    val difference =
        officialMileage - advertMiles

    /*
     * -------------------------------------------------------------
     * ADVERT LOWER THAN OFFICIAL
     * -------------------------------------------------------------
     *
     * This is the important contradiction.
     *
     * If the authoritative vehicle record already contains a
     * mileage higher than the seller's advertised mileage, the
     * advert mileage is inconsistent with official evidence.
     *
     * We deliberately do NOT call this "mileage rollback" here.
     * The MOT-history engine is responsible for establishing
     * chronological mileage regressions.
     */
    if (
        difference >
        MILEAGE_ROUNDING_TOLERANCE
    ) {
        warnings.add(
            "Mileage discrepancy detected: the advert states " +
                "${formatAdvertMileage(advertMileage)}, while the official vehicle record " +
                "reports ${formatMileage(officialMileage)} miles. The official mileage is " +
                "approximately ${formatMileage(difference)} miles higher than the advertised mileage."
        )

        verificationItems.add(
            "Ask the seller to explain why the advertised mileage is lower than the official " +
                "vehicle mileage. Verify the current odometer reading, MOT history and service records before purchase."
        )

        return
    }

    /*
     * -------------------------------------------------------------
     * ADVERT HIGHER THAN OFFICIAL
     * -------------------------------------------------------------
     *
     * This is NOT automatically suspicious.
     *
     * The vehicle may simply have been driven since the official
     * mileage was recorded.
     */
    if (
        -difference >
        MILEAGE_ROUNDING_TOLERANCE
    ) {
        val increase =
            -difference

        if (
            increase >=
            SIGNIFICANT_CURRENT_MILEAGE_GAP
        ) {
            verificationItems.add(
                "The advertised mileage is approximately ${formatMileage(increase)} miles above " +
                    "the official vehicle mileage. This may simply reflect mileage accumulated " +
                    "since the official reading, but the current odometer and recent mileage " +
                    "documentation should be checked."
            )
        } else {
            confirmations.add(
                "The advertised mileage of ${formatAdvertMileage(advertMileage)} is approximately " +
                    "${formatMileage(increase)} miles above the official vehicle mileage, which is " +
                    "consistent with mileage accumulated since the official reading."
            )
        }

        return
    }

    /*
     * -------------------------------------------------------------
     * MATCH
     * -------------------------------------------------------------
     */
    confirmations.add(
        "The advertised mileage of ${formatAdvertMileage(advertMileage)} " +
            "matches the official vehicle mileage record."
    )
}

internal fun crossCheckFuelType(
    advertValue: Any?,
    officialValue: String?,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {
    val advertFuel = normaliseFuelType(cleanText(advertValue))
    val officialFuel = normaliseFuelType(cleanText(officialValue))

    if (advertFuel == null || officialFuel == null) return

    if (advertFuel == officialFuel) {
        confirmations.add(
            "Advert fuel type '$advertFuel' matches the official vehicle record."
        )
    } else {
        warnings.add(
            "Fuel type discrepancy detected: the advert states '$advertFuel' while the official vehicle record states '$officialFuel'."
        )

        verificationItems.add(
            "Verify the vehicle identity and fuel type from the vehicle documentation and physical vehicle before purchase."
        )
    }
}

internal fun crossCheckEngineCapacity(
    advertValue: Any?,
    officialCc: Int?,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {
    val official = officialCc ?: return
    val advertCc = parseCcFromText(cleanText(advertValue)) ?: return

    val tolerance = maxOf(
        ENGINE_CC_ABSOLUTE_TOLERANCE,
        (official * ENGINE_CC_PERCENT_TOLERANCE).roundToInt()
    )

    if (abs(advertCc - official) <= tolerance) {
        confirmations.add(
            "Advert engine description is consistent with the official engine capacity of ${official}cc."
        )
    } else {
        warnings.add(
            "Engine capacity discrepancy detected: the advert describes approximately ${advertCc}cc while the official vehicle record reports ${official}cc."
        )

        verificationItems.add(
            "Verify the vehicle specification, VIN and engine identity because the advertised engine description does not match the official capacity."
        )
    }
}

internal fun crossCheckYear(
    advertValue: Any?,
    officialYear: Int?,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {
    val advertYear = extractYear(cleanText(advertValue))

    if (advertYear == null || officialYear == null) return

    if (advertYear == officialYear) {
        confirmations.add(
            "Advert vehicle year $advertYear matches the official vehicle record."
        )
    } else {
        warnings.add(
            "Vehicle year discrepancy detected: the advert states $advertYear while the official vehicle record reports $officialYear."
        )

        verificationItems.add(
            "Verify the vehicle identity, registration date and VIN before relying on the advertised year."
        )
    }
}

internal fun crossCheckIntegerClaim(
    label: String,
    advertValue: Any?,
    officialValue: Int?,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {
    val advertNumber = extractInteger(cleanText(advertValue))

    if (advertNumber == null || officialValue == null) return

    if (advertNumber == officialValue) {
        confirmations.add(
            "Advert $label value matches the official vehicle record."
        )
    } else {
        warnings.add(
            "Advert $label value $advertNumber does not match the official vehicle value $officialValue."
        )

        verificationItems.add(
            "Verify the vehicle specification because the advertised $label value differs from the official record."
        )
    }
}

internal fun crossCheckNumericClaim(
    label: String,
    advertValue: Any?,
    officialValue: Double?,
    tolerance: Double,
    unitText: String,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {
    val advertNumber = extractDouble(cleanText(advertValue))

    if (advertNumber == null || officialValue == null) return

    if (abs(advertNumber - officialValue) <= tolerance) {
        confirmations.add(
            "Advert $label is consistent with the official vehicle record."
        )
    } else {
        warnings.add(
            "Advert $label of $advertNumber$unitText does not match the official vehicle value of $officialValue$unitText."
        )

        verificationItems.add(
            "Verify the vehicle specification and supporting documentation for the advertised $label."
        )
    }
}

internal fun crossCheckDateClaim(
    label: String,
    advertValue: Any?,
    officialValue: String?,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {
    val advertDate = parseAdvertDate(cleanText(advertValue))
    val officialDate = parseAdvertDate(cleanText(officialValue))

    if (advertDate == null || officialDate == null) return

    if (advertDate == officialDate) {
        confirmations.add(
            "Advert $label is consistent with the official vehicle record."
        )
    } else {
        warnings.add(
            "Advert $label does not match the official vehicle record. Advert: $advertDate; official: $officialDate."
        )

        verificationItems.add(
            "Verify the current MOT documentation and ask the seller to explain the $label discrepancy."
        )
    }
}

internal fun crossCheckMotStatus(
    advertValue: Any?,
    officialValue: String?,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {
    val advertStatus = normaliseStatus(cleanText(advertValue))
    val officialStatus = normaliseStatus(cleanText(officialValue))

    if (advertStatus == null || officialStatus == null) return

    if (advertStatus == officialStatus) {
        confirmations.add(
            "Advert MOT status '$advertStatus' is consistent with the official vehicle record."
        )
    } else {
        warnings.add(
            "MOT status discrepancy detected: the advert states '$advertStatus' while the official vehicle record states '$officialStatus'."
        )

        verificationItems.add(
            "Verify the current MOT status directly from the official record before purchase."
        )
    }
}

internal fun crossCheckTaxStatus(
    advertValue: Any?,
    officialValue: String?,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {
    val advertStatus = normaliseStatus(cleanText(advertValue))
    val officialStatus = normaliseStatus(cleanText(officialValue))

    if (advertStatus == null || officialStatus == null) return

    if (advertStatus == officialStatus) {
        confirmations.add(
            "Advert tax status is consistent with the official vehicle record."
        )
    } else {
        warnings.add(
            "Tax status discrepancy detected: the advert states '$advertStatus' while the official vehicle record states '$officialStatus'."
        )

        verificationItems.add(
            "Verify the current tax status and seller's explanation before purchase."
        )
    }
}

internal fun crossCheckVin(
    advertValue: Any?,
    officialValue: String?,
    warnings: MutableList<String>,
    confirmations: MutableList<String>,
    verificationItems: MutableList<String>
) {
    val advertVin = cleanText(advertValue)
    val officialVin = cleanText(officialValue)

    if (advertVin == null || officialVin == null) return

    if (
        normaliseComparableText(advertVin) ==
        normaliseComparableText(officialVin)
    ) {
        confirmations.add(
            "Advert VIN matches the official vehicle record."
        )
    } else {
        warnings.add(
            "VIN discrepancy detected between the advert and official vehicle record."
        )

        verificationItems.add(
            "Do not proceed until the VIN on the vehicle, V5C and official record have been reconciled."
        )
    }
}