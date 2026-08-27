package com.hiddenhistory.database

import android.content.Context
import com.hiddenhistory.models.SavedVehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class SavedVehicleDao(private val context: Context) {
    private val vehiclesFile = File(context.filesDir, "saved_vehicles.json")
    private val _vehiclesFlow = MutableStateFlow<List<SavedVehicle>>(loadVehiclesSync())

    private fun loadVehiclesSync(): List<SavedVehicle> {
        return try {
            if (vehiclesFile.exists()) {
                Json.decodeFromString<List<SavedVehicle>>(vehiclesFile.readText())
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAllSavedVehicles(): Flow<List<SavedVehicle>> {
        return _vehiclesFlow.asStateFlow()
    }

    fun getSavedVehicleCount(): Flow<Int> {
        return _vehiclesFlow.map { it.size }
    }

    fun isVehicleSaved(vehicleId: String): Flow<Boolean> {
        return _vehiclesFlow.map { list -> list.any { it.id == vehicleId } }
    }

    suspend fun insertVehicle(vehicle: SavedVehicle) {
        try {
            val currentList = loadVehiclesSync().toMutableList()
            currentList.removeAll { it.id == vehicle.id }
            currentList.add(vehicle)
            vehiclesFile.writeText(Json.encodeToString(currentList))
            _vehiclesFlow.value = currentList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteVehicleById(vehicleId: String) {
        try {
            val currentList = loadVehiclesSync().toMutableList()
            currentList.removeAll { it.id == vehicleId }
            vehiclesFile.writeText(Json.encodeToString(currentList))
            _vehiclesFlow.value = currentList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
