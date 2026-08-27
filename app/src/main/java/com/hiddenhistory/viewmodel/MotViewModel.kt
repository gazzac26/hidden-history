package com.hiddenhistory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.models.MotTest
import com.hiddenhistory.models.Vehicle
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class MotViewModel : ViewModel() {

    /*
     * ============================================================
     * SELECTED MOT TEST
     * ============================================================
     */

    private val _selectedMotTest =
        kotlinx.coroutines.flow.MutableStateFlow<MotTest?>(null)

    val selectedMotTest:
        kotlinx.coroutines.flow.StateFlow<MotTest?> =
        _selectedMotTest.asStateFlow()

    /*
     * ============================================================
     * SELECTED OFFICIAL VEHICLE
     * ============================================================
     *
     * This is the complete Vehicle returned by the official vehicle
     * search. It travels with the selected MOT test into the MOT
     * detail screen and eBay parts search.
     */

    private val _selectedVehicle =
        kotlinx.coroutines.flow.MutableStateFlow<Vehicle?>(null)

    val selectedVehicle:
        kotlinx.coroutines.flow.StateFlow<Vehicle?> =
        _selectedVehicle.asStateFlow()

    /*
     * ============================================================
     * EBAY PART SEARCH
     * ============================================================
     */

    private val client =
        OkHttpClient()

    private val partsHandler =
        VehicleSearchParts(
            client = client
        )

    /*
     * ============================================================
     * SELECT MOT TEST
     * ============================================================
     */

    fun selectMotTest(
        motTest: MotTest,
        vehicle: Vehicle
    ) {

        _selectedMotTest.value =
            motTest

        _selectedVehicle.value =
            vehicle

        Log.d(
            "MotViewModel",
            "Selected MOT: id=${motTest.id}, " +
                "registration=${motTest.vehicleRegistration}, " +
                "make=${motTest.vehicleMake}, " +
                "model=${motTest.vehicleModel}, " +
                "year=${motTest.vehicleYear}, " +
                "engine=${motTest.vehicleEngineCapacity}"
        )

        Log.d(
            "MotViewModel",
            "Official Vehicle: " +
                "registration=${vehicle.registrationNumber}, " +
                "make=${vehicle.make}, " +
                "model=${vehicle.model}, " +
                "year=${vehicle.year}, " +
                "engine=${vehicle.engineCapacity}, " +
                "fuel=${vehicle.fuelType}, " +
                "vin=${vehicle.vin}"
        )
    }

    /*
     * ============================================================
     * EBAY PART SEARCH
     * ============================================================
     */

    fun searchEbayPart(
        defectText: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val motTest =
            _selectedMotTest.value

        val vehicle =
            _selectedVehicle.value

        if (
            motTest == null
        ) {

            onError(
                "No MOT test is selected for this vehicle."
            )

            return
        }

        if (
            vehicle == null
        ) {

            onError(
                "No official vehicle is selected for this MOT test."
            )

            return
        }

        if (
            defectText.isBlank()
        ) {

            onError(
                "The MOT defect is empty."
            )

            return
        }

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            try {

                val session =
                    SupabaseManager.client
                        .auth
                        .currentSessionOrNull()

                if (
                    session?.user?.id == null
                ) {

                    throw IllegalStateException(
                        "You must be logged in to search parts."
                    )
                }

                Log.d(
                    "MotViewModel",
                    "Starting eBay parts search."
                )

                Log.d(
                    "MotViewModel",
                    "Vehicle source: official vehicle search"
                )

                Log.d(
                    "MotViewModel",
                    "Official registration=${vehicle.registrationNumber}"
                )

                Log.d(
                    "MotViewModel",
                    "Official make=${vehicle.make}"
                )

                Log.d(
                    "MotViewModel",
                    "Official model=${vehicle.model}"
                )

                Log.d(
                    "MotViewModel",
                    "Official year=${vehicle.year}"
                )

                Log.d(
                    "MotViewModel",
                    "Official engineCapacity=${vehicle.engineCapacity}"
                )

                Log.d(
                    "MotViewModel",
                    "Official fuelType=${vehicle.fuelType}"
                )

                Log.d(
                    "MotViewModel",
                    "Official VIN=${vehicle.vin}"
                )

                Log.d(
                    "MotViewModel",
                    "Registration metadata=${motTest.vehicleRegistration}"
                )

                Log.d(
                    "MotViewModel",
                    "Make=${motTest.vehicleMake}"
                )

                Log.d(
                    "MotViewModel",
                    "Model=${motTest.vehicleModel}"
                )

                Log.d(
                    "MotViewModel",
                    "Year=${motTest.vehicleYear}"
                )

                Log.d(
                    "MotViewModel",
                    "Engine=${motTest.vehicleEngineCapacity}"
                )

                val responseData =
                    partsHandler.executeEbaySearch(
                        defectText =
                            defectText,

                        vehicle =
                            vehicle,

                        motTest =
                            motTest
                    )

                withContext(
                    Dispatchers.Main
                ) {

                    onSuccess(
                        responseData
                    )
                }

            } catch (e: Throwable) {

                Log.e(
                    "MotViewModel",
                    "eBay search failed: ${e.message}",
                    e
                )

                withContext(
                    Dispatchers.Main
                ) {

                    onError(
                        e.localizedMessage
                            ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    /*
     * ============================================================
     * DIRECT EBAY URL
     * ============================================================
     */

    fun buildEbaySearchUrl(
        defectText: String
    ): String? {

        val motTest =
            _selectedMotTest.value
                ?: return null

        val vehicle =
            _selectedVehicle.value
                ?: return null

        return runCatching {

            partsHandler.buildEbaySearchUrl(
                vehicle =
                    vehicle,

                motTest =
                    motTest,

                defectText =
                    defectText
            )

        }.getOrNull()
    }

    /*
     * ============================================================
     * CLEAR MOT TEST
     * ============================================================
     */

    fun clearSelectedMotTest() {

        _selectedMotTest.value =
            null

        _selectedVehicle.value =
            null
    }
}