package com.hiddenhistory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.models.MotTest
import com.hiddenhistory.models.Vehicle
import com.hiddenhistory.repository.HiddenHistoryRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class SavedVehicleRecord(
    val id: String?,
    val registration: String,
    val vehicle_json: String,
    val advert_json: String? = null,
    val cross_check_json: String? = null,
    val report_summary: String? = null,
    val created_at: String? = null
)

class SavedVehicleReportsViewModel : ViewModel() {

    private val jsonParser =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    private val hiddenHistoryRepository =
        HiddenHistoryRepository(
            SupabaseManager.client
        )

    private val _savedReportsList =
        MutableStateFlow<List<SavedVehicleRecord>>(
            emptyList()
        )

    val savedReportsList:
        StateFlow<List<SavedVehicleRecord>> =
        _savedReportsList.asStateFlow()

    private val _selectedReportUiState =
        MutableStateFlow<List<Any>>(
            emptyList()
        )

    val selectedReportUiState:
        StateFlow<List<Any>> =
        _selectedReportUiState.asStateFlow()

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading:
        StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _selectedMotTest =
        MutableStateFlow<MotTest?>(null)

    val selectedMotTest:
        StateFlow<MotTest?> =
        _selectedMotTest.asStateFlow()


    /*
     * ------------------------------------------------------------
     * INITIAL LOAD
     * ------------------------------------------------------------
     *
     * Supabase authentication can take a short amount of time to
     * restore the persisted session after a cold application start.
     *
     * fetchSavedReportsList() therefore waits briefly for the
     * session to become available instead of immediately assuming
     * that the user is logged out.
     */
    init {

        fetchSavedReportsList()
    }

    fun selectMotTest(
        motTest: MotTest
    ) {

        _selectedMotTest.value =
            motTest
    }

    /*
     * ------------------------------------------------------------
     * FETCH SAVED REPORTS
     * ------------------------------------------------------------
     *
     * Loads the user's permanent reports from Supabase.
     *
     * IMPORTANT:
     *
     * This is the code responsible for restoring the saved reports
     * after the application has been closed and reopened.
     */
    fun fetchSavedReportsList() {

        viewModelScope.launch {

            withContext(Dispatchers.Main) {

                _isLoading.value =
                    true
            }

            try {

                /*
                 * ------------------------------------------------
                 * WAIT FOR SUPABASE AUTH SESSION
                 * ------------------------------------------------
                 *
                 * On a cold start the ViewModel can be created
                 * before Supabase has finished restoring the
                 * persisted login session.
                 *
                 * Give the session restoration a short window.
                 */
                var session =
                    SupabaseManager.client
                        .auth
                        .currentSessionOrNull()

                var attempts =
                    0

                while (
                    session == null &&
                    attempts < 10
                ) {

                    delay(300)

                    session =
                        SupabaseManager.client
                            .auth
                            .currentSessionOrNull()

                    attempts++
                }

                val userId =
                    session
                        ?.user
                        ?.id

                /*
                 * ------------------------------------------------
                 * NO AUTHENTICATED USER
                 * ------------------------------------------------
                 */
                if (userId == null) {

                    Log.w(
                        "SavedVehicleReportsVM",
                        "Fetch failed: User session was not available after waiting for authentication restoration."
                    )

                    withContext(Dispatchers.Main) {

                        _savedReportsList.value =
                            emptyList()
                    }

                    return@launch
                }

                Log.d(
                    "SavedVehicleReportsVM",
                    "Authenticated user found: $userId"
                )

                /*
                 * ------------------------------------------------
                 * LOAD REPORTS FROM SUPABASE
                 * ------------------------------------------------
                 */
                val entities =
                    withContext(Dispatchers.IO) {

                        hiddenHistoryRepository
                            .getUserReports(
                                userId
                            )
                    }

                /*
                 * ------------------------------------------------
                 * MAP DATABASE ENTITIES TO UI RECORDS
                 * ------------------------------------------------
                 */
                val mappedRecords =
                    entities.map { entity ->

                        SavedVehicleRecord(

                            id =
                                entity.id,

                            registration =
                                entity.registration,

                            vehicle_json =
                                entity.vehicleData.toString(),

                            advert_json =
                                entity.advertData?.toString(),

                            cross_check_json =
                                entity.languageAnalysis?.toString(),

                            report_summary =
                                entity.reportSummary,

                            created_at =
                                entity.createdAt
                        )
                    }

                /*
                 * ------------------------------------------------
                 * UPDATE UI STATE
                 * ------------------------------------------------
                 */
                withContext(Dispatchers.Main) {

                    _savedReportsList.value =
                        mappedRecords
                }

                Log.d(
                    "SavedVehicleReportsVM",
                    "Successfully fetched ${mappedRecords.size} saved reports."
                )

            } catch (e: Throwable) {

                Log.e(
                    "SavedVehicleReportsVM",
                    "Failed to fetch saved reports: ${e.message}",
                    e
                )

                withContext(Dispatchers.Main) {

                    _savedReportsList.value =
                        emptyList()
                }

            } finally {

                withContext(Dispatchers.Main) {

                    _isLoading.value =
                        false
                }
            }
        }
    }

    /*
     * ------------------------------------------------------------
     * LOAD SAVED REPORT DETAILS
     * ------------------------------------------------------------
     *
     * Takes the vehicle JSON stored in Supabase, reconstructs the
     * Vehicle model and runs the existing intelligence pipeline.
     */
    fun loadSavedReportDetails(
        record: SavedVehicleRecord
    ) {

        viewModelScope.launch {

            withContext(Dispatchers.Main) {

                _isLoading.value =
                    true
            }

            try {

                val vehicle =
                    withContext(Dispatchers.Default) {

                        jsonParser.decodeFromString<Vehicle>(
                            record.vehicle_json
                        )
                    }


                withContext(Dispatchers.Main) {

                    _selectedReportUiState.value =
                        mapVehicleToAdapterList(
                            vehicle
                        )
                }

            } catch (e: Throwable) {

                Log.e(
                    "SavedVehicleReportsVM",
                    "Failed to decode saved report: ${e.message}",
                    e
                )

                withContext(Dispatchers.Main) {

                    _selectedReportUiState.value =
                        listOf(
                            "Error loading report: ${
                                e.localizedMessage
                                    ?: "Unknown error"
                            }"
                        )
                }

            } finally {

                withContext(Dispatchers.Main) {

                    _isLoading.value =
                        false
                }
            }
        }
    }

    /*
     * ------------------------------------------------------------
     * DELETE REPORT
     * ------------------------------------------------------------
     */
    fun deleteReport(
        reportId: String,
        onSuccess: () -> Unit
    ) {

        viewModelScope.launch {

            try {

                withContext(Dispatchers.IO) {

                    hiddenHistoryRepository
                        .deleteUserReport(
                            reportId
                        )
                }

                /*
                 * Reload the list from Supabase after deletion.
                 *
                 * This guarantees that the UI represents the
                 * actual database state.
                 */
                fetchSavedReportsList()

                withContext(Dispatchers.Main) {

                    onSuccess()
                }

            } catch (e: Throwable) {

                Log.e(
                    "SavedVehicleReportsVM",
                    "Failed to delete report $reportId: ${e.message}",
                    e
                )
            }
        }
    }

    /*
     * ------------------------------------------------------------
     * VEHICLE -> UI
     * ------------------------------------------------------------
     */
    private fun mapVehicleToAdapterList(
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
                "Seats" to
                    it.toString()
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
