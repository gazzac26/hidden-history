package com.hiddenhistory.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hiddenhistory.billing.VehicleReportBillingManager
import com.hiddenhistory.billing.VehicleReportPurchaseState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleReportPaymentScreen(
    activity: Activity,
    billingManager: VehicleReportBillingManager,
    onPurchaseConfirmed: (String) -> Unit,
    onNavigateBack: () -> Unit
) {

    val purchaseState by billingManager.purchaseState.collectAsStateWithLifecycle()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(purchaseState) {

        when (val state = purchaseState) {

            is VehicleReportPurchaseState.Error -> {
                snackbarHostState.showSnackbar(
                    state.message
                )
            }

            is VehicleReportPurchaseState.Purchased -> {
                onPurchaseConfirmed(
                    state.purchaseToken
                )
            }

            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Vehicle Report",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(56.dp)
                )

                Text(
                    text = "Full AI Vehicle Analysis",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Get a deeper analysis of the vehicle advert using our paid Gemini AI analysis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.45f
                        )
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = "Full report includes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "• Gemini AI advert analysis",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "• Deeper advert risk assessment",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "• Vehicle-specific observations",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "• Detailed buying guidance",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                when (val state = purchaseState) {

                    is VehicleReportPurchaseState.Available -> {

                        Button(
                            onClick = {
                                billingManager.launchPurchase(
                                    activity
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {

                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null
                            )

                            Spacer(
                                modifier = Modifier.height(0.dp)
                            )

                            Text(
                                text = "  Get Full AI Report — ${state.formattedPrice}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    VehicleReportPurchaseState.Loading -> {

                        CircularProgressIndicator()

                        Text(
                            text = "Loading report availability...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    VehicleReportPurchaseState.Purchasing -> {

                        CircularProgressIndicator()

                        Text(
                            text = "Processing purchase...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    VehicleReportPurchaseState.Pending -> {

                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Your payment is pending.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "The full AI report will become available once Google Play confirms the payment.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is VehicleReportPurchaseState.Purchased -> {

                        CircularProgressIndicator()

                        Text(
                            text = "Purchase confirmed.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Preparing your full AI vehicle report...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is VehicleReportPurchaseState.Error -> {

                        Text(
                            text = "Unable to load the AI report purchase.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedButton(
                            onClick = {
                                billingManager.refreshProduct()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Try Again",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    VehicleReportPurchaseState.Idle -> {

                        CircularProgressIndicator()

                        Text(
                            text = "Preparing secure payment...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Text(
                    text = "Payment is securely handled by Google Play.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}