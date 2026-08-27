package com.hiddenhistory.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_vehicles")
data class SavedVehicle(
    @PrimaryKey val id: String,
    val make: String,
    val model: String,
    val title: String?,
    val vehicleRegistration: String?,
    val vehicleUrl: String?
    // Add other fields as needed
)