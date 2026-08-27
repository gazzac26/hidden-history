package com.hiddenhistory.viewmodel

import android.util.Log
import com.hiddenhistory.models.Vehicle
import org.json.JSONObject

class VehicleSearchReportBuilder {

    /*
     * ------------------------------------------------------------
     * JSON -> MAP
     * ------------------------------------------------------------
     */

    fun parseJsonToMap(
        jsonString: String
    ): Map<String, Any?> {

        val map =
            mutableMapOf<String, Any?>()

        try {

            val jsonObject =
                JSONObject(
                    jsonString
                )

            val keys =
                jsonObject.keys()

            while (keys.hasNext()) {

                val key =
                    keys.next()

                val value =
                    jsonObject.opt(
                        key
                    )

                map[key] =
                    if (
                        value == JSONObject.NULL
                    ) {
                        null
                    } else {
                        value
                    }
            }

        } catch (e: Exception) {

            map["error"] =
                "Failed to parse raw payload: ${e.message}"
        }

        return map
    }

    /*
     * ------------------------------------------------------------
     * VEHICLE -> UI
     * ------------------------------------------------------------
     */

    fun mapVehicleToAdapterList(
        vehicle: Vehicle
    ): List<Any> {

        val list =
            mutableListOf<Any>()

        list.add(
            "Vehicle Identity"
        )

        vehicle.registrationNumber?.let {

            list.add(
                "Registration" to it
            )
        }

        vehicle.registration?.let {

            if (
                it != vehicle.registrationNumber
            ) {

                list.add(
                    "Alt Registration" to it
                )
            }
        }

        vehicle.make?.let {

            list.add(
                "Make" to it
            )
        }

        vehicle.model?.let {

            list.add(
                "Model" to it
            )
        }

        vehicle.year?.let {

            list.add(
                "Year of Manufacture" to
                    it.toString()
            )
        }

        vehicle.vin?.let {

            list.add(
                "VIN" to it
            )
        }

        vehicle.engineSize?.let {

            list.add(
                "Engine Size" to it
            )
        }

        vehicle.colour?.let {

            list.add(
                "Colour" to it
            )
        }

        vehicle.primaryColour?.let {

            list.add(
                "Primary Colour" to it
            )
        }

        vehicle.wheelplan?.let {

            list.add(
                "Wheelplan" to it
            )
        }

        list.add(
            "Technical Specifications"
        )

        vehicle.engineCapacity?.let {

            list.add(
                "Engine Capacity" to
                    "$it cc"
            )
        }

        vehicle.fuelType?.let {

            list.add(
                "Fuel Type" to it
            )
        }

        vehicle.co2Emissions?.let {

            list.add(
                "CO2 Emissions" to
                    "$it g/km"
            )
        }

        vehicle.typeApproval?.let {

            list.add(
                "Type Approval" to it
            )
        }

        vehicle.seats?.let {

            list.add(
                "Seats" to it.toString()
            )
        }

        vehicle.maxTowWeight?.let {

            list.add(
                "Max Tow Weight" to
                    "$it kg"
            )
        }

        list.add(
            "Registration Details"
        )

        vehicle.registrationDate?.let {

            list.add(
                "Registration Date" to it
            )
        }

        vehicle.monthOfFirstRegistration?.let {

            list.add(
                "Month of First Reg" to it
            )
        }

        vehicle.manufactureDate?.let {

            list.add(
                "Manufacture Date" to it
            )
        }

        vehicle.firstUsedDate?.let {

            list.add(
                "First Used Date" to it
            )
        }

        vehicle.dateOfLastV5CIssued?.let {

            list.add(
                "Last V5C Issued" to it
            )
        }

        vehicle.previousKeepers?.let {

            list.add(
                "Previous Keepers" to
                    it.toString()
            )
        }

        vehicle.previousOwners?.let {

            list.add(
                "Previous Owners" to
                    it.toString()
            )
        }

        list.add(
            "Status & History"
        )

        vehicle.taxStatus?.let {

            list.add(
                "Tax Status" to it
            )
        }

        vehicle.taxDueDate?.let {

            list.add(
                "Tax Due Date" to it
            )
        }

        vehicle.motStatus?.let {

            list.add(
                "MOT Status" to it
            )
        }

        vehicle.motExpiryDate?.let {

            list.add(
                "MOT Expiry Date" to it
            )
        }

        vehicle.price?.let {

            list.add(
                "Estimated Price" to
                    "£$it"
            )
        }

        vehicle.hasOutstandingRecall?.let {

            list.add(
                "Recall Status" to it
            )
        }

        vehicle.salvageCategory?.let {

            list.add(
                "Salvage Category" to it
            )
        }

        vehicle.vehicleTier?.let {

            list.add(
                "Vehicle Tier" to it
            )
        }

        vehicle.markedForExport?.let {

            list.add(
                "Marked For Export" to
                    if (it) "Yes" else "No"
            )
        }

        if (
            vehicle.motTests.isNotEmpty()
        ) {

            list.add(
                "MOT History"
            )

            list.addAll(
                vehicle.motTests
            )
        }

        if (
            vehicle.activeSymptoms.isNotEmpty()
        ) {

            list.add(
                "Active Symptoms"
            )

            list.addAll(
                vehicle.activeSymptoms
            )
        }

        return list
    }

}
