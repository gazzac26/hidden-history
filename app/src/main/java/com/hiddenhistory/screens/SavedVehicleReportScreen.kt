package com.hiddenhistory.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hiddenhistory.database.AppSettings
import com.hiddenhistory.database.SettingsDao
import com.hiddenhistory.models.MotTest
import com.hiddenhistory.models.SymptomReport
import com.hiddenhistory.viewmodel.SavedVehicleRecord
import com.hiddenhistory.viewmodel.SavedVehicleReportsViewModel
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedVehicleReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SavedVehicleReportsViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsDao = remember { SettingsDao(context) }
    val settingsState by settingsDao.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())

    val savedReports by viewModel.savedReportsList.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var activeRecord by remember { mutableStateOf<SavedVehicleRecord?>(null) }
    var pendingDelete by remember { mutableStateOf<SavedVehicleRecord?>(null) }

    val jsonParser = remember {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            prettyPrint = true
        }
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = {
                pendingDelete = null
            },
            title = {
                Text("Delete saved report?")
            },
            text = {
                Text(
                    "This will permanently delete the saved report for " +
                        "${record.registration}."
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
                    Text("Delete")
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

                        val actionContext =
                            androidx.compose.ui.platform.LocalContext.current

                        IconButton(
                            onClick = {
                                shareSavedReport(
                                    context = actionContext,
                                    record = record,
                                    jsonParser = jsonParser
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "No saved vehicle reports found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        items(reports) { record ->

            val vehicle =
                parseObject(
                    record.vehicle_json,
                    jsonParser
                )

            val make =
                vehicle?.string("make") ?: ""

            val model =
                vehicle?.string("model") ?: ""

            val year =
                vehicle?.string("yearOfManufacture")
                    ?: vehicle?.string("year")
                    ?: ""

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpen(record)
                    },
                shape = RoundedCornerShape(18.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(3.dp)
                    ) {

                        Text(
                            text = record.registration,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = listOf(
                                make,
                                model,
                                year
                            )
                                .filter {
                                    it.isNotBlank()
                                }
                                .joinToString(" ")
                                .ifEmpty {
                                    "Vehicle Report"
                                },
                            style =
                                MaterialTheme.typography.bodyMedium,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        record.created_at
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?.let {
                                Text(
                                    text =
                                        "Saved ${formatSavedDate(it)}",
                                    style =
                                        MaterialTheme.typography.labelMedium,
                                    color =
                                        MaterialTheme.colorScheme.outline
                                )
                            }
                    }

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        if (record.id != null) {

                            IconButton(
                                onClick = {
                                    onDelete(record)
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        Icons.Default.Delete,
                                    contentDescription =
                                        "Delete Report",
                                    tint =
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Icon(
                            imageVector =
                                Icons.Default.ChevronRight,
                            contentDescription =
                                "View Details",
                            tint =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier.size(22.dp)
                        )
                    }
                }
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
    val context = androidx.compose.ui.platform.LocalContext.current

    val vehicle =
        remember(record.vehicle_json) {
            parseObject(
                record.vehicle_json,
                jsonParser
            )
        }

    val advert =
        remember(record.advert_json) {
            record.advert_json?.let {
                parseObject(
                    it,
                    jsonParser
                )
            }
        }

    val crossCheck =
        remember(record.cross_check_json) {
            record.cross_check_json?.let {
                parseObject(
                    it,
                    jsonParser
                )
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

    val motTests =
        remember(vehicle) {
            decodeMotTests(
                vehicle,
                jsonParser
            )
        }

    val symptoms =
        remember(vehicle) {
            decodeSymptoms(
                vehicle,
                jsonParser
            )
        }

    val report =
        remember(
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        item {
            Button(
                onClick = {
                    shareSavedReport(context, record, jsonParser)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share / Export PDF Report")
            }
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
                        advertData.claims
                    )
                }
            }

            if (advertData.verify.isNotEmpty()) {
                item {
                    VerifyCard(
                        advertData.verify
                    )
                }
            }

            if (advertData.questions.isNotEmpty()) {
                item {
                    SellerQuestionsCard(
                        advertData.questions
                    )
                }
            }

            if (advertData.missing.isNotEmpty()) {
                item {
                    MissingInformationCard(
                        advertData.missing
                    )
                }
            }
        }

        if (report.symptoms.isNotEmpty()) {

            item {
                SavedSymptomsSection(
                    report.symptoms
                )
            }
        }

        item {
            AdvancedReportDataCard(
                report,
                jsonParser
            )
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
        summary =
            json.string("professionalSummary")
                ?: json.string("overallSummary"),
        claims =
            json.stringList("claimsMadeBySeller"),
        missing =
            json.stringList("missingInformation"),
        verify =
            json.stringList("thingsWorthVerifying"),
        questions =
            json.stringList("questionsTheBuyerShouldAsk"),
        risks =
            json.stringList("riskFlags"),
        insights =
            json.stringList("keyInsights"),
        inconsistencies =
            json.stringList("inconsistencies")
    )
}

private fun buildCrossCheckUiData(
    json: JsonObject?
): CrossCheckUiData? {

    if (json == null) {
        return null
    }

    return CrossCheckUiData(
        warnings =
            json.stringList("warnings"),
        confirmations =
            json.stringList("confirmations"),
        verificationItems =
            json.stringList("verificationItems")
    )
}

@Composable
private fun VehicleHeroCard(
    report: SavedReportData
) {
    val v = report.vehicle

    val make =
        v.string("make")
            .orEmpty()
            .uppercase()

    val model =
        v.string("model")
            .orEmpty()

    val year =
        v.string("yearOfManufacture")
            ?: v.string("year")

    /*
     * Display the saved engineSize exactly as stored.
     *
     * No fallback to engineCapacity is performed here.
     */
    val engine =
        v.string("engineSize")

    val fuel =
        v.string("fuelType")

    val transmission =
        report.advert?.transmission

    val registration =
        report.vehicle.string("registrationNumber")
            ?: report.vehicle.string("registration")
            ?: "Unknown registration"

    val colour =
        v.string("primaryColour")
            ?: v.string("colour")

    val motStatus =
        v.string("motStatus")

    val motExpiry =
        v.string("motExpiryDate")

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.35f)
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text =
                    listOf(
                        make,
                        model
                    )
                        .filter {
                            it.isNotBlank()
                        }
                        .joinToString(" ")
                        .ifEmpty {
                            "Vehicle"
                        },
                style =
                    MaterialTheme.typography.headlineSmall,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    listOfNotNull(
                        year,
                        engine,
                        fuel,
                        transmission
                    ).joinToString(" • "),
                style =
                    MaterialTheme.typography.bodyLarge,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text =
                    registration
                        .uppercase()
                        .replace(" ", ""),
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight =
                    FontWeight.ExtraBold,
                color =
                    MaterialTheme.colorScheme.primary
            )

            colour
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    Text(
                        text = it.uppercase(),
                        style =
                            MaterialTheme.typography.labelLarge,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                StatusPill(
                    text =
                        "MOT ${motStatus?.uppercase() ?: "UNKNOWN"}",
                    positive =
                        motStatus.equals(
                            "Valid",
                            ignoreCase = true
                        )
                )

                motExpiry?.let {
                    StatusPill(
                        text =
                            "Until ${formatDate(it)}",
                        positive = true
                    )
                }
            }

            report.createdAt
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    Text(
                        text =
                            "Saved ${formatSavedDate(it)}",
                        style =
                            MaterialTheme.typography.labelMedium,
                        color =
                            MaterialTheme.colorScheme.outline
                    )
                }
        }
    }
}

@Composable
private fun ReportAttentionCard(
    report: SavedReportData
) {
    /*
     * IMPORTANT:
     *
     * This screen does not calculate attention conditions.
     *
     * The only findings displayed here are warnings that were
     * actually persisted in cross_check_json.
     */
    val warnings =
        report.crossCheck?.warnings.orEmpty()

    if (warnings.isEmpty()) {
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.errorContainer
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = "ATTENTION REQUIRED",
                style =
                    MaterialTheme.typography.labelLarge,
                fontWeight =
                    FontWeight.ExtraBold,
                color =
                    MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text =
                    "${warnings.size} stored finding${if (warnings.size == 1) "" else "s"} require attention",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text =
                    "The saved cross-check contains the original findings stored with this report.",
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text =
                    "The report presents evidence from the saved advert and official vehicle/MOT data. Claims remain seller statements until independently verified.",
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun ReportOverviewCard(
    report: SavedReportData
) {
    val v = report.vehicle

    SectionCard(
        title = "REPORT OVERVIEW"
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            OverviewMetric(
                label = "MOT",
                value =
                    v.string("motStatus")
                        ?: "Unknown",
                positive =
                    v.string("motStatus")
                        .equals(
                            "Valid",
                            true
                        )
            )

            OverviewMetric(
                label = "Tax",
                value =
                    v.string("taxStatus")
                        ?: "Unknown",
                positive =
                    v.string("taxStatus")
                        .equals(
                            "Taxed",
                            true
                        )
            )
        }

        InfoRow(
            "MOT expiry",
            v.string("motExpiryDate")
                ?.let(::formatDate)
                ?: "Not available"
        )

        InfoRow(
            "Tax due",
            v.string("taxDueDate")
                ?.let(::formatDate)
                ?: "Not available"
        )

        InfoRow(
            "MOT tests",
            report.motTests.size.toString()
        )

        report.reportSummary
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {

                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            vertical = 4.dp
                        )
                )

                Text(
                    text = it,
                    style =
                        MaterialTheme.typography.bodyMedium,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
    }
}

@Composable
private fun AdvertAnalysisCard(
    report: SavedReportData
) {
    val advert =
        report.advert
            ?: return

    var expandedClaims by
        remember {
            mutableStateOf(false)
        }

    var expandedInsights by
        remember {
            mutableStateOf(false)
        }

    SectionCard(
        title = "ADVERT ANALYSIS"
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            ScoreRing(
                score =
                    advert.score ?: 0,
                modifier =
                    Modifier.size(104.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(4.dp)
            ) {

                Text(
                    text =
                        "Advert Information Index",
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Saved advert analysis score",
                    style =
                        MaterialTheme.typography.bodyMedium,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text =
                        "${advert.claims.size} seller claims • ${advert.missing.size} missing information items",
                    style =
                        MaterialTheme.typography.labelLarge,
                    color =
                        MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            SmallStatCard(
                "Price",
                advert.price ?: "Not stated",
                Modifier.weight(1f)
            )

            SmallStatCard(
                "Mileage",
                advert.mileage ?: "Not stated",
                Modifier.weight(1f)
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
                title =
                    "Seller claims — not independently verified",
                count =
                    advert.claims.size,
                expanded =
                    expandedClaims,
                onClick = {
                    expandedClaims =
                        !expandedClaims
                }
            )

            AnimatedVisibility(
                visible = expandedClaims,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(6.dp)
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
                count =
                    advert.insights.size,
                expanded =
                    expandedInsights,
                onClick = {
                    expandedInsights =
                        !expandedInsights
                }
            )

            AnimatedVisibility(
                visible = expandedInsights,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    advert.insights.forEach { insight ->
                        BulletRow(insight)
                    }
                }
            }
        }

        advert.summary
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {

                Text(
                    text = it,
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
    }
}

@Composable
private fun CrossCheckCard(
    report: SavedReportData
) {
    val cross =
        report.crossCheck
            ?: return

    var expandedWarning by
        remember {
            mutableIntStateOf(-1)
        }

    SectionCard(
        title = "ADVERT ↔ OFFICIAL CROSS-CHECK"
    ) {

        if (cross.warnings.isEmpty()) {

            StatusBanner(
                text =
                    "No cross-check warnings were stored for this report.",
                positive = true
            )

        } else {

            Text(
                text =
                    "${cross.warnings.size} finding${if (cross.warnings.size == 1) "" else "s"} require attention",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight =
                    FontWeight.Bold
            )

            cross.warnings.forEachIndexed { index, warning ->

                CrossCheckFinding(
                    warning = warning,
                    action = null,
                    expanded =
                        expandedWarning == index,
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

            if (cross.verificationItems.isNotEmpty()) {

                ExpandableList(
                    title = "Verification actions",
                    items =
                        cross.verificationItems
                )
            }
        }

        if (cross.confirmations.isNotEmpty()) {

            ExpandableList(
                title = "Confirmed report facts",
                items =
                    cross.confirmations
            )
        }
    }
}

@Composable
private fun CrossCheckFinding(
    warning: String,
    action: String?,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val title =
        warning
            .substringBefore(":")
            .trim()
            .ifBlank {
                "Finding"
            }

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.errorContainer
                        .copy(alpha = 0.55f)
            )
    ) {

        Column {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = onClick
                        )
                        .padding(13.dp),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = title,
                        style =
                            MaterialTheme.typography.titleSmall,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme.onErrorContainer
                    )

                    if (!expanded) {

                        Text(
                            text = warning,
                            style =
                                MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onErrorContainer,
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
                    contentDescription = null,
                    tint =
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            start = 13.dp,
                            end = 13.dp,
                            bottom = 13.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = warning,
                        style =
                            MaterialTheme.typography.bodyMedium,
                        color =
                            MaterialTheme.colorScheme.onErrorContainer
                    )

                    action?.let {

                        Surface(
                            color =
                                MaterialTheme.colorScheme.surface
                                    .copy(alpha = 0.65f),
                            shape =
                                RoundedCornerShape(10.dp)
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(10.dp)
                            ) {

                                Text(
                                    text = "WHAT TO DO",
                                    style =
                                        MaterialTheme.typography.labelMedium,
                                    fontWeight =
                                        FontWeight.Bold,
                                    color =
                                        MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = it,
                                    style =
                                        MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MileageHistoryCard(
    report: SavedReportData
) {
    /*
     * IMPORTANT:
     *
     * Every saved MOT test is displayed.
     *
     * The screen no longer:
     * - converts KM to miles
     * - groups tests by year
     * - chooses the highest mileage for a year
     * - calculates a mileage discrepancy
     *
     * The mileage text shown is the actual saved odometer value
     * and the actual saved odometer unit.
     *
     * The bar width is only visual presentation and does not alter
     * or generate any report data.
     */

    val tests =
        report.motTests.filter {
            !it.odometerValue.isNullOrBlank()
        }

    if (tests.isEmpty()) {
        return
    }

    val numericValues =
        tests.mapNotNull {
            it.odometerValue
                ?.replace(",", "")
                ?.toDoubleOrNull()
        }

    val maximumValue =
        numericValues.maxOrNull()
            ?: 0.0

    SectionCard(
        title = "MILEAGE HISTORY"
    ) {

        tests.forEach { test ->

            val rawValue =
                test.odometerValue
                    ?: ""

            val numericValue =
                rawValue
                    .replace(",", "")
                    .toDoubleOrNull()

            val fraction =
                if (
                    numericValue != null &&
                    maximumValue > 0.0
                ) {
                    (
                        numericValue /
                            maximumValue
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
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        test.completedDate
                            ?.take(4)
                            ?: "MOT",
                    style =
                        MaterialTheme.typography.labelLarge,
                    modifier =
                        Modifier.width(44.dp),
                    fontWeight =
                        FontWeight.Bold
                )

                Box(
                    modifier =
                        Modifier
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
                        modifier =
                            Modifier
                                .fillMaxWidth(
                                    fraction
                                )
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme.primary
                                )
                    )
                }

                Text(
                    text =
                        "$rawValue ${test.odometerUnit ?: ""}"
                            .trim(),
                    style =
                        MaterialTheme.typography.labelMedium,
                    modifier =
                        Modifier.widthIn(
                            min = 76.dp
                        ),
                    fontWeight =
                        FontWeight.SemiBold
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
                isLast =
                    index == report.motTests.lastIndex
            )
        }
    }
}

@Composable
private fun MotTimelineItem(
    test: MotTest,
    isLast: Boolean
) {
    val passed =
        test.testResult.equals(
            "PASSED",
            ignoreCase = true
        )

    val resultColor =
        if (passed) {
            Color(0xFF1B8F4A)
        } else {
            MaterialTheme.colorScheme.error
        }

    val year =
        test.completedDate
            ?.take(4)
            ?: "MOT"

    val defects =
        test.defects.filter {
            !it.text.isNullOrBlank()
        }

    Row(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier =
                    Modifier
                        .size(14.dp)
                        .clip(
                            RoundedCornerShape(50)
                        )
                        .background(
                            resultColor
                        )
            )

            if (!isLast) {

                Box(
                    modifier =
                        Modifier
                            .width(2.dp)
                            .height(68.dp)
                            .background(
                                MaterialTheme.colorScheme.outlineVariant
                            )
                )
            }
        }

        Spacer(
            modifier =
                Modifier.width(12.dp)
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom =
                            if (isLast) {
                                0.dp
                            } else {
                                10.dp
                            }
                    ),
            verticalArrangement =
                Arrangement.spacedBy(3.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    text = year,
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        test.testResult
                            ?.uppercase()
                            ?: "UNKNOWN",
                    style =
                        MaterialTheme.typography.labelLarge,
                    fontWeight =
                        FontWeight.ExtraBold,
                    color =
                        resultColor
                )
            }

            Text(
                text =
                    "${test.odometerValue ?: "N/A"} ${test.odometerUnit ?: "MI"}",
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            defects.take(3).forEach { defect ->

                Text(
                    text =
                        "• ${defect.text}",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (defects.size > 3) {

                Text(
                    text =
                        "+ ${defects.size - 3} more defect${if (defects.size - 3 == 1) "" else "s"}",
                    style =
                        MaterialTheme.typography.labelMedium,
                    color =
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SellerClaimsCard(
    claims: List<String>
) {
    var expanded by
        remember {
            mutableStateOf(false)
        }

    SectionCard(
        title = "SELLER CLAIMS"
    ) {

        Text(
            text =
                "${claims.size} claims identified",
            style =
                MaterialTheme.typography.titleMedium,
            fontWeight =
                FontWeight.Bold
        )

        StatusBanner(
            text =
                "Seller claims — not independently verified",
            positive = false
        )

        ExpandableHeader(
            title =
                if (expanded) {
                    "Hide claims"
                } else {
                    "Show claims"
                },
            count =
                claims.size,
            expanded =
                expanded,
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
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
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
    val checked =
        remember(items) {
            mutableStateListOf<Boolean>()
                .apply {
                    repeat(items.size) {
                        add(false)
                    }
                }
        }

    SectionCard(
        title = "WHAT YOU SHOULD VERIFY"
    ) {

        Text(
            text =
                "Before buying this vehicle:",
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        items.forEachIndexed { index, item ->

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            checked[index] =
                                !checked[index]
                        },
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Checkbox(
                    checked =
                        checked[index],
                    onCheckedChange = {
                        checked[index] = it
                    }
                )

                Text(
                    text = item,
                    style =
                        MaterialTheme.typography.bodyMedium,
                    color =
                        if (checked[index]) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                )
            }
        }
    }
}

@Composable
private fun SellerQuestionsCard(
    questions: List<String>
) {
    var expanded by
        remember {
            mutableStateOf(false)
        }

    SectionCard(
        title = "QUESTIONS FOR THE SELLER"
    ) {

        ExpandableHeader(
            title =
                if (expanded) {
                    "Hide questions"
                } else {
                    "Show questions"
                },
            count =
                questions.size,
            expanded =
                expanded,
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
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                questions.forEachIndexed { index, question ->

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(3.dp)
                    ) {

                        Text(
                            text =
                                "%02d".format(
                                    index + 1
                                ),
                            style =
                                MaterialTheme.typography.labelLarge,
                            fontWeight =
                                FontWeight.ExtraBold,
                            color =
                                MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = question,
                            style =
                                MaterialTheme.typography.bodyMedium
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
            text =
                "${items.size} items were not supplied by the advert.",
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
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
private fun AdvancedReportDataCard(
    report: SavedReportData,
    jsonParser: Json
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
                .copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            ExpandableHeader(
                title = "ADVANCED REPORT DATA",
                count = null,
                expanded = expanded,
                onClick = {
                    expanded = !expanded
                }
            )

            Text(
                text = "The complete saved report data is available below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {

                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    SavedDataSection(
                        title = "OFFICIAL VEHICLE DATA"
                    ) {
                        SavedDataField(
                            "Make",
                            report.vehicle.string("make")
                        )

                        SavedDataField(
                            "Model",
                            report.vehicle.string("model")
                        )

                        SavedDataField(
                            "Year",
                            report.vehicle.string("yearOfManufacture")
                                ?: report.vehicle.string("year")
                        )

                        SavedDataField(
                            "Registration",
                            report.vehicle.string("registrationNumber")
                                ?: report.vehicle.string("registration")
                        )

                        SavedDataField(
                            "Colour",
                            report.vehicle.string("primaryColour")
                                ?: report.vehicle.string("colour")
                        )

                        SavedDataField(
                            "Engine Size",
                            report.vehicle.string("engineSize")
                        )

                        SavedDataField(
                            "Engine Capacity",
                            report.vehicle.int("engineCapacity")?.let {
                                "${it}cc"
                            }
                        )

                        SavedDataField(
                            "Fuel Type",
                            report.vehicle.string("fuelType")
                        )

                        SavedDataField(
                            "MOT Status",
                            report.vehicle.string("motStatus")
                        )

                        SavedDataField(
                            "MOT Expiry",
                            report.vehicle.string("motExpiryDate")
                                ?.let(::formatDate)
                        )

                        SavedDataField(
                            "Tax Status",
                            report.vehicle.string("taxStatus")
                        )

                        SavedDataField(
                            "Tax Due",
                            report.vehicle.string("taxDueDate")
                                ?.let(::formatDate)
                        )

                        SavedDataField(
                            "MOT Tests",
                            report.motTests.size.toString()
                        )
                    }

                    report.advert?.let { advert ->

                        SavedDataSection(
                            title = "SAVED ADVERT ANALYSIS"
                        ) {

                            SavedDataField(
                                "Make",
                                advert.make
                            )

                            SavedDataField(
                                "Model",
                                advert.model
                            )

                            SavedDataField(
                                "Year",
                                advert.year
                            )

                            SavedDataField(
                                "Advertised Price",
                                advert.price
                            )

                            SavedDataField(
                                "Advertised Mileage",
                                advert.mileage
                            )

                            SavedDataField(
                                "Engine Size",
                                advert.engineSize
                            )

                            SavedDataField(
                                "Fuel Type",
                                advert.fuelType
                            )

                            SavedDataField(
                                "Transmission",
                                advert.transmission
                            )

                            SavedDataField(
                                "Condition Score",
                                advert.score?.let {
                                    "$it / 100"
                                }
                            )

                            SavedDataField(
                                "Seller Claims",
                                advert.claims.size.toString()
                            )

                            SavedDataField(
                                "Missing Information",
                                advert.missing.size.toString()
                            )

                            SavedDataField(
                                "Verification Items",
                                advert.verify.size.toString()
                            )

                            SavedDataField(
                                "Seller Questions",
                                advert.questions.size.toString()
                            )

                            SavedDataField(
                                "Risk Flags",
                                advert.risks.size.toString()
                            )

                            SavedDataField(
                                "Key Insights",
                                advert.insights.size.toString()
                            )

                            SavedDataField(
                                "Inconsistencies",
                                advert.inconsistencies.size.toString()
                            )

                            advert.summary
                                ?.takeIf { it.isNotBlank() }
                                ?.let {
                                    SavedDataTextBlock(
                                        title = "Saved Advert Summary",
                                        text = it
                                    )
                                }
                        }
                    }

                    report.crossCheck?.let { cross ->

                        SavedDataSection(
                            title = "SAVED ADVERT / OFFICIAL EVIDENCE"
                        ) {

                            SavedDataField(
                                "Warnings",
                                cross.warnings.size.toString()
                            )

                            SavedDataField(
                                "Confirmations",
                                cross.confirmations.size.toString()
                            )

                            SavedDataField(
                                "Verification Items",
                                cross.verificationItems.size.toString()
                            )

                            if (cross.warnings.isNotEmpty()) {

                                SavedDataTextList(
                                    title = "Stored Warnings",
                                    items = cross.warnings
                                )
                            }

                            if (cross.confirmations.isNotEmpty()) {

                                SavedDataTextList(
                                    title = "Stored Confirmations",
                                    items = cross.confirmations
                                )
                            }

                            if (cross.verificationItems.isNotEmpty()) {

                                SavedDataTextList(
                                    title = "Stored Verification Items",
                                    items = cross.verificationItems
                                )
                            }
                        }
                    }

                    if (report.motTests.isNotEmpty()) {

                        SavedDataSection(
                            title = "SAVED MOT HISTORY"
                        ) {

                            report.motTests.forEachIndexed { index, test ->

                                SavedMotEntry(
                                    index = index,
                                    test = test
                                )
                            }
                        }
                    }

                    if (report.symptoms.isNotEmpty()) {

                        SavedDataSection(
                            title = "SAVED ACTIVE SYMPTOMS"
                        ) {

                            report.symptoms.forEach { symptom ->

                                SavedDataBullet(
                                    symptom.userDescription
                                        ?: "Unspecified symptom"
                                )
                            }
                        }
                    }

                    report.reportSummary
                        ?.takeIf { it.isNotBlank() }
                        ?.let {

                            SavedDataSection(
                                title = "SAVED REPORT SUMMARY"
                            ) {

                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                    report.createdAt
                        ?.takeIf { it.isNotBlank() }
                        ?.let {

                            SavedDataField(
                                "Report Saved",
                                formatSavedDate(it)
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun SavedDataSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider()

            content()
        }
    }
}

@Composable
private fun SavedDataField(
    label: String,
    value: String?
) {
    value
        ?.takeIf { it.isNotBlank() }
        ?.let {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.42f)
                )

                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(0.58f)
                )
            }
        }
}

@Composable
private fun SavedDataTextBlock(
    title: String,
    text: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SavedDataTextList(
    title: String,
    items: List<String>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        items.forEach { item ->
            SavedDataBullet(item)
        }
    }
}

@Composable
private fun SavedDataBullet(
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {

        Text(
            text = "•",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SavedMotEntry(
    index: Int,
    test: MotTest
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
                .copy(alpha = 0.45f)
        )
    ) {

        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            Text(
                text = "MOT Test ${index + 1}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            SavedDataField(
                "Date",
                test.completedDate?.let(::formatDate)
            )

            SavedDataField(
                "Result",
                test.testResult?.uppercase()
            )

            SavedDataField(
                "Mileage",
                listOfNotNull(
                    test.odometerValue,
                    test.odometerUnit
                ).joinToString(" ")
                    .takeIf { it.isNotBlank() }
            )

            val defects = test.defects
                .mapNotNull { it.text }
                .filter { it.isNotBlank() }

            if (defects.isNotEmpty()) {

                SavedDataTextList(
                    title = "Defects",
                    items = defects
                )
            }
        }
    }
}

@Composable
private fun RawJsonBlock(
    title: String,
    data: JsonObject,
    jsonParser: Json
) {
    var expanded by
        remember(title) {
            mutableStateOf(false)
        }

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(12.dp)
    ) {

        Column(
            modifier =
                Modifier.padding(12.dp)
        ) {

            ExpandableHeader(
                title =
                    title,
                count = null,
                expanded =
                    expanded,
                onClick = {
                    expanded = !expanded
                }
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {

                Text(
                    text =
                        jsonParser.encodeToString(
                            JsonObject.serializer(),
                            data
                        ),
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant
                        .copy(alpha = 0.35f)
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = title,
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight =
                    FontWeight.ExtraBold,
                color =
                    MaterialTheme.colorScheme.primary
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
    /*
     * The score displayed is the exact saved conditionScore.
     *
     * It is not clamped or recalculated.
     */
    val scoreColor =
        when {
            score >= 70 ->
                Color(0xFF1B8F4A)

            score >= 50 ->
                MaterialTheme.colorScheme.tertiary

            else ->
                MaterialTheme.colorScheme.error
        }

    Box(
        modifier =
            modifier
                .clip(
                    RoundedCornerShape(20.dp)
                )
                .background(
                    scoreColor.copy(
                        alpha = 0.12f
                    )
                ),
        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    score.toString(),
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight =
                    FontWeight.ExtraBold,
                color =
                    scoreColor
            )

            Text(
                text =
                    "/ 100",
                style =
                    MaterialTheme.typography.labelSmall,
                color =
                    scoreColor
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
        modifier =
            Modifier.weight(1f),
        color =
            if (positive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        shape =
            RoundedCornerShape(12.dp)
    ) {

        Column(
            modifier =
                Modifier.padding(12.dp)
        ) {

            Text(
                text = label,
                style =
                    MaterialTheme.typography.labelMedium,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onSurface
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
        color =
            MaterialTheme.colorScheme.surfaceVariant
                .copy(alpha = 0.55f),
        shape =
            RoundedCornerShape(12.dp)
    ) {

        Column(
            modifier =
                Modifier.padding(12.dp)
        ) {

            Text(
                text = label,
                style =
                    MaterialTheme.typography.labelMedium,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style =
                    MaterialTheme.typography.titleSmall,
                fontWeight =
                    FontWeight.Bold
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
        shape =
            RoundedCornerShape(50)
    ) {

        Text(
            text = text,
            modifier =
                Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                ),
            style =
                MaterialTheme.typography.labelMedium,
            fontWeight =
                FontWeight.Bold,
            color =
                foreground
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
        shape =
            RoundedCornerShape(12.dp)
    ) {

        Text(
            text = text,
            modifier =
                Modifier.padding(10.dp),
            style =
                MaterialTheme.typography.bodySmall,
            fontWeight =
                FontWeight.SemiBold,
            color =
                foreground
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = label,
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style =
                MaterialTheme.typography.bodyMedium,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}

@Composable
private fun BulletRow(
    text: String
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp),
        verticalAlignment =
            Alignment.Top
    ) {

        Text(
            text = "•",
            style =
                MaterialTheme.typography.bodyLarge,
            color =
                MaterialTheme.colorScheme.primary,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text = text,
            style =
                MaterialTheme.typography.bodyMedium,
            modifier =
                Modifier.weight(1f)
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
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                ),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text =
                if (count != null) {
                    "$title ($count)"
                } else {
                    title
                },
            style =
                MaterialTheme.typography.titleSmall,
            fontWeight =
                FontWeight.Bold,
            modifier =
                Modifier.weight(1f)
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
        shape =
            RoundedCornerShape(12.dp)
    ) {

        Column(
            modifier =
                Modifier.padding(12.dp),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = title,
                style =
                    MaterialTheme.typography.labelLarge,
                fontWeight =
                    FontWeight.Bold,
                color =
                    foreground
            )

            items.forEach { item ->

                Text(
                    text =
                        "• $item",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        foreground
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
    var expanded by
        remember(title) {
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
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            items.forEach {
                BulletRow(it)
            }
        }
    }
}

private fun parseIsoDate(
    value: String
): java.util.Date? {

    val candidates =
        listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX",
            "yyyy-MM-dd'T'HH:mm:ssX"
        )

    for (pattern in candidates) {

        runCatching {

            return SimpleDateFormat(
                pattern,
                Locale.UK
            ).apply {
                timeZone =
                    TimeZone.getTimeZone("UTC")
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
            timeZone =
                TimeZone.getDefault()
        }.format(it)

    } ?: value
}

private fun formatDate(
    value: String
): String {

    val date =
        parseIsoDate(value)
            ?: return value

    return SimpleDateFormat(
        "dd MMM yyyy",
        Locale.UK
    ).apply {
        timeZone =
            TimeZone.getDefault()
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
                ?.takeIf(
                    String::isNotBlank
                )
        }
        ?: emptyList()

private fun decodeMotTests(
    vehicle: JsonObject,
    jsonParser: Json
): List<MotTest> =
    vehicle["motTests"]
        ?.jsonArray
        ?.mapNotNull { element ->

            runCatching {
                jsonParser
                    .decodeFromJsonElement<MotTest>(
                        element
                    )
            }.getOrNull()
        }
        ?: emptyList()

private fun decodeSymptoms(
    vehicle: JsonObject,
    jsonParser: Json
): List<SymptomReport> {

    val element =
        vehicle["symptoms"]
            ?: vehicle["activeSymptoms"]
            ?: return emptyList()

    return element
        .jsonArray
        .mapNotNull { item ->

            runCatching {
                jsonParser
                    .decodeFromJsonElement<SymptomReport>(
                        item
                    )
            }.getOrNull()
        }
}

private fun buildRawAdvertObject(
    advert: AdvertUiData
): JsonObject =
    buildJsonObject {

        put(
            "make",
            advert.make
        )

        put(
            "model",
            advert.model
        )

        put(
            "year",
            advert.year
        )

        put(
            "price",
            advert.price
        )

        put(
            "mileage",
            advert.mileage
        )

        put(
            "engineSize",
            advert.engineSize
        )

        put(
            "fuelType",
            advert.fuelType
        )

        put(
            "transmission",
            advert.transmission
        )

        put(
            "conditionScore",
            advert.score
        )

        put(
            "summary",
            advert.summary
        )

        put(
            "claimsMadeBySeller",
            JsonArray(
                advert.claims.map(
                    ::JsonPrimitive
                )
            )
        )

        put(
            "missingInformation",
            JsonArray(
                advert.missing.map(
                    ::JsonPrimitive
                )
            )
        )

        put(
            "thingsWorthVerifying",
            JsonArray(
                advert.verify.map(
                    ::JsonPrimitive
                )
            )
        )

        put(
            "questionsTheBuyerShouldAsk",
            JsonArray(
                advert.questions.map(
                    ::JsonPrimitive
                )
            )
        )

        put(
            "riskFlags",
            JsonArray(
                advert.risks.map(
                    ::JsonPrimitive
                )
            )
        )

        put(
            "keyInsights",
            JsonArray(
                advert.insights.map(
                    ::JsonPrimitive
                )
            )
        )

        put(
            "inconsistencies",
            JsonArray(
                advert.inconsistencies.map(
                    ::JsonPrimitive
                )
            )
        )
    }

private fun buildRawCrossCheckObject(
    cross: CrossCheckUiData
): JsonObject =
    buildJsonObject {

        put(
            "warnings",
            JsonArray(
                cross.warnings.map(
                    ::JsonPrimitive
                )
            )
        )

        put(
            "confirmations",
            JsonArray(
                cross.confirmations.map(
                    ::JsonPrimitive
                )
            )
        )

        put(
            "verificationItems",
            JsonArray(
                cross.verificationItems.map(
                    ::JsonPrimitive
                )
            )
        )
    }

private fun shareSavedReport(
    context: Context,
    record: SavedVehicleRecord,
    jsonParser: Json
) {
    val pdfFile = com.hiddenhistory.reports.VehicleReportPdfGenerator.generate(context, record)
    val uri = com.hiddenhistory.reports.VehicleReportPdfGenerator.getShareUri(context, pdfFile)

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(
            Intent.EXTRA_TITLE,
            "Hidden History Vehicle Report - ${record.registration}"
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            "Share Vehicle Report PDF"
        )
    )
}
