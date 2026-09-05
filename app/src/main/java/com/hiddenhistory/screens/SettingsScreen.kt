package com.hiddenhistory.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hiddenhistory.viewmodel.SettingsViewModel


@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToVehicleDisclaimer: () -> Unit,
    onNavigateToAffiliateDisclosure: () -> Unit,
    onNavigateToAccountDeletion: () -> Unit,
    onNavigateToInteractiveAccountDeletion: () -> Unit
) {

    // ------------------------------------------------------------------------
    // Context and Runtime Permission Launcher
    // ------------------------------------------------------------------------

    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                viewModel.setNotificationsEnabled(true, context)
            }
        }


    // ------------------------------------------------------------------------
    // Observe settings state
    // ------------------------------------------------------------------------

    val notificationsEnabled by
        viewModel.notificationsEnabled
            .collectAsStateWithLifecycle()

    val analysisWarningsEnabled by
        viewModel.analysisWarningsEnabled
            .collectAsStateWithLifecycle()


    // ------------------------------------------------------------------------
    // Screen
    // ------------------------------------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(24.dp),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        // --------------------------------------------------------------------
        // Header
        // --------------------------------------------------------------------

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )


        // --------------------------------------------------------------------
        // Account
        // --------------------------------------------------------------------

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column {

                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp
                    )
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                ListItem(
                    headlineContent = {
                        Text("Profile")
                    },
                    supportingContent = {
                        Text(
                            "View and edit your personal information"
                        )
                    },
                    trailingContent = {
                        TextButton(
                            onClick = {
                                onNavigateToProfile()
                            }
                        ) {
                            Text("Open")
                        }
                    }
                )
            }
        }


        // --------------------------------------------------------------------
        // App settings
        // --------------------------------------------------------------------

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column {

                Text(
                    text = "App Settings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp
                    )
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )


                // Notifications

                ListItem(
                    headlineContent = {
                        Text("Notifications")
                    },
                    supportingContent = {
                        Text(
                            "Allow Hidden History to provide notifications"
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked =
                                notificationsEnabled,

                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (!hasPermission) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        return@Switch
                                    }
                                }
                                viewModel.setNotificationsEnabled(enabled, context)
                            }
                        )
                    }
                )


                HorizontalDivider()


                // Analysis warnings

                ListItem(
                    headlineContent = {
                        Text("Analysis Warnings")
                    },
                    supportingContent = {
                        Text(
                            "Show important warnings when reviewing vehicle information"
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked =
                                analysisWarningsEnabled,

                            onCheckedChange = {
                                viewModel
                                    .setAnalysisWarningsEnabled(
                                        it
                                    )
                            }
                        )
                    }
                )
            }
        }


        // --------------------------------------------------------------------
        // Legal & Privacy
        // --------------------------------------------------------------------

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column {

                Text(
                    text = "Legal & Privacy",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp
                    )
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )


                ListItem(
                    headlineContent = {
                        Text("Terms & Conditions")
                    },
                    supportingContent = {
                        Text(
                            "Read the Hidden History terms"
                        )
                    },
                    trailingContent = {
                        TextButton(
                            onClick = {
                                onNavigateToTerms()
                            }
                        ) {
                            Text("View")
                        }
                    }
                )


                HorizontalDivider()


                ListItem(
                    headlineContent = {
                        Text("Privacy Policy")
                    },
                    supportingContent = {
                        Text(
                            "Read how Hidden History handles information"
                        )
                    },
                    trailingContent = {
                        TextButton(
                            onClick = {
                                onNavigateToPrivacy()
                            }
                        ) {
                            Text("View")
                        }
                    }
                )


                HorizontalDivider()


                ListItem(
                    headlineContent = {
                        Text("Vehicle Data & Analysis")
                    },
                    supportingContent = {
                        Text(
                            "Important information about vehicle data and analysis"
                        )
                    },
                    trailingContent = {
                        TextButton(
                            onClick = {
                                onNavigateToVehicleDisclaimer()
                            }
                        ) {
                            Text("View")
                        }
                    }
                )


                HorizontalDivider()


                ListItem(
                    headlineContent = {
                        Text("Affiliate Disclosure")
                    },
                    supportingContent = {
                        Text(
                            "Information about affiliate links"
                        )
                    },
                    trailingContent = {
                        TextButton(
                            onClick = {
                                onNavigateToAffiliateDisclosure()
                            }
                        ) {
                            Text("View")
                        }
                    }
                )


                HorizontalDivider()


                ListItem(
                    headlineContent = {
                        Text("Account Deletion")
                    },
                    supportingContent = {
                        Text(
                            "Request deletion of your Hidden History account and associated data"
                        )
                    },
                    trailingContent = {
                        TextButton(
                            onClick = {
                                onNavigateToAccountDeletion()
                            }
                        ) {
                            Text("View")
                        }
                    }
                )
            }
        }


        // --------------------------------------------------------------------
        // Account & Data
        // --------------------------------------------------------------------

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column {

                Text(
                    text = "Account & Data",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp
                    )
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )


                ListItem(
                    headlineContent = {
                        Text("Account Deletion")
                    },
                    supportingContent = {
                        Text(
                            "Request deletion of your Hidden History account and associated data"
                        )
                    },
                    trailingContent = {
                        TextButton(
                            onClick = {
                                onNavigateToInteractiveAccountDeletion()
                            }
                        ) {
                            Text("Manage")
                        }
                    }
                )
            }
        }


        // --------------------------------------------------------------------
        // Application information
        // --------------------------------------------------------------------

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "About Hidden History",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Hidden History"
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "Vehicle information and analysis"
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "Version: TODO"
                )
            }
        }
    }
}
