package com.hiddenhistory.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hiddenhistory.models.MotTest
import com.hiddenhistory.models.Vehicle
import com.hiddenhistory.viewmodel.FreeVehicleSearchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeVehicleSearchScreen(
    onNavigateToMotDetails: (MotTest, Vehicle) -> Unit,
    onNavigateToSavedReports: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: FreeVehicleSearchViewModel =
        viewModel()
) {

    val uiState by
        viewModel.uiState.collectAsStateWithLifecycle()

    val isLoading by
        viewModel.isLoading.collectAsStateWithLifecycle()

    val parsedAdvert by
        viewModel.parsedAdvert.collectAsStateWithLifecycle()

    val mappedAdvertVehicle by
        viewModel.mappedAdvertVehicle.collectAsStateWithLifecycle()

    val currentVehicle by
        viewModel.currentVehicle.collectAsStateWithLifecycle()

    val officialCrossCheck by
        viewModel.officialCrossCheck.collectAsStateWithLifecycle()

    var universalInput by
        remember {
            mutableStateOf("")
        }

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    val coroutineScope =
        rememberCoroutineScope()

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "Free Vehicle Search",
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick =
                            onNavigateBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription =
                                "Back"
                        )
                    }
                }
            )
        },

        snackbarHost = {

            SnackbarHost(
                snackbarHostState
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
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            /*
             * =========================================================
             * UNIVERSAL INPUT
             * =========================================================
             */

            OutlinedTextField(

                value =
                    universalInput,

                onValueChange = {
                    universalInput = it
                },

                label = {

                    Text(
                        "Enter Reg, or Paste Advert And Reg..."
                    )
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp),

                shape =
                    RoundedCornerShape(12.dp),

                enabled =
                    !isLoading,

                colors =
                    OutlinedTextFieldDefaults.colors(

                        focusedBorderColor =
                            MaterialTheme.colorScheme.primary,

                        unfocusedBorderColor =
                            MaterialTheme.colorScheme
                                .outline
                                .copy(alpha = 0.5f),

                        focusedContainerColor =
                            MaterialTheme.colorScheme
                                .surfaceVariant
                                .copy(alpha = 0.3f),

                        unfocusedContainerColor =
                            MaterialTheme.colorScheme
                                .surfaceVariant
                                .copy(alpha = 0.3f)
                    )
            )

            /*
             * =========================================================
             * SINGLE SEARCH
             * =========================================================
             */

            Button(

                onClick = {

                    viewModel.processUniversalInput(
                        universalInput
                    )
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp),

                shape =
                    RoundedCornerShape(12.dp),

                enabled =
                    universalInput.isNotBlank() &&
                        !isLoading
            ) {

                if (isLoading) {

                    CircularProgressIndicator(

                        modifier =
                            Modifier.size(20.dp),

                        strokeWidth = 2.dp,

                        color =
                            MaterialTheme.colorScheme
                                .onPrimary
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Text(
                        "Processing..."
                    )

                } else {

                    Icon(
                        imageVector =
                            Icons.Default.Search,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.padding(
                                end = 8.dp
                            )
                    )

                    Text(
                        text =
                            "Search Vehicle",

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            /*
             * =========================================================
             * LOADING
             * =========================================================
             */

            if (isLoading) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(24.dp)
                            )

                            Text(
                                text =
                                    "Processing vehicle and advert data...",

                                style =
                                    MaterialTheme.typography
                                        .titleMedium
                            )
                        }

                        Text(
                            text =
                                "The advert is analysed and official " +
                                    "vehicle and MOT information is retrieved " +
                                    "for comparison.",

                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            /*
             * =========================================================
             * FULL FREE ADVERT ANALYSIS
             * =========================================================
             */

            parsedAdvert?.let { advert ->

                CollapsibleResultCard(
                    title =
                        "Free Advert Analysis",

                    initiallyExpanded =
                        true
                ) {

                    /*
                     * -------------------------------------------------
                     * ADVERT SUMMARY
                     * -------------------------------------------------
                     */

                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme
                                        .surfaceVariant
                            ),

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(16.dp)
                        ) {

                            Text(
                                text =
                                    "Advert Summary",

                                style =
                                    MaterialTheme.typography
                                        .titleMedium,

                                color =
                                    MaterialTheme.colorScheme
                                        .primary
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(6.dp)
                            )

                            Text(
                                text =
                                    advert.professionalSummary,

                                style =
                                    MaterialTheme.typography
                                        .bodyMedium
                            )
                        }
                    }

                    /*
                     * -------------------------------------------------
                     * ADVERT INFORMATION
                     * -------------------------------------------------
                     */

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(16.dp),

                            verticalArrangement =
                                Arrangement.spacedBy(5.dp)
                        ) {

                            Text(
                                text =
                                    "Advert Information",

                                style =
                                    MaterialTheme.typography
                                        .titleMedium
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "• Advert Health Index: " +
                                        "${advert.conditionScore}%"
                            )

                            Text(
                                text =
                                    "• Detected Dialect: " +
                                        advert.detectedDialect
                            )

                            Text(
                                text =
                                    "• Advert Year: " +
                                        "${advert.year ?: "Unknown"}"
                            )

                            Text(
                                text =
                                    "• Advert Mileage: " +
                                        "${advert.mileage ?: "Not Specified"}"
                            )

                            Text(
                                text =
                                    "• Advert Asking Price: " +
                                        "${advert.price ?: "Not Specified"}"
                            )

                            Text(
                                text =
                                    "• Advert Transmission: " +
                                        "${advert.transmission ?: "Unknown"}"
                            )

                            Text(
                                text =
                                    "• Advert Engine & Fuel: " +
                                        "${advert.engineSize ?: "Unknown"} " +
                                        "${advert.fuelType?.let { "($it)" } ?: ""}"
                            )
                        }
                    }

                    /*
                     * -------------------------------------------------
                     * RISK FLAGS
                     * -------------------------------------------------
                     */

                    if (advert.riskFlags.isNotEmpty()) {

                        Text(
                            text =
                                "⚠️ Advert Risk Warnings",

                            style =
                                MaterialTheme.typography.titleMedium,

                            color =
                                MaterialTheme.colorScheme.error
                        )

                        advert.riskFlags.forEach { flag ->

                            Surface(
                                color =
                                    MaterialTheme.colorScheme
                                        .errorContainer,

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 2.dp
                                        )
                            ) {

                                Text(
                                    text =
                                        "• $flag",

                                    color =
                                        MaterialTheme.colorScheme
                                            .onErrorContainer,

                                    modifier =
                                        Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    /*
                     * -------------------------------------------------
                     * KEY INSIGHTS
                     * -------------------------------------------------
                     */

                    if (advert.keyInsights.isNotEmpty()) {

                        Text(
                            text =
                                "✅ Advert Highlights",

                            style =
                                MaterialTheme.typography
                                    .titleMedium
                        )

                        advert.keyInsights.forEach { insight ->

                            Surface(
                                tonalElevation =
                                    2.dp,

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 2.dp
                                        )
                            ) {

                                Text(
                                    text =
                                        "• $insight",

                                    modifier =
                                        Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            /*
             * =========================================================
             * OFFICIAL ADVERT CROSS-CHECK
             * =========================================================
             */

            officialCrossCheck?.let { crossCheck ->

                CollapsibleResultCard(
                    title =
                        "Official Advert Cross-Check",

                    initiallyExpanded =
                        false
                ) {

                    Text(
                        text =
                            "These findings compare information stated " +
                                "in the advert against official vehicle " +
                                "and MOT records.",

                        style =
                            MaterialTheme.typography.bodySmall
                    )

                    if (crossCheck.warnings.isNotEmpty()) {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "⚠️ Discrepancies Found",

                            style =
                                MaterialTheme.typography
                                    .titleMedium,

                            color =
                                MaterialTheme.colorScheme.error
                        )

                        crossCheck.warnings.forEach { warning ->

                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 3.dp
                                        ),

                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            MaterialTheme.colorScheme
                                                .errorContainer
                                    )
                            ) {

                                Text(
                                    text =
                                        "• $warning",

                                    modifier =
                                        Modifier.padding(12.dp),

                                    color =
                                        MaterialTheme.colorScheme
                                            .onErrorContainer
                                )
                            }
                        }
                    }

                    if (crossCheck.confirmations.isNotEmpty()) {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "✓ Official Cross-Check Findings",

                            style =
                                MaterialTheme.typography
                                    .titleMedium
                        )

                        crossCheck.confirmations.forEach {
                            confirmation ->

                            Surface(
                                tonalElevation =
                                    2.dp,

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 3.dp
                                        )
                            ) {

                                Text(
                                    text =
                                        "• $confirmation",

                                    modifier =
                                        Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    if (crossCheck.verificationItems.isNotEmpty()) {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "🔎 Recommended Verification",

                            style =
                                MaterialTheme.typography
                                    .titleMedium
                        )

                        crossCheck.verificationItems.forEach {
                            item ->

                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 3.dp
                                        )
                            ) {

                                Text(
                                    text =
                                        "• $item",

                                    modifier =
                                        Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    if (
                        crossCheck.warnings.isEmpty() &&
                        crossCheck.confirmations.isEmpty() &&
                        crossCheck.verificationItems.isEmpty()
                    ) {

                        Card(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text =
                                    "No official cross-check findings " +
                                        "are available for the information " +
                                        "currently extracted from the advert.",

                                modifier =
                                    Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            /*
             * =========================================================
             * OFFICIAL VEHICLE INFORMATION
             * =========================================================
             */

            val sections =
                remember(uiState) {

                    VehicleSearchSectionParser.parse(
                        uiState
                    )
                }

            if (
                !isLoading &&
                sections.isNotEmpty()
            ) {

                CollapsibleResultCard(
                    title =
                        "Official Vehicle Information",

                    initiallyExpanded =
                        true
                ) {

                    Text(
                        text =
                            "This information comes from the official " +
                                "vehicle lookup and is separate from the " +
                                "seller's advert.",

                        style =
                            MaterialTheme.typography.bodySmall
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    sections.forEach { section ->

                        when (section) {

                            is UiSection.Header -> {

                                if (
                                    section.items.isNotEmpty()
                                ) {

                                    SectionHeaderCard(
                                        section =
                                            section
                                    )
                                }
                            }

                            is UiSection.MotTests -> {

                                MotHistorySection(

                                    tests =
                                        section.tests,

                                    onMotTestClick = { motTest ->

                                        /*
                                         * Keep the selected MOT test
                                         * inside the FreeVehicleSearchViewModel
                                         * as before.
                                         */

                                        viewModel.selectMotTest(
                                            motTest
                                        )

                                        /*
                                         * IMPORTANT:
                                         *
                                         * Pass the ACTUAL MotTest object
                                         * to MainActivity.
                                         *
                                         * MainActivity will then place the
                                         * same object into the shared
                                         * MotViewModel before navigating.
                                         */

                                        currentVehicle?.let { vehicle ->

                                            onNavigateToMotDetails(
                                                motTest,
                                                vehicle
                                            )
                                        }
                                    }
                                )
                            }

                            is UiSection.Symptoms -> {

                                SymptomsSection(
                                    symptoms =
                                        section.symptoms
                                )
                            }
                        }
                    }
                }
            }

            /*
             * =========================================================
             * ERROR FALLBACK
             * =========================================================
             */

            if (
                !isLoading &&
                uiState.isNotEmpty() &&
                sections.isEmpty()
            ) {

                uiState.forEach { item ->

                    if (item is String) {

                        Card(
                            modifier =
                                Modifier.fillMaxWidth(),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme.colorScheme
                                            .errorContainer
                                )
                        ) {

                            Text(
                                text =
                                    item,

                                modifier =
                                    Modifier.padding(16.dp),

                                color =
                                    MaterialTheme.colorScheme
                                        .onErrorContainer
                            )
                        }
                    }
                }
            }

            /*
             * =========================================================
             * NO OFFICIAL VEHICLE RESULT
             * =========================================================
             */

            if (
                !isLoading &&
                parsedAdvert != null &&
                uiState.isEmpty()
            ) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(6.dp)
                    ) {

                        Text(
                            text =
                                "Official Vehicle Lookup",

                            style =
                                MaterialTheme.typography
                                    .titleMedium
                        )

                        Text(
                            text =
                                "The advert was analysed successfully, " +
                                    "but no official vehicle result is " +
                                    "available because a registration " +
                                    "could not be used.",

                            style =
                                MaterialTheme.typography
                                    .bodyMedium
                        )
                    }
                }
            }

            /*
             * =========================================================
             * ACTION ROW
             * =========================================================
             */

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                if (uiState.isNotEmpty()) {

                    OutlinedButton(

                        onClick = {

                            viewModel.saveCurrentReport(

                                onSuccess = {

                                    coroutineScope.launch {

                                        snackbarHostState
                                            .showSnackbar(
                                                "Report saved successfully!"
                                            )
                                    }

                                    onNavigateToSavedReports()
                                },

                                onError = { errorMsg ->

                                    coroutineScope.launch {

                                        snackbarHostState
                                            .showSnackbar(
                                                "Failed to save: $errorMsg"
                                            )
                                    }
                                }
                            )
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(40.dp),

                        shape =
                            RoundedCornerShape(10.dp),

                        colors =
                            ButtonDefaults
                                .outlinedButtonColors(

                                    contentColor =
                                        MaterialTheme.colorScheme
                                            .primary
                                )
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Favorite,

                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(14.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(4.dp)
                        )

                        Text(
                            text =
                                "Save Report",

                            style =
                                MaterialTheme.typography
                                    .labelMedium,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }

            /*
             * =========================================================
             * EMPTY STATE
             * =========================================================
             */

            if (
                uiState.isEmpty() &&
                !isLoading &&
                parsedAdvert == null
            ) {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 32.dp
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally,

                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Search,

                            contentDescription =
                                null,

                            tint =
                                MaterialTheme.colorScheme
                                    .outline,

                            modifier =
                                Modifier.size(48.dp)
                        )

                        Text(
                            text =
                                "Enter a registration, advert, link, " +
                                    "or vehicle description.",

                            style =
                                MaterialTheme.typography.bodyMedium,

                            color =
                                MaterialTheme.colorScheme
                                    .outline,

                            fontWeight =
                                FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}


/*
 * =====================================================================
 * COLLAPSIBLE RESULT CARD
 * =====================================================================
 */

@Composable
private fun CollapsibleResultCard(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {

    var expanded by remember(
        title,
        initiallyExpanded
    ) {
        mutableStateOf(
            initiallyExpanded
        )
    }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .animateContentSize()
    ) {

        Column(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            expanded =
                                !expanded
                        }
                        .padding(16.dp),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        title,

                    style =
                        MaterialTheme.typography.titleLarge
                )

                Text(
                    text =
                        if (expanded) {
                            "▲"
                        } else {
                            "▼"
                        },

                    style =
                        MaterialTheme.typography.titleMedium,

                    color =
                        MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                visible =
                    expanded
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp
                            ),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp),

                    content =
                        content
                )
            }
        }
    }
}