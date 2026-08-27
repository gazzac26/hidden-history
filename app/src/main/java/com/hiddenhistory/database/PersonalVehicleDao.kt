package com.hiddenhistory.database

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PersonalVehicleEntity(
    val id: Long = 0L,
    val make: String,
    val model: String,
    val registration: String,
    val year: String,
    val mileage: String,
    val fuelType: String = "",
    val engineCapacity: String = "",
    val colour: String = "",
    val transmission: String = "",
    val bhp: String = "",
    val taxStatus: String = "",
    val motExpiry: String = "",
    val notes: String = "",
    val photoUris: List<String> = emptyList(), // Up to 10 photos
    // --- GOLDMINE VEHICLE INTELLIGENCE FIELDS ---
    val vin: String = "",
    val taxDueDate: String = "",
    val motStatus: String = "",
    val co2Emissions: String = "",
    val previousKeepers: String = "",
    val hasOutstandingRecall: String = "",
    val salvageCategory: String = "",
    val seats: String = "",
    val maxTowWeight: String = "",
    val motTestsJson: String = "" // Serialized MOT history & defect records
)

class PersonalVehicleDao(context: Context) {
    private val _vehiclesFlow = MutableStateFlow<List<PersonalVehicleEntity>>(emptyList())
    private val vehicleList = mutableListOf<PersonalVehicleEntity>()
    private var nextId = 1L

    fun getAllVehicles(): Flow<List<PersonalVehicleEntity>> = _vehiclesFlow.asStateFlow()

    suspend fun insertVehicle(vehicle: PersonalVehicleEntity) {
        val newVehicle = vehicle.copy(id = nextId++)
        vehicleList.add(0, newVehicle)
        _vehiclesFlow.value = vehicleList.toList()
    }

    suspend fun updateVehicle(vehicle: PersonalVehicleEntity) {
        val index = vehicleList.indexOfFirst { it.id == vehicle.id }
        if (index != -1) {
            vehicleList[index] = vehicle
            _vehiclesFlow.value = vehicleList.toList()
        }
    }

    suspend fun deleteVehicle(vehicle: PersonalVehicleEntity) {
        vehicleList.removeIf { it.id == vehicle.id }
        _vehiclesFlow.value = vehicleList.toList()
    }
}
