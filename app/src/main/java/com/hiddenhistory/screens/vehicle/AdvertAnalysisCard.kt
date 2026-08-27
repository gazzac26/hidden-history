package com.hiddenhistory.screens.vehicle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hiddenhistory.engine.ParsedVehicleAdvert

@Composable
fun AdvertAnalysisCard(
    advert: ParsedVehicleAdvert
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Advert Analysis",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text("• Condition Index: ${advert.conditionScore}%", style = MaterialTheme.typography.bodySmall)
            Text("• Market Dialect: ${advert.detectedDialect}", style = MaterialTheme.typography.bodySmall)
            Text("• Asking Price: ${advert.price ?: "Not Specified"}", style = MaterialTheme.typography.bodySmall)
            Text("• Stated Mileage: ${advert.mileage ?: "Not Specified"}", style = MaterialTheme.typography.bodySmall)

            advert.year?.let { Text("• Advert Year: $it", style = MaterialTheme.typography.bodySmall) }
            advert.engineSize?.let { Text("• Advert Engine: $it", style = MaterialTheme.typography.bodySmall) }
            advert.fuelType?.let { Text("• Advert Fuel: $it", style = MaterialTheme.typography.bodySmall) }
            advert.transmission?.let { Text("• Advert Transmission: $it", style = MaterialTheme.typography.bodySmall) }

            if (advert.riskFlags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Risk Flags",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                advert.riskFlags.forEach { flag ->
                    Text("• $flag", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (advert.keyInsights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Key Insights",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                advert.keyInsights.forEach { insight ->
                    Text("• $insight", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
