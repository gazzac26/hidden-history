package com.hiddenhistory.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSearchGatewayScreen(
    onNavigateToFreeSearch: () -> Unit,
    onNavigateToProSearch: () -> Unit,
    onNavigateBack: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vehicle Search",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Choose your vehicle search",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Select the level of vehicle intelligence you want before entering the vehicle registration.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            /*
             * ----------------------------------------------------
             * FREE SEARCH
             * ----------------------------------------------------
             */

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Search,
                            contentDescription = null,
                            tint =
                                MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Free Vehicle Search",
                            modifier =
                                Modifier.padding(start = 10.dp),
                            style =
                                MaterialTheme.typography.titleLarge,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Text(
                        text =
                            "Official vehicle information and MOT history with Hidden History's free deterministic analysis.",
                        style =
                            MaterialTheme.typography.bodyMedium,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text =
                            "Includes: DVLA, DVSA/MOT and Free Analysis",
                        style =
                            MaterialTheme.typography.bodySmall,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    OutlinedButton(
                        onClick =
                            onNavigateToFreeSearch,
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(14.dp)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Search,
                            contentDescription = null
                        )

                        Text(
                            text = "Start Free Search",
                            modifier =
                                Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            /*
             * ----------------------------------------------------
             * PRO SEARCH
             * ----------------------------------------------------
             */

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Lock,
                            contentDescription = null,
                            tint =
                                MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Pro Vehicle Search",
                            modifier =
                                Modifier.padding(start = 10.dp),
                            style =
                                MaterialTheme.typography.titleLarge,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Text(
                        text =
                            "The full Hidden History vehicle intelligence experience.",
                        style =
                            MaterialTheme.typography.bodyMedium,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text =
                            "Includes: DVLA, DVSA/MOT, AI analysis and premium vehicle intelligence.",
                        style =
                            MaterialTheme.typography.bodySmall,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Button(
                        onClick =
                            onNavigateToProSearch,
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(14.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    MaterialTheme.colorScheme.primary
                            )
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Lock,
                            contentDescription = null
                        )

                        Text(
                            text = "Start Pro Search",
                            modifier =
                                Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}