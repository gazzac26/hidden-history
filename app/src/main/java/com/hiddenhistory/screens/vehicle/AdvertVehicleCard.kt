package com.hiddenhistory.screens.vehicle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hiddenhistory.models.Vehicle

@Composable
fun AdvertVehicleCard(
    vehicle: Vehicle
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Advert Vehicle Data",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text("• Make: ${vehicle.make ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
            Text("• Model: ${vehicle.model ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
            Text("• Year: ${vehicle.year ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
            Text("• Engine Capacity: ${vehicle.engineCapacity?.let { "$it cc" } ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
            Text("• Fuel Type: ${vehicle.fuelType ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
            Text("• Parsed Price: ${vehicle.price?.let { "£$it" } ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
            Text("• Parsed Mileage: ${vehicle.mileage ?: "Unknown"} miles", style = MaterialTheme.typography.bodySmall)
        }
    }
}
