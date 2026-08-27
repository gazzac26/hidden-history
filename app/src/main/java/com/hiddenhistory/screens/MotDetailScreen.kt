package com.hiddenhistory.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hiddenhistory.viewmodel.MotViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotScreen(
    viewModel: MotViewModel,
    onBackClick: () -> Unit
) {

    val selectedTest by
        viewModel
            .selectedMotTest
            .collectAsState()

    val context =
        LocalContext.current

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "MOT Details",
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBackClick
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
        }

    ) { innerPadding ->

        /*
         * --------------------------------------------------------
         * NO MOT SELECTED
         * --------------------------------------------------------
         */

        if (
            selectedTest == null
        ) {

            Box(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            innerPadding
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
                            Icons.Default.Assignment,

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
                            "No MOT test selected.",

                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge,

                        color =
                            MaterialTheme
                                .colorScheme
                                .outline,

                        fontWeight =
                            FontWeight.Medium
                    )
                }
            }

        } else {

            /*
             * ----------------------------------------------------
             * SELECTED MOT TEST
             * ----------------------------------------------------
             */

            val test =
                selectedTest!!

            val isPassed =
                test.testResult
                    .equals(
                        "PASSED",
                        ignoreCase = true
                    )

            val resultColor =
                if (isPassed) {

                    Color(
                        0xFF2ECC71
                    )

                } else {

                    MaterialTheme
                        .colorScheme
                        .error
                }

            val resultContainerColor =
                if (isPassed) {

                    Color(
                        0xFF2ECC71
                    ).copy(
                        alpha = 0.15f
                    )

                } else {

                    MaterialTheme
                        .colorScheme
                        .errorContainer
                        .copy(
                            alpha = 0.5f
                        )
                }

            LazyColumn(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            innerPadding
                        ),

                contentPadding =
                    PaddingValues(
                        horizontal = 20.dp,
                        vertical = 16.dp
                    ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        16.dp
                    )

            ) {

                /*
                 * ------------------------------------------------
                 * SUMMARY OVERVIEW
                 * ------------------------------------------------
                 */

                item {

                    Card(

                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(
                                24.dp
                            ),

                        colors =
                            CardDefaults
                                .cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceVariant
                                            .copy(
                                                alpha = 0.5f
                                            )
                                ),

                        elevation =
                            CardDefaults
                                .cardElevation(
                                    defaultElevation =
                                        1.dp
                                )
                    ) {

                        Column(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        20.dp
                                    ),

                            verticalArrangement =
                                Arrangement.spacedBy(
                                    16.dp
                                )

                        ) {

                            Row(

                                modifier =
                                    Modifier.fillMaxWidth(),

                                horizontalArrangement =
                                    Arrangement.SpaceBetween,

                                verticalAlignment =
                                    Alignment.CenterVertically

                            ) {

                                Column(

                                    verticalArrangement =
                                        Arrangement.spacedBy(
                                            2.dp
                                        )

                                ) {

                                    Text(

                                        text =
                                            "Test Result",

                                        style =
                                            MaterialTheme
                                                .typography
                                                .labelMedium,

                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )

                                    Text(

                                        text =
                                            test.testResult
                                                ?.uppercase()
                                                ?: "UNKNOWN",

                                        style =
                                            MaterialTheme
                                                .typography
                                                .headlineSmall,

                                        fontWeight =
                                            FontWeight.Bold,

                                        color =
                                            resultColor
                                    )
                                }

                                Box(

                                    modifier =
                                        Modifier
                                            .size(
                                                48.dp
                                            )
                                            .background(
                                                color =
                                                    resultContainerColor,
                                                shape =
                                                    CircleShape
                                            ),

                                    contentAlignment =
                                        Alignment.Center

                                ) {

                                    Icon(

                                        imageVector =
                                            if (isPassed) {

                                                Icons.Default
                                                    .CheckCircle

                                            } else {

                                                Icons.Default
                                                    .Cancel
                                            },

                                        contentDescription =
                                            null,

                                        tint =
                                            resultColor,

                                        modifier =
                                            Modifier.size(
                                                26.dp
                                            )
                                    )
                                }
                            }

                            HorizontalDivider(

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .outlineVariant
                                        .copy(
                                            alpha = 0.4f
                                        )
                            )

                            Row(

                                modifier =
                                    Modifier.fillMaxWidth(),

                                horizontalArrangement =
                                    Arrangement.SpaceBetween

                            ) {

                                Row(

                                    horizontalArrangement =
                                        Arrangement.spacedBy(
                                            8.dp
                                        ),

                                    verticalAlignment =
                                        Alignment.CenterVertically

                                ) {

                                    Icon(

                                        imageVector =
                                            Icons.Default
                                                .CalendarToday,

                                        contentDescription =
                                            null,

                                        tint =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,

                                        modifier =
                                            Modifier.size(
                                                18.dp
                                            )
                                    )

                                    Column {

                                        Text(

                                            text =
                                                "Completed Date",

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
                                                test.completedDate
                                                    ?: "N/A",

                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .bodyMedium,

                                            fontWeight =
                                                FontWeight.SemiBold
                                        )
                                    }
                                }

                                Row(

                                    horizontalArrangement =
                                        Arrangement.spacedBy(
                                            8.dp
                                        ),

                                    verticalAlignment =
                                        Alignment.CenterVertically

                                ) {

                                    Icon(

                                        imageVector =
                                            Icons.Default.Speed,

                                        contentDescription =
                                            null,

                                        tint =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,

                                        modifier =
                                            Modifier.size(
                                                18.dp
                                            )
                                    )

                                    Column {

                                        Text(

                                            text =
                                                "Odometer",

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
                                                "${test.odometerValue ?: "N/A"} ${
                                                    test.odometerUnit
                                                        ?: "MI"
                                                }",

                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .bodyMedium,

                                            fontWeight =
                                                FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                /*
                 * ------------------------------------------------
                 * DEFECTS HEADER
                 * ------------------------------------------------
                 */

                item {

                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp
                            )
                    )

                    Text(

                        text =
                            "Defects & Advisory Notes",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onBackground
                    )
                }

                /*
                 * ------------------------------------------------
                 * NO DEFECTS
                 * ------------------------------------------------
                 */

                if (
                    test.defects.isNullOrEmpty()
                ) {

                    item {

                        Card(

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(
                                    16.dp
                                ),

                            colors =
                                CardDefaults
                                    .cardColors(
                                        containerColor =
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant
                                                .copy(
                                                    alpha = 0.3f
                                                )
                                    ),

                            elevation =
                                CardDefaults
                                    .cardElevation(
                                        defaultElevation =
                                            0.dp
                                    )
                        ) {

                            Row(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            16.dp
                                        ),

                                verticalAlignment =
                                    Alignment.CenterVertically,

                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        12.dp
                                    )

                            ) {

                                Icon(

                                    imageVector =
                                        Icons.Default
                                            .CheckCircle,

                                    contentDescription =
                                        null,

                                    tint =
                                        Color(
                                            0xFF2ECC71
                                        ),

                                    modifier =
                                        Modifier.size(
                                            24.dp
                                        )
                                )

                                Text(

                                    text =
                                        "No recorded defects or advisories for this test.",

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

                } else {

                    /*
                     * ------------------------------------------------
                     * DEFECT LIST
                     * ------------------------------------------------
                     */

                    itemsIndexed(
                        test.defects
                    ) { index, defect ->

                        val defectType =
                            defect.type
                                ?: "ADVISORY"

                        val defectText =
                            defect.text
                                ?: defect.toString()

                        val isDangerous =
                            defect.dangerous == true

                        val badgeColor =
                            if (isDangerous) {

                                MaterialTheme
                                    .colorScheme
                                    .error

                            } else {

                                MaterialTheme
                                    .colorScheme
                                    .primary
                            }

                        Card(

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(
                                    16.dp
                                ),

                            colors =
                                CardDefaults
                                    .cardColors(
                                        containerColor =
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant
                                                .copy(
                                                    alpha = 0.4f
                                                )
                                    ),

                            elevation =
                                CardDefaults
                                    .cardElevation(
                                        defaultElevation =
                                            0.dp
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
                                        12.dp
                                    )

                            ) {

                                Row(

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    horizontalArrangement =
                                        Arrangement.spacedBy(
                                            16.dp
                                        ),

                                    verticalAlignment =
                                        Alignment.Top

                                ) {

                                    Icon(

                                        imageVector =
                                            if (
                                                isDangerous
                                            ) {

                                                Icons.Default
                                                    .Warning

                                            } else {

                                                Icons.Default
                                                    .Info
                                            },

                                        contentDescription =
                                            null,

                                        tint =
                                            badgeColor,

                                        modifier =
                                            Modifier
                                                .size(
                                                    20.dp
                                                )
                                                .padding(
                                                    top = 2.dp
                                                )
                                    )

                                    Column(

                                        modifier =
                                            Modifier.weight(
                                                1f
                                            ),

                                        verticalArrangement =
                                            Arrangement.spacedBy(
                                                4.dp
                                            )

                                    ) {

                                        Text(

                                            text =
                                                "${index + 1}. $defectType",

                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .titleSmall,

                                            fontWeight =
                                                FontWeight.Bold,

                                            color =
                                                badgeColor
                                        )

                                        Text(

                                            text =
                                                defectText,

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

                                /*
                                 * ------------------------------------
                                 * EBAY SEARCH
                                 * ------------------------------------
                                 */

                                Row(

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    horizontalArrangement =
                                        Arrangement.End

                                ) {

                                    TextButton(

                                        onClick = {

                                            viewModel.searchEbayPart(

                                                defectText =
                                                    defectText,

                                                onSuccess = { responseData ->

                                                    try {

                                                        val ebayUrl =
                                                            responseData.trim()

                                                        if (
                                                            ebayUrl.startsWith(
                                                                "http://",
                                                                ignoreCase = true
                                                            ) ||
                                                            ebayUrl.startsWith(
                                                                "https://",
                                                                ignoreCase = true
                                                            )
                                                        ) {

                                                            Log.d(
                                                                "MotScreen",
                                                                "Opening eBay search: $ebayUrl"
                                                            )

                                                            context.startActivity(

                                                                android.content.Intent(
                                                                    android.content.Intent.ACTION_VIEW,
                                                                    android.net.Uri.parse(
                                                                        ebayUrl
                                                                    )
                                                                )
                                                            )

                                                        } else {

                                                            Log.e(
                                                                "MotScreen",
                                                                "eBay search returned an invalid URL: $responseData"
                                                            )
                                                        }

                                                    } catch (e: Throwable) {

                                                        Log.e(
                                                            "MotScreen",
                                                            "Failed to open eBay search result",
                                                            e
                                                        )
                                                    }
                                                },

                                                onError = { error ->

                                                    Log.e(
                                                        "MotScreen",
                                                        "eBay search failed: $error"
                                                    )
                                                }
                                            )
                                        },

                                        contentPadding =
                                            PaddingValues(
                                                horizontal = 12.dp,
                                                vertical = 4.dp
                                            )

                                    ) {

                                        Icon(

                                            imageVector =
                                                Icons.Default
                                                    .ShoppingCart,

                                            contentDescription =
                                                null,

                                            modifier =
                                                Modifier.size(
                                                    16.dp
                                                ),

                                            tint =
                                                MaterialTheme
                                                    .colorScheme
                                                    .primary
                                        )

                                        Spacer(

                                            modifier =
                                                Modifier.width(
                                                    6.dp
                                                )
                                        )

                                        Text(

                                            text =
                                                "Find on eBay",

                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .labelMedium,

                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .primary,

                                            fontWeight =
                                                FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {

                    Spacer(
                        modifier =
                            Modifier.height(
                                24.dp
                            )
                    )
                }
            }
        }
    }
}
