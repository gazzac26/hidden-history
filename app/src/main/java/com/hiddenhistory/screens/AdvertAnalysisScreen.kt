package com.hiddenhistory.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hiddenhistory.viewmodel.AdvertAnalysisUiState
import com.hiddenhistory.viewmodel.AdvertAnalysisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvertAnalysisScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDvlaLookup: (String) -> Unit,
    viewModel: AdvertAnalysisViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var advertInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analyse Vehicle Advert",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            /*
             * =========================================================
             * INPUT SECTION
             * =========================================================
             */

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(16.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant
                                    .copy(alpha = 0.35f)
                        ),

                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                ) {

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text =
                                "Paste Advert Link or Text",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )

                        OutlinedTextField(

                            value =
                                advertInput,

                            onValueChange = {
                                advertInput = it
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),

                            placeholder = {
                                Text(
                                    "Paste marketplace description or URL here..."
                                )
                            },

                            enabled =
                                uiState !is
                                    AdvertAnalysisUiState.Analysing,

                            shape =
                                RoundedCornerShape(12.dp)
                        )

                        Button(

                            onClick = {
                                viewModel.analyzeAdvert(
                                    advertInput
                                )
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            enabled =
                                advertInput.isNotBlank() &&
                                    uiState !is
                                        AdvertAnalysisUiState.Analysing,

                            shape =
                                RoundedCornerShape(12.dp)
                        ) {

                            if (
                                uiState is
                                    AdvertAnalysisUiState.Analysing
                            ) {

                                CircularProgressIndicator(

                                    modifier =
                                        Modifier.size(20.dp),

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onPrimary,

                                    strokeWidth = 2.dp
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(8.dp)
                                )

                                Text(
                                    "Analysing Advert..."
                                )

                            } else {

                                Icon(
                                    Icons.Default.Analytics,
                                    contentDescription = null,
                                    modifier =
                                        Modifier.size(18.dp)
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(8.dp)
                                )

                                Text(
                                    "ANALYSE"
                                )
                            }
                        }
                    }
                }
            }

            /*
             * =========================================================
             * ERROR STATE
             * =========================================================
             */

            if (
                uiState is
                    AdvertAnalysisUiState.Error
            ) {

                item {

                    Card(

                        modifier =
                            Modifier.fillMaxWidth(),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .errorContainer
                                        .copy(alpha = 0.4f)
                            ),

                        shape =
                            RoundedCornerShape(16.dp)
                    ) {

                        Text(

                            text =
                                (
                                    uiState as
                                        AdvertAnalysisUiState.Error
                                ).message,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,

                            modifier =
                                Modifier.padding(16.dp)
                        )
                    }
                }
            }

            /*
             * =========================================================
             * SUCCESS STATE
             * =========================================================
             */

            if (
                uiState is
                    AdvertAnalysisUiState.Success
            ) {

                val analysis =
                    (
                        uiState as
                            AdvertAnalysisUiState.Success
                    ).analysis

                /*
                 * -----------------------------------------------------
                 * REGISTRATION DETECTION / DVLA BRIDGE
                 * -----------------------------------------------------
                 */

                analysis.registrationNumber?.let { reg ->

                    item {

                        Card(

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(16.dp),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .primaryContainer
                                            .copy(alpha = 0.4f)
                                ),

                            elevation =
                                CardDefaults.cardElevation(
                                    defaultElevation = 0.dp
                                )
                        ) {

                            Column(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),

                                verticalArrangement =
                                    Arrangement.spacedBy(10.dp)
                            ) {

                                Row(

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    horizontalArrangement =
                                        Arrangement.SpaceBetween,

                                    verticalAlignment =
                                        androidx.compose.ui
                                            .Alignment
                                            .CenterVertically
                                ) {

                                    Column(
                                        verticalArrangement =
                                            Arrangement.spacedBy(2.dp)
                                    ) {

                                        Text(

                                            text =
                                                "Registration Detected",

                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .labelSmall,

                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .primary
                                        )

                                        Text(

                                            text =
                                                reg,

                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .titleLarge,

                                            fontWeight =
                                                FontWeight.Bold,

                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onPrimaryContainer
                                        )
                                    }
                                }

                                Button(

                                    onClick = {
                                        onNavigateToDvlaLookup(
                                            reg
                                        )
                                    },

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    shape =
                                        RoundedCornerShape(10.dp)
                                ) {

                                    Icon(

                                        Icons.Default.DirectionsCar,

                                        contentDescription =
                                            null,

                                        modifier =
                                            Modifier.size(18.dp)
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.width(8.dp)
                                    )

                                    Text(
                                        "Lookup Official DVLA / MOT Data"
                                    )
                                }
                            }
                        }
                    }
                }

                /*
                 * -----------------------------------------------------
                 * ANALYSIS HEADER
                 * -----------------------------------------------------
                 */

                item {

                    HorizontalDivider(
                        modifier =
                            Modifier.padding(
                                vertical = 4.dp
                            )
                    )

                    Text(

                        text =
                            "ANALYSIS",

                        style =
                            MaterialTheme
                                .typography
                                .labelLarge,

                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,

                        fontWeight =
                            FontWeight.Bold
                    )
                }

                /*
                 * -----------------------------------------------------
                 * CORE VEHICLE DETAILS
                 * -----------------------------------------------------
                 */

                item {

                    Card(

                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(16.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                                        .copy(alpha = 0.35f)
                            ),

                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation = 0.dp
                            )
                    ) {

                        Column(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),

                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            analysis.advertTitle?.let {

                                Text(

                                    text =
                                        it,

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,

                                    fontWeight =
                                        FontWeight.Bold,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface
                                )
                            }

                            analysis.price?.let {

                                BulletDetailRow(
                                    label = "Advert price:",
                                    value = it
                                )
                            }

                            analysis.mileage?.let {

                                BulletDetailRow(
                                    label = "Mileage:",
                                    value = it
                                )
                            }

                            analysis.vehicleDetails
                                ?.forEach { (key, value) ->

                                    BulletDetailRow(
                                        label = "$key:",
                                        value = value
                                    )
                                }
                        }
                    }
                }

                /*
                 * -----------------------------------------------------
                 * SELLER INFORMATION
                 * -----------------------------------------------------
                 */

                analysis.sellerInformation?.let { sellerInfo ->

                    item {

                        AnalysisSectionCard(
                            title =
                                "WHAT THE SELLER IS SAYING"
                        ) {

                            Text(

                                text =
                                    sellerInfo,

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }
                }

                /*
                 * -----------------------------------------------------
                 * CLAIMS
                 * -----------------------------------------------------
                 */

                if (
                    analysis.claimsMadeBySeller
                        .isNotEmpty()
                ) {

                    item {

                        AnalysisListSectionCard(

                            title =
                                "CLAIMS MADE BY SELLER",

                            items =
                                analysis.claimsMadeBySeller
                        )
                    }
                }

                /*
                 * -----------------------------------------------------
                 * NOTABLE WORDING
                 * -----------------------------------------------------
                 */

                if (
                    analysis.notableWording
                        .isNotEmpty()
                ) {

                    item {

                        AnalysisListSectionCard(

                            title =
                                "NOTABLE WORDING",

                            items =
                                analysis.notableWording
                        )
                    }
                }

                /*
                 * -----------------------------------------------------
                 * THINGS TO CHECK
                 * -----------------------------------------------------
                 */

                if (
                    analysis.thingsWorthVerifying
                        .isNotEmpty()
                ) {

                    item {

                        AnalysisListSectionCard(

                            title =
                                "THINGS TO CHECK",

                            items =
                                analysis.thingsWorthVerifying
                        )
                    }
                }

                /*
                 * -----------------------------------------------------
                 * MISSING INFORMATION
                 * -----------------------------------------------------
                 */

                if (
                    analysis.missingInformation
                        .isNotEmpty()
                ) {

                    item {

                        AnalysisListSectionCard(

                            title =
                                "MISSING INFORMATION",

                            items =
                                analysis.missingInformation
                        )
                    }
                }

                /*
                 * -----------------------------------------------------
                 * INCONSISTENCIES
                 * -----------------------------------------------------
                 */

                if (
                    analysis.inconsistencies
                        .isNotEmpty()
                ) {

                    item {

                        AnalysisListSectionCard(

                            title =
                                "INCONSISTENCIES FOUND",

                            items =
                                analysis.inconsistencies
                        )
                    }
                }

                /*
                 * -----------------------------------------------------
                 * QUESTIONS TO ASK
                 * -----------------------------------------------------
                 */

                if (
                    analysis.questionsTheBuyerShouldAsk
                        .isNotEmpty()
                ) {

                    item {

                        AnalysisListSectionCard(

                            title =
                                "QUESTIONS TO ASK THE SELLER",

                            items =
                                analysis.questionsTheBuyerShouldAsk
                        )
                    }
                }

                /*
                 * -----------------------------------------------------
                 * OVERALL SUMMARY
                 * -----------------------------------------------------
                 */

                analysis.overallSummary?.let { summary ->

                    item {

                        AnalysisSectionCard(
                            title =
                                "OVERALL SUMMARY"
                        ) {

                            Text(

                                text =
                                    summary,

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}


/*
 * =====================================================================
 * BULLET DETAIL ROW
 * =====================================================================
 */

@Composable
private fun BulletDetailRow(
    label: String,
    value: String
) {

    Column(
        verticalArrangement =
            Arrangement.spacedBy(2.dp)
    ) {

        Text(

            text =
                label,

            style =
                MaterialTheme
                    .typography
                    .labelSmall,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Text(

            text =
                value,

            style =
                MaterialTheme
                    .typography
                    .bodyLarge,

            fontWeight =
                FontWeight.SemiBold,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )
    }
}


/*
 * =====================================================================
 * ANALYSIS SECTION CARD
 * =====================================================================
 */

@Composable
private fun AnalysisSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                        .copy(alpha = 0.35f)
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Text(

                text =
                    title,

                style =
                    MaterialTheme
                        .typography
                        .titleSmall,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            content()
        }
    }
}


/*
 * =====================================================================
 * ANALYSIS LIST SECTION CARD
 * =====================================================================
 */

@Composable
private fun AnalysisListSectionCard(
    title: String,
    items: List<String>
) {

    AnalysisSectionCard(
        title = title
    ) {

        items.forEach { item ->

            Text(

                text =
                    "• $item",

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}