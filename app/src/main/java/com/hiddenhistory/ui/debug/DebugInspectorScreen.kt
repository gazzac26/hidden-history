package com.hiddenhistory.ui.debug

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hiddenhistory.database.AppDatabase
import java.lang.reflect.Field
import java.lang.reflect.Modifier as JavaModifier
import kotlinx.coroutines.launch

/*
 * =========================================================
 * OBJECT INSPECTION
 * =========================================================
 *
 * Recursively converts Maps, Lists, arrays and normal objects
 * into readable key/value structures.
 *
 * This is deliberately generic so the Debug screen can inspect
 * advert analysis and cross-check results without requiring changes
 * to those production models.
 */

private fun inspectValue(
    value: Any?,
    visited: MutableSet<Int> = mutableSetOf()
): Any? {

    if (value == null) {
        return null
    }

    /*
     * Primitive / simple values.
     */

    if (
        value is String ||
        value is Number ||
        value is Boolean ||
        value is Char ||
        value is Enum<*>
    ) {
        return value
    }

    /*
     * Prevent recursive object loops.
     */

    val identity =
        System.identityHashCode(value)

    if (
        !visited.add(identity)
    ) {
        return "<recursive reference>"
    }

    try {

        /*
         * Map
         */

        if (
            value is Map<*, *>
        ) {

            return value.entries.associate { entry ->

                entry.key.toString() to
                    inspectValue(
                        entry.value,
                        visited
                    )
            }
        }

        /*
         * Iterable / List / Set
         */

        if (
            value is Iterable<*>
        ) {

            return value.map { item ->

                inspectValue(
                    item,
                    visited
                )
            }
        }

        /*
         * Arrays
         */

        if (
            value.javaClass.isArray
        ) {

            val length =
                java.lang.reflect.Array.getLength(
                    value
                )

            return (0 until length).map { index ->

                inspectValue(
                    java.lang.reflect.Array.get(
                        value,
                        index
                    ),
                    visited
                )
            }
        }

        /*
         * Normal object.
         */

        val result =
            linkedMapOf<String, Any?>()

        var currentClass:
            Class<*>? =
            value.javaClass

        while (
            currentClass != null &&
            currentClass != Any::class.java
        ) {

            val fields =
                currentClass.declaredFields

            for (
                field in fields
            ) {

                /*
                 * Ignore compiler/internal/static fields.
                 */

                if (
                    field.name.startsWith("$")
                ) {
                    continue
                }

                if (
                    field.isSynthetic
                ) {
                    continue
                }

                if (
                    JavaModifier.isStatic(
                        field.modifiers
                    )
                ) {
                    continue
                }

                try {

                    field.isAccessible =
                        true

                    result[field.name] =
                        inspectValue(
                            field.get(value),
                            visited
                        )

                } catch (
                    e: Throwable
                ) {

                    result[field.name] =
                        "<unable to read: ${e.message}>"
                }
            }

            currentClass =
                currentClass.superclass
        }

        if (
            result.isEmpty()
        ) {
            return value.toString()
        }

        return result

    } finally {

        visited.remove(identity)
    }
}

/*
 * =========================================================
 * INSPECTION MAP
 * =========================================================
 */

private fun Any?.toInspectionValue(): Any? {

    return inspectValue(
        this,
        mutableSetOf()
    )
}

/*
 * =========================================================
 * MAP NORMALISATION
 * =========================================================
 */

@Suppress("UNCHECKED_CAST")
private fun Any?.toInspectionMap():
    Map<String, Any?> {

    val inspected =
        this.toInspectionValue()

    return when (
        inspected
    ) {

        is Map<*, *> -> {

            inspected.entries.associate {

                it.key.toString() to
                    it.value
            }
        }

        null -> {
            emptyMap()
        }

        else -> {

            mapOf(
                "value" to inspected
            )
        }
    }
}

/*
 * =========================================================
 * READABLE VALUE FORMATTER
 * =========================================================
 */

private fun formatInspectionValue(
    value: Any?,
    indent: Int = 0
): String {

    val spacing =
        " ".repeat(
            indent
        )

    return when (
        value
    ) {

        null ->
            "NULL"

        is Map<*, *> -> {

            if (
                value.isEmpty()
            ) {
                "{}"
            } else {

                buildString {

                    value.forEach {
                        (key, childValue) ->

                        append(
                            spacing
                        )

                        append(
                            key
                        )

                        append(
                            " = "
                        )

                        when (
                            childValue
                        ) {

                            is Map<*, *> -> {

                                append(
                                    "\n"
                                )

                                append(
                                    formatInspectionValue(
                                        childValue,
                                        indent + 4
                                    )
                                )
                            }

                            is List<*> -> {

                                if (
                                    childValue.isEmpty()
                                ) {

                                    append(
                                        "[]"
                                    )

                                } else {

                                    append(
                                        "\n"
                                    )

                                    childValue.forEachIndexed {
                                        index,
                                        item ->

                                        append(
                                            spacing
                                        )

                                        append(
                                            "    [$index]"
                                        )

                                        when (
                                            item
                                        ) {

                                            is Map<*, *>,
                                            is List<*> -> {

                                                append(
                                                    "\n"
                                                )

                                                append(
                                                    formatInspectionValue(
                                                        item,
                                                        indent + 8
                                                    )
                                                )
                                            }

                                            else -> {

                                                append(
                                                    " = "
                                                )

                                                append(
                                                    item
                                                        ?: "NULL"
                                                )
                                            }
                                        }

                                        append(
                                            "\n"
                                        )
                                    }
                                }
                            }

                            else -> {

                                append(
                                    childValue
                                        ?: "NULL"
                                )
                            }
                        }

                        append(
                            "\n"
                        )
                    }
                }.trimEnd()
            }
        }

        is List<*> -> {

            if (
                value.isEmpty()
            ) {
                "[]"
            } else {

                buildString {

                    value.forEachIndexed {
                        index,
                        item ->

                        append(
                            spacing
                        )

                        append(
                            "[$index]"
                        )

                        when (
                            item
                        ) {

                            is Map<*, *>,
                            is List<*> -> {

                                append(
                                    "\n"
                                )

                                append(
                                    formatInspectionValue(
                                        item,
                                        indent + 4
                                    )
                                )
                            }

                            else -> {

                                append(
                                    " = "
                                )

                                append(
                                    item
                                        ?: "NULL"
                                )
                            }
                        }

                        append(
                            "\n"
                        )
                    }
                }.trimEnd()
            }
        }

        else ->
            value.toString()
    }
}

/*
 * =========================================================
 * SECTION TEXT
 * =========================================================
 */

private fun buildInspectionText(
    title: String,
    value: Any?
): String {

    return buildString {

        append(
            "--- "
        )

        append(
            title
        )

        append(
            " ---\n"
        )

        val inspected =
            value.toInspectionValue()

        append(
            formatInspectionValue(
                inspected
            )
        )

        append(
            "\n"
        )
    }
}

/*
 * =========================================================
 * SECTION COMPOSABLE
 * =========================================================
 */

@Composable
private fun InspectionCard(
    title: String,
    value: Any?,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    titleColor: Color? = null
) {

    val clipboardManager =
        LocalClipboardManager.current

    val text =
        remember(
            title,
            value
        ) {

            buildInspectionText(
                title,
                value
            )
        }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .pointerInput(
                    text
                ) {

                    detectTapGestures(
                        onLongPress = {

                            clipboardManager.setText(
                                AnnotatedString(
                                    text
                                )
                            )
                        }
                    )
                },
        colors =
            if (
                containerColor != null
            ) {

                CardDefaults.cardColors(
                    containerColor =
                        containerColor
                )

            } else {

                CardDefaults.cardColors()
            }
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
        ) {

            Text(
                text =
                    "$title (Hold to copy)",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.Bold,
                color =
                    titleColor
                        ?: MaterialTheme
                            .colorScheme
                            .onSurface
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            if (
                value == null
            ) {

                Text(
                    text =
                        "No data recorded.",
                    color =
                        Color.Gray,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

            } else {

                val inspected =
                    value.toInspectionValue()

                if (
                    inspected is Map<*, *> &&
                    inspected.isEmpty()
                ) {

                    Text(
                        text =
                            "No data recorded.",
                        color =
                            Color.Gray,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                } else {

                    Text(
                        text =
                            formatInspectionValue(
                                inspected
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }
        }
    }
}

/*
 * =========================================================
 * DEBUG INSPECTOR SCREEN
 * =========================================================
 */

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun DebugInspectorScreen(
    onNavigateBack: () -> Unit
) {

    val context =
        LocalContext.current

    val scrollState =
        rememberScrollState()

    val clipboardManager =
        LocalClipboardManager.current

    val coroutineScope =
        rememberCoroutineScope()

    /*
     * =========================================================
     * DEBUG STATE
     * =========================================================
     */

    val rawProviderData =
        DebugStateHolder.rawProviderData

    val advertAnalysisResult =
        DebugStateHolder.advertAnalysisResult

    val officialCrossCheckResult =
        DebugStateHolder.officialCrossCheckResult

    /*
     * Convert all complex objects into readable structures.
     */

    val rawInspection =
        remember(
            rawProviderData
        ) {

            rawProviderData
                .toInspectionMap()
        }

    val advertInspection =
        remember(
            advertAnalysisResult
        ) {

            advertAnalysisResult
                .toInspectionMap()
        }

    val crossCheckInspection =
        remember(
            officialCrossCheckResult
        ) {

            officialCrossCheckResult
                .toInspectionMap()
        }

    /*
     * =========================================================
     * STARTUP DIAGNOSTICS
     * =========================================================
     */

    var startupStats by
        remember {

            mutableStateOf<
                Map<String, Any>
            >(
                emptyMap()
            )
        }

    var isLoadingStartupStats by
        remember {

            mutableStateOf(
                true
            )
        }

    fun loadStartupDiagnostics() {

        coroutineScope.launch {

            isLoadingStartupStats =
                true

            val stats =
                mutableMapOf<
                    String,
                    Any
                >()

            try {

                val db =
                    AppDatabase
                        .getDatabase(
                            context
                        )

                val catalogDao =
                    db.vehicleCatalogDao()

                val startTime =
                    System.currentTimeMillis()

                val makes =
                    catalogDao.getAllMakes()

                val loadDuration =
                    System.currentTimeMillis() -
                        startTime

                stats[
                    "Catalog Status"
                ] =
                    "Loaded Successfully"

                stats[
                    "Total Unique Makes"
                ] =
                    makes.size

                stats[
                    "First 5 Makes"
                ] =
                    makes
                        .take(5)
                        .joinToString(
                            ", "
                        )

                stats[
                    "Catalog Load Time"
                ] =
                    "${loadDuration}ms"

                stats[
                    "Database Instance"
                ] =
                    "Active (Singleton)"

                stats[
                    "CSV Asset Parsing"
                ] =
                    "Operational"

            } catch (
                e: Throwable
            ) {

                stats[
                    "Catalog Status"
                ] =
                    "Error: ${e.localizedMessage}"

                Log.e(
                    "DebugInspectorScreen",
                    "Failed to load startup catalog stats",
                    e
                )
            }

            startupStats =
                stats

            isLoadingStartupStats =
                false
        }
    }

    LaunchedEffect(
        Unit
    ) {

        loadStartupDiagnostics()
    }

    /*
     * =========================================================
     * WASTED / UNMAPPED DATA
     * =========================================================
     */

    val handledKeys =
        setOf(

            "registrationNumber",
            "registration",
            "make",
            "model",
            "year",
            "vin",
            "engineSize",
            "colour",
            "primaryColour",
            "wheelplan",
            "engineCapacity",
            "fuelType",
            "co2Emissions",
            "typeApproval",
            "seats",
            "maxTowWeight",
            "registrationDate",
            "monthOfFirstRegistration",
            "manufactureDate",
            "firstUsedDate",
            "dateOfLastV5CIssued",
            "previousKeepers",
            "previousOwners",
            "taxStatus",
            "taxDueDate",
            "motStatus",
            "motExpiryDate",
            "price",
            "hasOutstandingRecall",
            "salvageCategory",
            "vehicleTier",
            "markedForExport",
            "motTests",
            "activeSymptoms",
            "userId"
        )

    val wastedData =
        rawProviderData
            ?.filterKeys {
                it !in handledKeys
            }

    /*
     * =========================================================
     * COPY EVERYTHING
     * =========================================================
     */

    val allDebugText =
        remember(
            startupStats,
            rawProviderData,
            advertAnalysisResult,
            officialCrossCheckResult,
            wastedData
        ) {

            buildString {

                append(
                    "--- APP STARTUP & CATALOG DIAGNOSTICS ---\n"
                )

                startupStats.forEach {
                    (key, value) ->

                    append(
                        "$key = $value\n"
                    )
                }

                append(
                    "\n"
                )

                append(
                    buildInspectionText(
                        "RAW PROVIDER DATA",
                        rawProviderData
                    )
                )

                append(
                    "\n"
                )

                append(
                    buildInspectionText(
                        "WASTED / UNMAPPED DATA",
                        wastedData
                    )
                )

                append(
                    "\n"
                )

                append(
                    buildInspectionText(
                        "ADVERT ANALYSIS RESULT",
                        advertAnalysisResult
                    )
                )

                append(
                    "\n"
                )

                append(
                    buildInspectionText(
                        "OFFICIAL ADVERT CROSS-CHECK RESULT",
                        officialCrossCheckResult
                    )
                )
            }
        }

    /*
     * =========================================================
     * SCREEN
     * =========================================================
     */

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        "Pipeline Inspector",
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
                                Icons
                                    .AutoMirrored
                                    .Filled
                                    .ArrowBack,
                            contentDescription =
                                "Back"
                        )
                    }
                }
            )
        }

    ) { innerPadding ->

        Surface(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    ),

            color =
                MaterialTheme
                    .colorScheme
                    .background

        ) {

            Column(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(
                            scrollState
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        16.dp
                    )
            ) {

                /*
                 * =================================================
                 * ACTION BUTTONS
                 * =================================================
                 */

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    Button(

                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        onClick = {

                            clipboardManager
                                .setText(
                                    AnnotatedString(
                                        allDebugText
                                    )
                                )
                        }

                    ) {

                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription =
                                "Copy",
                            modifier =
                                Modifier.size(
                                    18.dp
                                )
                        )

                        Spacer(
                            modifier =
                                Modifier.width(
                                    6.dp
                                )
                        )

                        Text(
                            "Copy All Data"
                        )
                    }

                    OutlinedButton(

                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        onClick = {

                            DebugStateHolder
                                .clear()

                            loadStartupDiagnostics()
                        }

                    ) {

                        Icon(
                            Icons.Default.Refresh,
                            contentDescription =
                                "Refresh",
                            modifier =
                                Modifier.size(
                                    18.dp
                                )
                        )

                        Spacer(
                            modifier =
                                Modifier.width(
                                    6.dp
                                )
                        )

                        Text(
                            "Refresh"
                        )
                    }
                }

                /*
                 * =================================================
                 * TIP
                 * =================================================
                 */

                Text(

                    text =
                        "💡 Tip: Long-press any section below to copy its complete structured data.",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                /*
                 * =================================================
                 * 0. STARTUP
                 * =================================================
                 */

                InspectionCard(

                    title =
                        "App Startup & Catalog Diagnostics",

                    value =
                        startupStats,

                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                            .copy(
                                alpha = 0.4f
                            ),

                    titleColor =
                        MaterialTheme
                            .colorScheme
                            .primary
                )

                if (
                    isLoadingStartupStats
                ) {

                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically,

                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(
                                    16.dp
                                ),
                            strokeWidth =
                                2.dp
                        )

                        Text(
                            "Loading catalog metrics...",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                Color.Gray
                        )
                    }
                }

                /*
                 * =================================================
                 * 1. RAW PROVIDER DATA
                 * =================================================
                 */

                InspectionCard(

                    title =
                        "Full Raw Provider Data",

                    value =
                        rawProviderData
                )

                /*
                 * =================================================
                 * 2. WASTED DATA
                 * =================================================
                 */

                InspectionCard(

                    title =
                        "Wasted / Unmapped API Data",

                    value =
                        wastedData,

                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .errorContainer
                            .copy(
                                alpha = 0.3f
                            ),

                    titleColor =
                        MaterialTheme
                            .colorScheme
                            .error
                )

                /*
                 * =================================================
                 * 3. ADVERT ANALYSIS
                 * =================================================
                 */

                InspectionCard(

                    title =
                        "Advert Analysis Result",

                    value =
                        advertAnalysisResult,

                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .tertiaryContainer
                            .copy(
                                alpha = 0.3f
                            ),

                    titleColor =
                        MaterialTheme
                            .colorScheme
                            .tertiary
                )

                /*
                 * =================================================
                 * 4. OFFICIAL ADVERT CROSS-CHECK
                 * =================================================
                 */

                InspectionCard(

                    title =
                        "Official Advert Cross-Check Result",

                    value =
                        officialCrossCheckResult,

                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .secondaryContainer
                            .copy(
                                alpha = 0.3f
                            ),

                    titleColor =
                        MaterialTheme
                            .colorScheme
                            .secondary
                )
            }
        }
    }
}