package com.hiddenhistory.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hiddenhistory.database.AppSettings
import com.hiddenhistory.database.SettingsDao
import com.hiddenhistory.models.MotTest
import com.hiddenhistory.models.SymptomReport
import com.hiddenhistory.reports.VehicleReportPdfGenerator
import com.hiddenhistory.viewmodel.SavedVehicleRecord
import com.hiddenhistory.viewmodel.SavedVehicleReportsViewModel
import kotlinx.serialization.json.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedVehicleReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SavedVehicleReportsViewModel = viewModel()
) {
    val context = LocalContext.current
    val settingsDao = remember { SettingsDao(context) }

    val settingsState by settingsDao.settingsFlow.collectAsStateWithLifecycle(
        initialValue = AppSettings()
    )

    val savedReports by viewModel.savedReportsList.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var activeRecord by remember {
        mutableStateOf<SavedVehicleRecord?>(null)
    }

    var pendingDelete by remember {
        mutableStateOf<SavedVehicleRecord?>(null)
    }

    val jsonParser = remember {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            prettyPrint = true
        }
    }

    BackHandler(enabled = activeRecord != null) {
        activeRecord = null
    }

    pendingDelete?.let { record ->

        AlertDialog(
            onDismissRequest = {
                pendingDelete = null
            },
            title = {
                Text(
                    text = "Delete saved report?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "This will permanently delete the saved report for ${record.registration}."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val reportId = record.id

                        if (reportId != null) {
                            viewModel.deleteReport(
                                reportId = reportId,
                                onSuccess = {
                                    if (activeRecord?.id == reportId) {
                                        activeRecord = null
                                    }

                                    pendingDelete = null
                                }
                            )
                        } else {
                            pendingDelete = null
                        }
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (activeRecord == null) {
                            "Saved Vehicle Reports"
                        } else {
                            "Vehicle Report"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (activeRecord != null) {
                                activeRecord = null
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    activeRecord?.let { record ->

                        IconButton(
                            onClick = {
                                savePdfToDownloads(
                                    context = context,
                                    record = record
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Save PDF"
                            )
                        }

                        IconButton(
                            onClick = {
                                shareSavedReport(
                                    context = context,
                                    record = record
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Report"
                            )
                        }

                        if (record.id != null) {
                            IconButton(
                                onClick = {
                                    pendingDelete = record
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Report"
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            if (isLoading && savedReports.isEmpty()) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

            } else if (activeRecord == null) {

                SavedReportList(
                    reports = savedReports,
                    jsonParser = jsonParser,
                    onOpen = { record ->
                        activeRecord = record
                        viewModel.loadSavedReportDetails(record)
                    },
                    onDelete = { record ->
                        pendingDelete = record
                    }
                )

            } else {

                SavedReportDetail(
                    record = activeRecord!!,
                    jsonParser = jsonParser,
                    showWarnings = settingsState.analysisWarningsEnabled
                )
            }
        }
    }
}

@Composable
private fun SavedReportList(
    reports: List<SavedVehicleRecord>,
    jsonParser: Json,
    onOpen: (SavedVehicleRecord) -> Unit,
    onDelete: (SavedVehicleRecord) -> Unit
) {
    if (reports.isEmpty()) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(52.dp)
                )

                Text(
                    text = "No saved vehicle reports",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Vehicle reports you save will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        items(
            items = reports,
            key = { record ->
                record.id ?: "${record.registration}_${record.created_at}"
            }
        ) { record ->

            val vehicle = parseObject(
                record.vehicle_json,
                jsonParser
            )

            val make = vehicle?.string("make").orEmpty()
            val model = vehicle?.string("model").orEmpty()

            val year = vehicle?.string("yearOfManufacture")
                ?: vehicle?.string("year")
                ?: ""

            SavedReportListCard(
                record = record,
                make = make,
                model = model,
                year = year,
                onOpen = {
                    onOpen(record)
                },
                onDelete = {
                    onDelete(record)
                }
            )
        }
    }
}

@Composable
private fun SavedReportListCard(
    record: SavedVehicleRecord,
    make: String,
    model: String,
    year: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                Text(
                    text = record.registration
                        .uppercase()
                        .replace(" ", ""),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = listOf(
                        make,
                        model,
                        year
                    )
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifEmpty { "Vehicle Report" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                record.created_at
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        Text(
                            text = "Saved ${formatSavedDate(it)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (record.id != null) {
                    IconButton(
                        onClick = onDelete
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Report",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Report",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun SavedReportDetail(
    record: SavedVehicleRecord,
    jsonParser: Json,
    showWarnings: Boolean
) {
    val context = LocalContext.current

    val vehicle = remember(record.vehicle_json) {
        parseObject(
            record.vehicle_json,
            jsonParser
        )
    }

    val advert = remember(record.advert_json) {
        record.advert_json?.let {
            parseObject(it, jsonParser)
        }
    }

    val crossCheck = remember(record.cross_check_json) {
        record.cross_check_json?.let {
            parseObject(it, jsonParser)
        }
    }

    if (vehicle == null) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Error parsing report data.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        return
    }

    val motTests = remember(vehicle) {
        decodeMotTests(
            vehicle = vehicle,
            jsonParser = jsonParser
        )
    }

    val symptoms = remember(vehicle) {
        decodeSymptoms(
            vehicle = vehicle,
            jsonParser = jsonParser
        )
    }

    val report = remember(
        vehicle,
        advert,
        crossCheck,
        motTests,
        symptoms,
        record
    ) {
        SavedReportData(
            vehicle = vehicle,
            advert = buildAdvertUiData(advert),
            crossCheck = buildCrossCheckUiData(crossCheck),
            motTests = motTests,
            symptoms = symptoms,
            createdAt = record.created_at,
            reportSummary = record.report_summary
        )
    }

    val verificationItems = remember(
        report.advert?.verify,
        report.crossCheck?.verificationItems
    ) {
        (
            report.advert?.verify.orEmpty() +
                report.crossCheck?.verificationItems.orEmpty()
            )
            .filter { it.isNotBlank() }
            .distinct()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {

            ReportActionBar(
                onSavePdf = {
                    savePdfToDownloads(
                        context = context,
                        record = record
                    )
                },
                onSharePdf = {
                    shareSavedReport(
                        context = context,
                        record = record
                    )
                }
            )
        }

        item {
            VehicleHeroCard(report)
        }

        if (showWarnings) {
            item {
                ReportAttentionCard(report)
            }
        }

        item {
            ReportOverviewCard(report)
        }

        report.advert?.let {
            item {
                AdvertAnalysisCard(report)
            }
        }

        if (showWarnings) {
            report.crossCheck?.let {
                item {
                    CrossCheckCard(report)
                }
            }
        }

        if (report.motTests.isNotEmpty()) {

            item {
                MileageHistoryCard(report)
            }

            item {
                MotTimelineCard(report)
            }
        }

        report.advert?.let { advertData ->

            if (advertData.claims.isNotEmpty()) {
                item {
                    SellerClaimsCard(
                        claims = advertData.claims
                    )
                }
            }

            if (advertData.questions.isNotEmpty()) {
                item {
                    SellerQuestionsCard(
                        questions = advertData.questions
                    )
                }
            }

            if (advertData.missing.isNotEmpty()) {
                item {
                    MissingInformationCard(
                        items = advertData.missing
                    )
                }
            }
        }

        if (verificationItems.isNotEmpty()) {
            item {
                VerifyCard(
                    items = verificationItems
                )
            }
        }

        if (report.symptoms.isNotEmpty()) {
            item {
                SavedSymptomsSection(
                    symptoms = report.symptoms
                )
            }
        }

        item {
            ReportFooter()
        }
    }
}

@Composable
private fun ReportActionBar(
    onSavePdf: () -> Unit,
    onSharePdf: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Button(
            onClick = onSavePdf,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )

            Spacer(
                modifier = Modifier.width(6.dp)
            )

            Text("Save PDF")
        }

        OutlinedButton(
            onClick = onSharePdf,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )

            Spacer(
                modifier = Modifier.width(6.dp)
            )

            Text("Share PDF")
        }
    }
}

@Composable
private fun VehicleHeroCard(
    report: SavedReportData
) {
    val vehicle = report.vehicle

    val make = vehicle.string("make")
        .orEmpty()
        .uppercase()

    val model = vehicle.string("model")
        .orEmpty()

    val year = vehicle.string("yearOfManufacture")
        ?: vehicle.string("year")

    val engine = vehicle.string("engineSize")
    val fuel = vehicle.string("fuelType")
    val transmission = report.advert?.transmission

    val registration = vehicle.string("registrationNumber")
        ?: vehicle.string("registration")
        ?: "Unknown registration"

    val colour = vehicle.string("primaryColour")
        ?: vehicle.string("colour")

    val motStatus = vehicle.string("motStatus")
    val motExpiry = vehicle.string("motExpiryDate")

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = listOf(
                    make,
                    model
                )
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifEmpty { "Vehicle" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = listOfNotNull(
                    year,
                    engine,
                    fuel,
                    transmission
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = registration
                        .uppercase()
                        .replace(" ", ""),
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 9.dp
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                StatusPill(
                    text = "MOT ${motStatus?.uppercase() ?: "UNKNOWN"}",
                    positive = motStatus.equals(
                        "Valid",
                        ignoreCase = true
                    )
                )

                motExpiry
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        StatusPill(
                            text = "Until ${formatDate(it)}",
                            positive = true
                        )
                    }
            }

            colour
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    Text(
                        text = "Colour: ${it.uppercase()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            report.createdAt
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    HorizontalDivider()

                    Text(
                        text = "Report saved ${formatSavedDate(it)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
        }
    }
}

@Composable
private fun ReportAttentionCard(
    report: SavedReportData
) {
    val warnings = report.crossCheck?.warnings.orEmpty()

    if (warnings.isEmpty()) {
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {

            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {

                Text(
                    text = "ATTENTION REQUIRED",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Text(
                    text = "${warnings.size} finding${if (warnings.size == 1) "" else "s"} require attention",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Text(
                    text = "Review the cross-check findings below before making a buying decision.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Text(
                    text = "Seller claims remain unverified unless supported by independent evidence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun ReportOverviewCard(
    report: SavedReportData
) {
    val vehicle = report.vehicle

    SectionCard(
        title = "REPORT OVERVIEW"
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            OverviewMetric(
                label = "MOT",
                value = vehicle.string("motStatus")
                    ?: "Unknown",
                positive = vehicle.string("motStatus")
                    .equals("Valid", true)
            )

            OverviewMetric(
                label = "Tax",
                value = vehicle.string("taxStatus")
                    ?: "Unknown",
                positive = vehicle.string("taxStatus")
                    .equals("Taxed", true)
            )
        }

        InfoRow(
            label = "MOT expiry",
            value = vehicle.string("motExpiryDate")
                ?.let(::formatDate)
                ?: "Not available"
        )

        InfoRow(
            label = "Tax due",
            value = vehicle.string("taxDueDate")
                ?.let(::formatDate)
                ?: "Not available"
        )

        InfoRow(
            label = "MOT tests",
            value = report.motTests.size.toString()
        )

        report.reportSummary
            ?.takeIf { it.isNotBlank() }
            ?.let {

                HorizontalDivider(
                    modifier = Modifier.padding(
                        vertical = 4.dp
                    )
                )

                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
    }
}

@Composable
private fun AdvertAnalysisCard(
    report: SavedReportData
) {
    val advert = report.advert
        ?: return

    var expandedClaims by remember {
        mutableStateOf(false)
    }

    var expandedInsights by remember {
        mutableStateOf(false)
    }

    SectionCard(
        title = "ADVERT ANALYSIS"
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            ScoreRing(
                score = advert.score ?: 0,
                modifier = Modifier.size(104.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {

                Text(
                    text = "Advert Information Index",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Analysis of the information and claims contained in the saved advert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${advert.claims.size} claims • ${advert.missing.size} missing",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            SmallStatCard(
                label = "Price",
                value = advert.price ?: "Not stated",
                modifier = Modifier.weight(1f)
            )

            SmallStatCard(
                label = "Mileage",
                value = advert.mileage ?: "Not stated",
                modifier = Modifier.weight(1f)
            )
        }

        if (advert.risks.isNotEmpty()) {

            AlertBlock(
                title = "Risk flags",
                items = advert.risks,
                error = true
            )
        }

        if (advert.inconsistencies.isNotEmpty()) {

            AlertBlock(
                title = "Advert inconsistencies",
                items = advert.inconsistencies,
                error = false
            )
        }

        if (advert.claims.isNotEmpty()) {

            ExpandableHeader(
                title = "Seller claims — not independently verified",
                count = advert.claims.size,
                expanded = expandedClaims,
                onClick = {
                    expandedClaims = !expandedClaims
                }
            )

            AnimatedVisibility(
                visible = expandedClaims,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    advert.claims.forEach { claim ->
                        BulletRow(claim)
                    }
                }
            }
        }

        if (advert.insights.isNotEmpty()) {

            ExpandableHeader(
                title = "Advert insights",
                count = advert.insights.size,
                expanded = expandedInsights,
                onClick = {
                    expandedInsights = !expandedInsights
                }
            )

            AnimatedVisibility(
                visible = expandedInsights,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    advert.insights.forEach { insight ->
                        BulletRow(insight)
                    }
                }
            }
        }

        advert.summary
            ?.takeIf { it.isNotBlank() }
            ?.let {

                HorizontalDivider()

                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
    }
}

@Composable
private fun CrossCheckCard(
    report: SavedReportData
) {
    val cross = report.crossCheck
        ?: return

    var expandedWarning by remember {
        mutableIntStateOf(-1)
    }

    SectionCard(
        title = "ADVERT ↔ OFFICIAL CROSS-CHECK"
    ) {

        Text(
            text = "This section compares information stored from the advert with official vehicle and MOT evidence.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (cross.warnings.isEmpty()) {

            StatusBanner(
                text = "No cross-check warnings were stored for this report.",
                positive = true
            )

        } else {

            Text(
                text = "${cross.warnings.size} finding${if (cross.warnings.size == 1) "" else "s"} require attention",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            cross.warnings.forEachIndexed { index, warning ->

                CrossCheckFinding(
                    warning = warning,
                    expanded = expandedWarning == index,
                    onClick = {
                        expandedWarning =
                            if (expandedWarning == index) {
                                -1
                            } else {
                                index
                            }
                    }
                )
            }
        }

        if (cross.confirmations.isNotEmpty()) {

            ExpandableList(
                title = "Confirmed report facts",
                items = cross.confirmations
            )
        }
    }
}

@Composable
private fun CrossCheckFinding(
    warning: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val title = warning
        .substringBefore(":")
        .trim()
        .ifBlank {
            "Finding"
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                alpha = 0.55f
            )
        )
    ) {

        Column {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onError
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    if (!expanded) {
                        Text(
                            text = warning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 2
                        )
                    }
                }

                Icon(
                    imageVector =
                        if (expanded) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                    contentDescription = if (expanded) {
                        "Collapse finding"
                    } else {
                        "Expand finding"
                    },
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {

                Column(
                    modifier = Modifier.padding(
                        start = 56.dp,
                        end = 14.dp,
                        bottom = 14.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun MileageHistoryCard(
    report: SavedReportData
) {
    val tests = report.motTests.filter {
        !it.odometerValue.isNullOrBlank()
    }

    if (tests.isEmpty()) {
        return
    }

    val numericValues = tests.mapNotNull {
        it.odometerValue
            ?.replace(",", "")
            ?.toDoubleOrNull()
    }

    val maximumValue = numericValues.maxOrNull()
        ?: 0.0

    SectionCard(
        title = "MILEAGE HISTORY"
    ) {

        Text(
            text = "Recorded mileage from available MOT tests.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        tests.forEach { test ->

            val rawValue = test.odometerValue
                ?: ""

            val numericValue = rawValue
                .replace(",", "")
                .toDoubleOrNull()

            val fraction =
                if (
                    numericValue != null &&
                    maximumValue > 0.0
                ) {
                    (
                        numericValue / maximumValue
                    )
                        .coerceIn(
                            0.05,
                            1.0
                        )
                        .toFloat()
                } else {
                    1.0f
                }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = test.completedDate
                        ?.take(4)
                        ?: "MOT",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.width(44.dp),
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(
                            RoundedCornerShape(20.dp)
                        )
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.primary
                            )
                    )
                }

                Text(
                    text = "$rawValue ${test.odometerUnit ?: ""}".trim(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.widthIn(
                        min = 76.dp
                    ),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MotTimelineCard(
    report: SavedReportData
) {
    SectionCard(
        title = "MOT HISTORY"
    ) {

        report.motTests.forEachIndexed { index, test ->

            MotTimelineItem(
                test = test,
                isLast = index == report.motTests.lastIndex
            )
        }
    }
}

@Composable
private fun MotTimelineItem(
    test: MotTest,
    isLast: Boolean
) {
    val passed = test.testResult.equals(
        "PASSED",
        ignoreCase = true
    )

    val resultColor =
        if (passed) {
            Color(0xFF1B8F4A)
        } else {
            MaterialTheme.colorScheme.error
        }

    val year = test.completedDate
        ?.take(4)
        ?: "MOT"

    val defects = test.defects.filter {
        !it.text.isNullOrBlank()
    }

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(resultColor)
            )

            if (!isLast) {

                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(68.dp)
                        .background(
                            MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    bottom = if (isLast) {
                        0.dp
                    } else {
                        12.dp
                    }
                ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = year,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = test.testResult?.uppercase()
                        ?: "UNKNOWN",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = resultColor
                )
            }

            Text(
                text = "${test.odometerValue ?: "N/A"} ${test.odometerUnit ?: "MI"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            defects
                .take(3)
                .forEach { defect ->

                    Text(
                        text = "• ${defect.text}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            if (defects.size > 3) {

                Text(
                    text = "+ ${defects.size - 3} more defect${if (defects.size - 3 == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SellerClaimsCard(
    claims: List<String>
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    SectionCard(
        title = "SELLER CLAIMS"
    ) {

        StatusBanner(
            text = "Seller claims — not independently verified",
            positive = false
        )

        ExpandableHeader(
            title = if (expanded) {
                "Hide claims"
            } else {
                "Show claims"
            },
            count = claims.size,
            expanded = expanded,
            onClick = {
                expanded = !expanded
            }
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {

            Column(
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                claims.forEach { claim ->
                    BulletRow(claim)
                }
            }
        }
    }
}

@Composable
private fun VerifyCard(
    items: List<String>
) {
    val checked = remember(items) {
        mutableStateListOf<Boolean>().apply {
            repeat(items.size) {
                add(false)
            }
        }
    }

    val completedCount = checked.count { it }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                alpha = 0.65f
            )
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "WHAT YOU SHOULD VERIFY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "Practical checks to complete before buying.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (completedCount > 0) {

                Text(
                    text = "$completedCount of ${items.size} checks completed",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider()

            items.forEachIndexed { index, item ->

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            checked[index] = !checked[index]
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (checked[index]) {
                        MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.55f
                        )
                    } else {
                        MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.85f
                        )
                    }
                ) {

                    Row(
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Checkbox(
                            checked = checked[index],
                            onCheckedChange = {
                                checked[index] = it
                            }
                        )

                        Text(
                            text = item,
                            modifier = Modifier
                                .weight(1f)
                                .padding(
                                    end = 8.dp
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (checked[index]) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerQuestionsCard(
    questions: List<String>
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    SectionCard(
        title = "QUESTIONS FOR THE SELLER"
    ) {

        ExpandableHeader(
            title = if (expanded) {
                "Hide questions"
            } else {
                "Show questions"
            },
            count = questions.size,
            expanded = expanded,
            onClick = {
                expanded = !expanded
            }
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                questions.forEachIndexed { index, question ->

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {

                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "%02d".format(index + 1),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = question,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MissingInformationCard(
    items: List<String>
) {
    SectionCard(
        title = "MISSING FROM ADVERT"
    ) {

        Text(
            text = "${items.size} item${if (items.size == 1) "" else "s"} were not supplied by the advert.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        items.forEach { item ->
            BulletRow(item)
        }
    }
}

@Composable
private fun SavedSymptomsSection(
    symptoms: List<SymptomReport>
) {
    SectionCard(
        title = "ACTIVE SYMPTOMS"
    ) {

        symptoms.forEach { symptom ->

            BulletRow(
                symptom.userDescription
                    ?: "Unspecified symptom"
            )
        }
    }
}

@Composable
private fun ReportFooter() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.35f
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {

            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "This report contains information and evidence stored when the vehicle was analysed. Always independently verify important details before purchasing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.35f
            )
        )
    ) {

        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            content()
        }
    }
}

@Composable
private fun ScoreRing(
    score: Int,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        score >= 70 ->
            Color(0xFF1B8F4A)

        score >= 50 ->
            MaterialTheme.colorScheme.tertiary

        else ->
            MaterialTheme.colorScheme.error
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                scoreColor.copy(alpha = 0.12f)
            ),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = score.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = scoreColor
            )

            Text(
                text = "/ 100",
                style = MaterialTheme.typography.labelSmall,
                color = scoreColor
            )
        }
    }
}

@Composable
private fun RowScope.OverviewMetric(
    label: String,
    value: String,
    positive: Boolean
) {
    Surface(
        modifier = Modifier.weight(1f),
        color = if (positive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(14.dp)
    ) {

        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SmallStatCard(
    label: String,
    value: String,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.55f
        ),
        shape = RoundedCornerShape(14.dp)
    ) {

        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    positive: Boolean
) {
    val background =
        if (positive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        }

    val foreground =
        if (positive) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        }

    Surface(
        color = background,
        shape = RoundedCornerShape(50)
    ) {

        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 11.dp,
                vertical = 6.dp
            ),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = foreground
        )
    }
}

@Composable
private fun StatusBanner(
    text: String,
    positive: Boolean
) {
    val background =
        if (positive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        }

    val foreground =
        if (positive) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onTertiaryContainer
        }

    Surface(
        color = background,
        shape = RoundedCornerShape(13.dp)
    ) {

        Text(
            text = text,
            modifier = Modifier.padding(11.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = foreground
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BulletRow(
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {

        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ExpandableHeader(
    title: String,
    count: Int?,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = if (count != null) {
                "$title ($count)"
            } else {
                title
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector =
                if (expanded) {
                    Icons.Default.ExpandLess
                } else {
                    Icons.Default.ExpandMore
                },
            contentDescription =
                if (expanded) {
                    "Collapse"
                } else {
                    "Expand"
                }
        )
    }
}

@Composable
private fun AlertBlock(
    title: String,
    items: List<String>,
    error: Boolean
) {
    val background =
        if (error) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        }

    val foreground =
        if (error) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onTertiaryContainer
        }

    Surface(
        color = background,
        shape = RoundedCornerShape(14.dp)
    ) {

        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = foreground
            )

            items.forEach { item ->

                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = foreground
                )
            }
        }
    }
}

@Composable
private fun ExpandableList(
    title: String,
    items: List<String>
) {
    var expanded by remember(title) {
        mutableStateOf(false)
    }

    ExpandableHeader(
        title = title,
        count = items.size,
        expanded = expanded,
        onClick = {
            expanded = !expanded
        }
    )

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {

            items.forEach { item ->
                BulletRow(item)
            }
        }
    }
}

private data class SavedReportData(
    val vehicle: JsonObject,
    val advert: AdvertUiData?,
    val crossCheck: CrossCheckUiData?,
    val motTests: List<MotTest>,
    val symptoms: List<SymptomReport>,
    val createdAt: String?,
    val reportSummary: String?
)

private data class AdvertUiData(
    val make: String?,
    val model: String?,
    val year: String?,
    val price: String?,
    val mileage: String?,
    val engineSize: String?,
    val fuelType: String?,
    val transmission: String?,
    val score: Int?,
    val summary: String?,
    val claims: List<String>,
    val missing: List<String>,
    val verify: List<String>,
    val questions: List<String>,
    val risks: List<String>,
    val insights: List<String>,
    val inconsistencies: List<String>
)

private data class CrossCheckUiData(
    val warnings: List<String>,
    val confirmations: List<String>,
    val verificationItems: List<String>
)

private fun buildAdvertUiData(
    json: JsonObject?
): AdvertUiData? {
    if (json == null) {
        return null
    }

    return AdvertUiData(
        make = json.string("make"),
        model = json.string("model"),
        year = json.string("year"),
        price = json.string("price"),
        mileage = json.string("mileage"),
        engineSize = json.string("engineSize"),
        fuelType = json.string("fuelType"),
        transmission = json.string("transmission"),
        score = json.int("conditionScore"),
        summary = json.string("professionalSummary")
            ?: json.string("overallSummary"),
        claims = json.stringList("claimsMadeBySeller"),
        missing = json.stringList("missingInformation"),
        verify = json.stringList("thingsWorthVerifying"),
        questions = json.stringList("questionsTheBuyerShouldAsk"),
        risks = json.stringList("riskFlags"),
        insights = json.stringList("keyInsights"),
        inconsistencies = json.stringList("inconsistencies")
    )
}

private fun buildCrossCheckUiData(
    json: JsonObject?
): CrossCheckUiData? {
    if (json == null) {
        return null
    }

    return CrossCheckUiData(
        warnings = json.stringList("warnings"),
        confirmations = json.stringList("confirmations"),
        verificationItems = json.stringList("verificationItems")
    )
}

private fun parseIsoDate(
    value: String
): java.util.Date? {

    val candidates = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd"
    )

    for (pattern in candidates) {

        runCatching {

            return SimpleDateFormat(
                pattern,
                Locale.UK
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(value)
        }
    }

    return null
}

private fun formatSavedDate(
    value: String
): String {
    return parseIsoDate(value)?.let {

        SimpleDateFormat(
            "dd MMM yyyy • HH:mm",
            Locale.UK
        ).apply {
            timeZone = TimeZone.getDefault()
        }.format(it)

    } ?: value
}

private fun formatDate(
    value: String
): String {

    val date = parseIsoDate(value)
        ?: return value

    return SimpleDateFormat(
        "dd MMM yyyy",
        Locale.UK
    ).apply {
        timeZone = TimeZone.getDefault()
    }.format(date)
}

private fun parseObject(
    value: String,
    jsonParser: Json
): JsonObject? =
    runCatching {
        jsonParser
            .parseToJsonElement(value)
            .jsonObject
    }.getOrNull()

private fun JsonObject.string(
    key: String
): String? =
    this[key]
        ?.jsonPrimitive
        ?.contentOrNull

private fun JsonObject.int(
    key: String
): Int? =
    this[key]
        ?.jsonPrimitive
        ?.intOrNull

private fun JsonObject.stringList(
    key: String
): List<String> =
    this[key]
        ?.jsonArray
        ?.mapNotNull {
            it.jsonPrimitive
                .contentOrNull
                ?.takeIf(String::isNotBlank)
        }
        ?: emptyList()

private fun decodeMotTests(
    vehicle: JsonObject,
    jsonParser: Json
): List<MotTest> =
    runCatching {

        vehicle["motTests"]
            ?.jsonArray
            ?.mapNotNull { element ->

                runCatching {
                    jsonParser.decodeFromJsonElement<MotTest>(
                        element
                    )
                }.getOrNull()
            }

    }.getOrNull()
        ?: emptyList()

private fun decodeSymptoms(
    vehicle: JsonObject,
    jsonParser: Json
): List<SymptomReport> {

    val element =
        vehicle["symptoms"]
            ?: vehicle["activeSymptoms"]
            ?: return emptyList()

    return runCatching {

        element.jsonArray.mapNotNull { item ->

            runCatching {
                jsonParser.decodeFromJsonElement<SymptomReport>(
                    item
                )
            }.getOrNull()
        }

    }.getOrNull()
        ?: emptyList()
}

private fun savePdfToDownloads(
    context: Context,
    record: SavedVehicleRecord
) {
    runCatching {

        val tempFile =
            VehicleReportPdfGenerator.generate(
                context,
                record
            )

        val cleanReg =
            record.registration
                .uppercase()
                .replace(" ", "")

        val fileName =
            "HiddenHistory_$cleanReg.pdf"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val contentValues =
                ContentValues().apply {

                    put(
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        fileName
                    )

                    put(
                        MediaStore.MediaColumns.MIME_TYPE,
                        "application/pdf"
                    )

                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS
                    )
                }

            val resolver =
                context.contentResolver

            val uri =
                resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )

            if (uri != null) {

                resolver
                    .openOutputStream(uri)
                    ?.use { outputStream ->

                        tempFile
                            .inputStream()
                            .use { inputStream ->

                                inputStream.copyTo(
                                    outputStream
                                )
                            }
                    }

                Toast.makeText(
                    context,
                    "Saved to Downloads: $fileName",
                    Toast.LENGTH_LONG
                ).show()

            } else {

                Toast.makeText(
                    context,
                    "Failed to create file in Downloads",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } else {

            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )

            val destFile =
                File(
                    downloadsDir,
                    fileName
                )

            tempFile.copyTo(
                destFile,
                overwrite = true
            )

            Toast.makeText(
                context,
                "Saved to Downloads: $fileName",
                Toast.LENGTH_LONG
            ).show()
        }

    }.onFailure { error ->

        Toast.makeText(
            context,
            "Failed to save PDF: ${error.localizedMessage}",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun shareSavedReport(
    context: Context,
    record: SavedVehicleRecord
) {
    val pdfFile =
        VehicleReportPdfGenerator.generate(
            context,
            record
        )

    val uri =
        VehicleReportPdfGenerator.getShareUri(
            context,
            pdfFile
        )

    val shareIntent =
        Intent(Intent.ACTION_SEND).apply {

            type = "application/pdf"

            putExtra(
                Intent.EXTRA_STREAM,
                uri
            )

            putExtra(
                Intent.EXTRA_TITLE,
                "Hidden History Vehicle Report - ${record.registration}"
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            "Share Vehicle Report PDF"
        )
    )
}