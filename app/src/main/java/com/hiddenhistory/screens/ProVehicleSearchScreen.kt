package com.hiddenhistory.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hiddenhistory.viewmodel.ProVehicleSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProVehicleSearchScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProVehicleSearchViewModel =
        viewModel()
) {

    val uiState by
        viewModel.uiState
            .collectAsStateWithLifecycle()

    val isLoading by
        viewModel.isLoading
            .collectAsStateWithLifecycle()

    val currentVehicle by
        viewModel.currentVehicle
            .collectAsStateWithLifecycle()

    val advertAnalysis by
        viewModel.advertAnalysis
            .collectAsStateWithLifecycle()

    val isSaving by
        viewModel.isSaving
            .collectAsStateWithLifecycle()

    val saveMessage by
        viewModel.saveMessage
            .collectAsStateWithLifecycle()

    val snackbarHostState =
        remember { SnackbarHostState() }

    LaunchedEffect(saveMessage) {
        saveMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSaveMessage()
        }
    }

    var universalInput by
        remember {
            mutableStateOf("")
        }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text =
                            "Pro Vehicle Search",

                        fontWeight =
                            FontWeight.Bold
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
                },

                actions = {
                    if (!isLoading && (uiState.isNotEmpty() || advertAnalysis != null)) {
                        IconButton(
                            onClick = {
                                viewModel.saveCurrentReport()
                            },
                            enabled = !isSaving
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Default.BookmarkAdd,

                                contentDescription =
                                    "Save Report"
                            )
                        }
                    }
                }
            )
        }

    ) { innerPadding ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    )
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            /*
             * =========================================================
             * PRO SEARCH HEADER
             * =========================================================
             */

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(
                        18.dp
                    ),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                16.dp
                            ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Lock,

                            contentDescription =
                                null,

                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )

                        Text(
                            text =
                                "Pro Vehicle Intelligence",

                            modifier =
                                Modifier.padding(
                                    start = 10.dp
                                ),

                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Text(
                        text =
                            "Use a vehicle registration or paste a vehicle advert containing the registration. Pro Search uses the full vehicle intelligence pathway and Gemini-powered advert analysis.",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }
            }

            /*
             * =========================================================
             * UNIVERSAL INPUT
             * =========================================================
             */

            OutlinedTextField(

                value =
                    universalInput,

                onValueChange = {
                    universalInput =
                        it
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            120.dp
                        ),

                label = {

                    Text(
                        "Enter Reg or paste vehicle advert..."
                    )
                },

                placeholder = {

                    Text(
                        "Example: AB12 CDE or paste the full advert text containing the registration."
                    )
                },

                enabled =
                    !isLoading,

                shape =
                    RoundedCornerShape(
                        12.dp
                    )
            )

            /*
             * =========================================================
             * SEARCH BUTTON
             * =========================================================
             */

            Button(

                onClick = {

                    viewModel
                        .processUniversalInput(
                            universalInput
                        )
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            52.dp
                        ),

                enabled =
                    universalInput.isNotBlank() &&
                        !isLoading,

                shape =
                    RoundedCornerShape(
                        14.dp
                    )
            ) {

                if (
                    isLoading
                ) {

                    CircularProgressIndicator(

                        modifier =
                            Modifier.size(
                                20.dp
                            ),

                        strokeWidth =
                            2.dp,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onPrimary
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                8.dp
                            )
                    )

                    Text(
                        "Running Pro Search..."
                    )

                } else {

                    Icon(
                        imageVector =
                            Icons.Default.Search,

                        contentDescription =
                            null
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                8.dp
                            )
                    )

                    Text(
                        text =
                            "Start Pro Search",

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            /*
             * =========================================================
             * SAVE REPORT BUTTON (Styled)
             * =========================================================
             */

            if (
                !isLoading &&
                (uiState.isNotEmpty() || advertAnalysis != null)
            ) {

                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),

                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),

                    color =
                        MaterialTheme
                            .colorScheme
                            .surface,

                    tonalElevation =
                        1.dp,

                    shadowElevation =
                        2.dp
                ) {

                    OutlinedButton(

                        onClick = {
                            viewModel
                                .saveCurrentReport()
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(2.dp),

                        enabled =
                            !isSaving,

                        shape =
                            RoundedCornerShape(
                                12.dp
                            ),

                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                                        .copy(
                                            alpha = 0.25f
                                        ),

                                contentColor =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            ),

                        border =
                            ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp
                            )
                    ) {

                        if (
                            isSaving
                        ) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(
                                        20.dp
                                    ),

                                strokeWidth =
                                    2.dp,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(
                                        8.dp
                                    )
                            )

                            Text(
                                text =
                                    "Saving Report...",

                                fontWeight =
                                    FontWeight.SemiBold
                            )

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Default.BookmarkAdd,

                                contentDescription =
                                    null,

                                modifier =
                                    Modifier.size(
                                        18.dp
                                    ),

                                tint =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(
                                        8.dp
                                    )
                            )

                            Text(
                                text =
                                    "Save Report",

                                fontWeight =
                                    FontWeight.SemiBold,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )
                        }
                    }
                }
            }


            /*
             * =========================================================
             * LOADING
             * =========================================================
             */

            if (
                isLoading
            ) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                16.dp
                            ),

                        verticalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    12.dp
                                ),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(
                                        24.dp
                                    )
                            )

                            Text(
                                text =
                                    "Processing Pro vehicle intelligence...",

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium
                            )
                        }

                        Text(
                            text =
                                "Official vehicle data and, where supplied, the vehicle advert are being analysed.",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
                }
            }

            /*
             * =========================================================
             * PRO ADVERT ANALYSIS
             * =========================================================
             */

            advertAnalysis?.let { analysis ->

                CollapsibleProResultCard(

                    title =
                        "Pro Advert Analysis",

                    initiallyExpanded =
                        true
                ) {

                    /*
                     * -------------------------------------------------
                     * ADVERT TITLE
                     * -------------------------------------------------
                     */

                    analysis.advertTitle?.let { title ->

                        Card(
                            modifier =
                                Modifier.fillMaxWidth(),

                            colors =
                                CardDefaults
                                    .cardColors(
                                        containerColor =
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant
                                    )
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(
                                        16.dp
                                    )
                            ) {

                                Text(
                                    text =
                                        "Advert",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelMedium,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            4.dp
                                        )
                                )

                                Text(
                                    text =
                                        title,

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,

                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }
                    }

                    /*
                     * -------------------------------------------------
                     * REGISTRATION
                     * -------------------------------------------------
                     */

                    analysis.registrationNumber?.let { reg ->

                        ProAnalysisDetailCard(
                            title =
                                "Registration",

                            value =
                                reg
                        )
                    }

                    /*
                     * -------------------------------------------------
                     * PRICE
                     * -------------------------------------------------
                     */

                    analysis.price?.let { price ->

                        ProAnalysisDetailCard(
                            title =
                                "Advert Price",

                            value =
                                price
                        )
                    }

                    /*
                     * -------------------------------------------------
                     * MILEAGE
                     * -------------------------------------------------
                     */

                    analysis.mileage?.let { mileage ->

                        ProAnalysisDetailCard(
                            title =
                                "Advert Mileage",

                            value =
                                mileage
                        )
                    }

                    /*
                     * -------------------------------------------------
                     * VEHICLE DETAILS
                     * -------------------------------------------------
                     */

                    analysis.vehicleDetails
                        ?.takeIf {
                            it.isNotEmpty()
                        }
                        ?.let { details ->

                            ProAnalysisListCard(
                                title =
                                    "Vehicle Details",

                                items =
                                    details.map {
                                        "${it.key}: ${it.value}"
                                    }
                            )
                        }

                    /*
                     * -------------------------------------------------
                     * SELLER INFORMATION
                     * -------------------------------------------------
                     */

                    analysis.sellerInformation
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let { sellerInformation ->

                            ProAnalysisTextCard(
                                title =
                                    "What The Seller Is Saying",

                                text =
                                    sellerInformation
                            )
                        }

                    /*
                     * -------------------------------------------------
                     * CLAIMS
                     * -------------------------------------------------
                     */

                    if (
                        analysis
                            .claimsMadeBySeller
                            .isNotEmpty()
                    ) {

                        ProAnalysisListCard(
                            title =
                                "Claims Made By Seller",

                            items =
                                analysis
                                    .claimsMadeBySeller
                        )
                    }

                    /*
                     * -------------------------------------------------
                     * NOTABLE WORDING
                     * -------------------------------------------------
                     */

                    if (
                        analysis
                            .notableWording
                            .isNotEmpty()
                    ) {

                        ProAnalysisListCard(
                            title =
                                "Notable Wording",

                            items =
                                analysis
                                    .notableWording
                        )
                    }

                    /*
                     * -------------------------------------------------
                     * THINGS TO VERIFY
                     * -------------------------------------------------
                     */

                    if (
                        analysis
                            .thingsWorthVerifying
                            .isNotEmpty()
                    ) {

                        ProAnalysisListCard(
                            title =
                                "Things To Check",

                            items =
                                analysis
                                    .thingsWorthVerifying
                        )
                    }

                    /*
                     * -------------------------------------------------
                     * MISSING INFORMATION
                     * -------------------------------------------------
                     */

                    if (
                        analysis
                            .missingInformation
                            .isNotEmpty()
                    ) {

                        ProAnalysisListCard(
                            title =
                                "Missing Information",

                            items =
                                analysis
                                    .missingInformation
                        )
                    }

                    /*
                     * -------------------------------------------------
                     * INCONSISTENCIES
                     * -------------------------------------------------
                     */

                    if (
                        analysis
                            .inconsistencies
                            .isNotEmpty()
                    ) {

                        ProAnalysisListCard(
                            title =
                                "Inconsistencies Found",

                            items =
                                analysis
                                    .inconsistencies
                        )
                    }

                    /*
                     * -------------------------------------------------
                     * QUESTIONS
                     * -------------------------------------------------
                     */

                    if (
                        analysis
                            .questionsTheBuyerShouldAsk
                            .isNotEmpty()
                    ) {

                        ProAnalysisListCard(
                            title =
                                "Questions To Ask The Seller",

                            items =
                                analysis
                                    .questionsTheBuyerShouldAsk
                        )
                    }

                    /*
                     * -------------------------------------------------
                     * OVERALL SUMMARY
                     * -------------------------------------------------
                     */

                    analysis.overallSummary
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let { summary ->

                            ProAnalysisTextCard(
                                title =
                                    "Overall Summary",

                                text =
                                    summary
                            )
                        }
                }
            }

            /*
             * =========================================================
             * OFFICIAL VEHICLE INFORMATION
             * =========================================================
             */

            val sections =
                remember(
                    uiState
                ) {

                    VehicleSearchSectionParser.parse(
                        uiState
                    )
                }

            if (
                !isLoading &&
                sections.isNotEmpty()
            ) {

                CollapsibleProResultCard(

                    title =
                        "Official Vehicle Information",

                    initiallyExpanded =
                        true
                ) {

                    Text(
                        text =
                            "This information comes from the official vehicle lookup through the existing smooth-handler pathway.",

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp
                            )
                    )

                    sections.forEach { section ->

                        when (
                            section
                        ) {

                            is UiSection.Header -> {

                                if (
                                    section.items
                                        .isNotEmpty()
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

                                    onMotTestClick = {
                                        motTest ->

                                        viewModel
                                            .selectMotTest(
                                                motTest
                                            )
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
             * ADVERT WITHOUT REGISTRATION
             * =========================================================
             */

            if (
                !isLoading &&
                advertAnalysis != null &&
                currentVehicle == null
            ) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                16.dp
                            ),

                        verticalArrangement =
                            Arrangement.spacedBy(
                                6.dp
                            )
                    ) {

                        Text(
                            text =
                                "Official Vehicle Lookup",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )

                        Text(
                            text =
                                "The advert was analysed, but an official vehicle lookup could not be performed because a registration was not available.",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium
                        )
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

                    if (
                        item is String
                    ) {

                        Card(
                            modifier =
                                Modifier.fillMaxWidth(),

                            colors =
                                CardDefaults
                                    .cardColors(
                                        containerColor =
                                            MaterialTheme
                                                .colorScheme
                                                .errorContainer
                                    )
                        ) {

                            Text(
                                text =
                                    item,

                                modifier =
                                    Modifier.padding(
                                        16.dp
                                    ),

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onErrorContainer
                            )
                        }
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
                advertAnalysis == null &&
                !isLoading
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
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Search,

                            contentDescription =
                                null,

                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .outline,

                            modifier =
                                Modifier.size(
                                    48.dp
                                )
                        )

                        Text(
                            text =
                                "Enter a registration or paste a vehicle advert containing the registration.",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,

                            color =
                                MaterialTheme
                                    .colorScheme
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
 * COLLAPSIBLE PRO RESULT CARD
 * =====================================================================
 */

@Composable
private fun CollapsibleProResultCard(
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
                        .padding(
                            16.dp
                        ),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        title,

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )

                Text(
                    text =
                        if (
                            expanded
                        ) {
                            "▲"
                        } else {
                            "▼"
                        },

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
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
                        Arrangement.spacedBy(
                            10.dp
                        ),

                    content =
                        content
                )
            }
        }
    }
}


/*
 * =====================================================================
 * PRO ANALYSIS DETAIL CARD
 * =====================================================================
 */

@Composable
private fun ProAnalysisDetailCard(
    title: String,
    value: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(
                    12.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    3.dp
                )
        ) {

            Text(
                text =
                    title,

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
                    FontWeight.SemiBold
            )
        }
    }
}


/*
 * =====================================================================
 * PRO ANALYSIS TEXT CARD
 * =====================================================================
 */

@Composable
private fun ProAnalysisTextCard(
    title: String,
    text: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                        .copy(
                            alpha = 0.35f
                        )
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
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

            Text(
                text =
                    text,

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
 * =====================================================================
 * PRO ANALYSIS LIST CARD
 * =====================================================================
 */

@Composable
private fun ProAnalysisListCard(
    title: String,
    items: List<String>
) {

    ProAnalysisTextCard(

        title =
            title,

        text =
            items.joinToString(
                separator = "\n\n"
            ) {
                "• $it"
            }
    )
}
