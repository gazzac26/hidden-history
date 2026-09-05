package com.hiddenhistory.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hiddenhistory.viewmodel.AdvertAnalyzerViewModel
import com.hiddenhistory.viewmodel.FreeVehicleSearchViewModel

@Composable
fun AdvertAnalyzerScreen(
    initialAdvertText: String? = null,
    advertViewModel: AdvertAnalyzerViewModel = viewModel(),
    vehicleViewModel: FreeVehicleSearchViewModel = viewModel()
) {

    /*
     * =========================================================
     * RAW ADVERT INPUT & SEARCH TYPE STATE
     * =========================================================
     */

    var rawInput by remember {
        mutableStateOf(
            initialAdvertText.orEmpty()
        )
    }

    var selectedSearchType by remember {
        mutableStateOf(FreeSearchType.ADVERT)
    }

    /*
     * =========================================================
     * FREE ADVERT ANALYSIS
     * =========================================================
     */

    val parsedAdvert by advertViewModel.parsedResult
        .collectAsStateWithLifecycle()

    /*
     * =========================================================
     * OFFICIAL CROSS-CHECK
     * =========================================================
     */

    val officialCrossCheck by advertViewModel.officialCrossCheck
        .collectAsStateWithLifecycle()

    /*
     * =========================================================
     * OFFICIAL VEHICLE SEARCH
     * =========================================================
     */

    val uiState by vehicleViewModel.uiState
        .collectAsStateWithLifecycle()

    val isLoading by vehicleViewModel.isLoading
        .collectAsStateWithLifecycle()

    /*
     * =========================================================
     * INITIAL ADVERT TEXT
     * =========================================================
     */

    LaunchedEffect(initialAdvertText) {

        if (!initialAdvertText.isNullOrBlank()) {

            rawInput = initialAdvertText

            advertViewModel.analyzeAdvert(
                initialAdvertText
            )

            vehicleViewModel.processUniversalInput(
                input = initialAdvertText,
                searchType = FreeSearchType.REGISTRATION_AND_ADVERT
            )
        }
    }

    /*
     * =========================================================
     * SECTION PARSING
     * =========================================================
     */

    val sections = remember(uiState) {

        VehicleSearchSectionParser.parse(
            uiState
        )
    }

    /*
     * =========================================================
     * OFFICIAL CROSS-CHECK TRIGGER
     * =========================================================
     */

    LaunchedEffect(sections, parsedAdvert) {

        if (parsedAdvert == null) {
            return@LaunchedEffect
        }

        val motTests =
            sections
                .filterIsInstance<UiSection.MotTests>()
                .flatMap {
                    it.tests
                }

        if (motTests.isNotEmpty()) {

            advertViewModel.crossCheckAgainstOfficialMot(
                motTests
            )
        }
    }

    /*
     * =========================================================
     * SCREEN
     * =========================================================
     */

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(
                rememberScrollState()
            ),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        /*
         * =========================================================
         * TITLE
         * =========================================================
         */

        Text(
            text = "Free Vehicle Advert Analyzer",
            style = MaterialTheme.typography.headlineMedium
        )

        /*
         * =========================================================
         * SEARCH TYPE SELECTOR CARDS
         * =========================================================
         */

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedSearchType == FreeSearchType.ADVERT) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            onClick = { selectedSearchType = FreeSearchType.ADVERT }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(
                    selected = selectedSearchType == FreeSearchType.ADVERT,
                    onClick = { selectedSearchType = FreeSearchType.ADVERT }
                )
                Column {
                    Text(
                        text = "Advert Search",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Advert Analysis",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedSearchType == FreeSearchType.REGISTRATION_AND_ADVERT) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            onClick = { selectedSearchType = FreeSearchType.REGISTRATION_AND_ADVERT }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(
                    selected = selectedSearchType == FreeSearchType.REGISTRATION_AND_ADVERT,
                    onClick = { selectedSearchType = FreeSearchType.REGISTRATION_AND_ADVERT }
                )
                Column {
                    Text(
                        text = "Registration + Advert",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Official Vehicle Data Sources + Analysis",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        /*
         * =========================================================
         * ADVERT INPUT
         * =========================================================
         */

        OutlinedTextField(

            value = rawInput,

            onValueChange = {
                rawInput = it
            },

            label = {
                Text(
                    "Paste Advert Text or Link..."
                )
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),

            enabled = !isLoading
        )

        /*
         * =========================================================
         * ANALYSE BUTTON
         * =========================================================
         */

        Button(

            onClick = {
                if (selectedSearchType == FreeSearchType.ADVERT) {
                    advertViewModel.analyzeAdvert(rawInput)
                }
                vehicleViewModel.processUniversalInput(
                    input = rawInput,
                    searchType = selectedSearchType
                )
            },

            modifier = Modifier.fillMaxWidth(),

            enabled =
                rawInput.isNotBlank() &&
                    !isLoading

        ) {

            if (isLoading) {

                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    "Processing..."
                )

            } else {

                Text(
                    "Search Vehicle"
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
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )

                        Text(
                            text =
                                "Processing advert and vehicle data...",
                            style =
                                MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        text =
                            "The advert is being analysed and any " +
                                "official vehicle information is being " +
                                "retrieved for comparison.",
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        /*
         * =========================================================
         * FREE ADVERT ANALYSIS
         * =========================================================
         */

        parsedAdvert?.let { advert ->

            CollapsibleResultCard(
                title = "Free Advert Analysis",
                initiallyExpanded = true
            ) {

                /*
                 * -------------------------------------------------
                 * ADVERT SUMMARY
                 * -------------------------------------------------
                 */

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "Advert Summary",
                            style =
                                MaterialTheme.typography.titleMedium,
                            color =
                                MaterialTheme.colorScheme.primary
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                advert.professionalSummary,
                            style =
                                MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                /*
                 * -------------------------------------------------
                 * ADVERT INFORMATION
                 * -------------------------------------------------
                 */

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(5.dp)
                    ) {

                        Text(
                            text = "Advert Information",
                            style =
                                MaterialTheme.typography.titleMedium
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "• Advert Health Index: " +
                                    "${advert.conditionScore}%"
                        )

                        Text(
                            text =
                                "• Detected Dialect: " +
                                    "${advert.detectedDialect}"
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
                 * ADVERT RISK FLAGS
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
                                    .padding(vertical = 2.dp)
                        ) {

                            Text(
                                text = "• $flag",
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
                 * POSITIVE ADVERT INSIGHTS
                 * -------------------------------------------------
                 */

                if (advert.keyInsights.isNotEmpty()) {

                    Text(
                        text =
                            "✅ Advert Highlights",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    advert.keyInsights.forEach { insight ->

                        Surface(
                            tonalElevation = 2.dp,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                        ) {

                            Text(
                                text = "• $insight",
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
                title = "Official Advert Cross-Check",
                initiallyExpanded = false
            ) {

                Text(
                    text =
                        "The following findings compare information " +
                            "stated in the advert against official " +
                            "vehicle records.",
                    style =
                        MaterialTheme.typography.bodySmall
                )

                /*
                 * -------------------------------------------------
                 * WARNINGS
                 * -------------------------------------------------
                 */

                if (crossCheck.warnings.isNotEmpty()) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "⚠️ Discrepancies Found",
                        style =
                            MaterialTheme.typography.titleMedium,
                        color =
                            MaterialTheme.colorScheme.error
                    )

                    crossCheck.warnings.forEach { warning ->

                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme.colorScheme
                                            .errorContainer
                                )
                        ) {

                            Text(
                                text = "• $warning",
                                modifier =
                                    Modifier.padding(12.dp),
                                color =
                                    MaterialTheme.colorScheme
                                        .onErrorContainer
                            )
                        }
                    }
                }

                /*
                 * -------------------------------------------------
                 * CONFIRMATIONS
                 * -------------------------------------------------
                 */

                if (crossCheck.confirmations.isNotEmpty()) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "✓ Official Cross-Check Findings",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    crossCheck.confirmations.forEach { confirmation ->

                        Surface(
                            tonalElevation = 2.dp,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                        ) {

                            Text(
                                text = "• $confirmation",
                                modifier =
                                    Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                /*
                 * -------------------------------------------------
                 * VERIFICATION ITEMS
                 * -------------------------------------------------
                 */

                if (crossCheck.verificationItems.isNotEmpty()) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "🔎 Recommended Verification",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    crossCheck.verificationItems.forEach { item ->

                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                        ) {

                            Text(
                                text = "• $item",
                                modifier =
                                    Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                /*
                 * -------------------------------------------------
                 * NOTHING FOUND
                 * -------------------------------------------------
                 */

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
                                "No official cross-check findings are " +
                                    "available for the information " +
                                    "currently extracted from the advert.",
                            modifier =
                                Modifier.padding(12.dp),
                            style =
                                MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        /*
         * =========================================================
         * OFFICIAL VEHICLE RESULT
         * =========================================================
         */

        if (!isLoading && sections.isNotEmpty()) {

            CollapsibleResultCard(
                title = "Official Vehicle Information",
                initiallyExpanded = false
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
                    modifier = Modifier.height(4.dp)
                )

                sections.forEach { section ->

                    when (section) {

                        /*
                         * -------------------------------------------------
                         * VEHICLE INFORMATION
                         * -------------------------------------------------
                         */

                        is UiSection.Header -> {

                            if (section.items.isNotEmpty()) {

                                SectionHeaderCard(
                                    section = section
                                )
                            }
                        }

                        /*
                         * -------------------------------------------------
                         * MOT HISTORY
                         * -------------------------------------------------
                         */

                        is UiSection.MotTests -> {

                            MotHistorySection(

                                tests = section.tests,

                                onMotTestClick = { motTest ->

                                    vehicleViewModel.selectMotTest(
                                        motTest
                                    )
                                }
                            )
                        }

                        /*
                         * -------------------------------------------------
                         * ACTIVE SYMPTOMS
                         * -------------------------------------------------
                         */

                        is UiSection.Symptoms -> {

                            SymptomsSection(
                                symptoms = section.symptoms
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
                            text = item,
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
                            MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text =
                            "No official vehicle result is available " +
                                "yet. The advert was analysed successfully, " +
                                "but a vehicle registration could not be " +
                                "used to retrieve the official vehicle data.",
                        style =
                            MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        /*
         * =========================================================
         * COMPLETELY EMPTY STATE
         * =========================================================
         */

        if (
            !isLoading &&
            rawInput.isNotBlank() &&
            parsedAdvert == null &&
            uiState.isEmpty()
        ) {

            Text(
                text =
                    "No analysis result has been returned yet.",
                style =
                    MaterialTheme.typography.bodyMedium
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded = !expanded
                    }
                    .padding(16.dp),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = title,
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
                visible = expanded
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
                    content = content
                )
            }
        }
    }
}
