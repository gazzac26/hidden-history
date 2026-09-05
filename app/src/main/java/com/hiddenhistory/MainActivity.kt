package com.hiddenhistory

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hiddenhistory.billing.VehicleReportBillingManager
import com.hiddenhistory.billing.VehicleReportTokenManager
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.database.AppDatabase
import com.hiddenhistory.database.SettingsDao
import com.hiddenhistory.screens.AdvertAnalyzerScreen
import com.hiddenhistory.screens.DeleteAccountScreen
import com.hiddenhistory.screens.FreeVehicleSearchScreen
import com.hiddenhistory.screens.LegalDocumentScreen
import com.hiddenhistory.screens.LoginScreen
import com.hiddenhistory.screens.MotScreen
import com.hiddenhistory.screens.ProVehicleSearchScreen
import com.hiddenhistory.screens.SavedVehicleReportsScreen
import com.hiddenhistory.screens.SettingsScreen
import com.hiddenhistory.screens.UserProfileEditScreen
import com.hiddenhistory.screens.UserProfileScreen
import com.hiddenhistory.screens.VehicleReportPaymentScreen
import com.hiddenhistory.screens.VehicleReportScreen
import com.hiddenhistory.screens.VehicleSearchGatewayScreen
import com.hiddenhistory.ui.debug.DebugInspectorScreen
import com.hiddenhistory.ui.theme.HiddenHistoryTheme
import com.hiddenhistory.viewmodel.AdvertAnalysisUiState
import com.hiddenhistory.viewmodel.AdvertAnalysisViewModel
import com.hiddenhistory.viewmodel.FreeVehicleSearchViewModel
import com.hiddenhistory.viewmodel.MotViewModel
import com.hiddenhistory.viewmodel.ProfileViewModel
import com.hiddenhistory.viewmodel.SettingsViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()

        SupabaseManager.init(
            applicationContext
        )

        setContent {

            HiddenHistoryTheme {

                MainScreen()
            }
        }
    }
}


/*
 * =====================================================================
 * SCREEN ROUTES
 * =====================================================================
 */

sealed class Screen(
    val route: String,
    val title: String,
    val icon:
        androidx.compose.ui.graphics.vector.ImageVector
) {

    object Home : Screen(
        "home",
        "Home",
        Icons.Default.Home
    )

    object Explore : Screen(
        "explore",
        "Vehicle Search",
        Icons.Default.Search
    )

    object FreeVehicleSearch : Screen(
        "free_vehicle_search",
        "Free Vehicle Search",
        Icons.Default.Search
    )

    object ProVehicleSearch : Screen(
        "pro_vehicle_search",
        "Pro Vehicle Search",
        Icons.Default.Search
    )

    object SavedReports : Screen(
        "saved_reports",
        "Saved Reports",
        Icons.Default.Favorite
    )

    object Profile : Screen(
        "profile",
        "Profile",
        Icons.Default.Home
    )

    object EditProfile : Screen(
        "edit_profile",
        "Edit Profile",
        Icons.Default.Edit
    )

    object Settings : Screen(
        "settings",
        "Settings",
        Icons.Default.Settings
    )

    object Terms : Screen(
        "terms",
        "Terms & Conditions",
        Icons.Default.Search
    )

    object Privacy : Screen(
        "privacy",
        "Privacy Policy",
        Icons.Default.Search
    )

    object VehicleDisclaimer : Screen(
        "vehicle_disclaimer",
        "Vehicle Data & Analysis",
        Icons.Default.Search
    )

    object AffiliateDisclosure : Screen(
        "affiliate_disclosure",
        "Affiliate Disclosure",
        Icons.Default.Search
    )

    object AccountDeletion : Screen(
        "account_deletion",
        "Account Deletion",
        Icons.Default.Search
    )

    object InteractiveAccountDeletion : Screen(
        "interactive_account_deletion",
        "Delete Account",
        Icons.Default.Search
    )

    object Login : Screen(
        "login",
        "Login",
        Icons.Default.Home
    )

    object MotDetail : Screen(
        "mot_detail/{motId}",
        "MOT Detail",
        Icons.Default.Search
    )

    object VehicleReportPayment : Screen(
        "vehicle_report_payment",
        "Pro Vehicle Search",
        Icons.Default.Search
    )

    object VehicleReport : Screen(
        "vehicle_report/{advertText}",
        "Full Pro Vehicle Search",
        Icons.Default.Search
    )

    object AdvertAnalyzer : Screen(
        "advert_analyzer/{advertText}",
        "Advert Analyzer",
        Icons.Default.Search
    )

    object DebugInspector : Screen(
        "debug_inspector",
        "Debug",
        Icons.Default.Search
    )
}


/*
 * =====================================================================
 * MAIN SCREEN
 * =====================================================================
 */

@Composable
fun MainScreen() {

    val navController =
        rememberNavController()

    val motViewModel:
        MotViewModel =
        viewModel()

    val navBackStackEntry by
        navController
            .currentBackStackEntryAsState()

    val currentRoute =
        navBackStackEntry
            ?.destination
            ?.route

    val startDestination =
        remember {

            val session =
                try {

                    SupabaseManager
                        .client
                        .auth
                        .currentSessionOrNull()

                } catch (
                    e: Exception
                ) {

                    null
                }

            if (
                session != null
            ) {

                Screen.Home.route

            } else {

                Screen.Login.route
            }
        }

    val context =
        LocalContext.current

    val billingManager =
        remember {

            VehicleReportBillingManager(
                context
            )
        }

    val tokenManager =
        remember {

            VehicleReportTokenManager()
        }

    val showBottomBar =
        currentRoute !=
            Screen.Login.route &&

        currentRoute !=
            Screen.EditProfile.route &&

        currentRoute !=
            Screen.Terms.route &&

        currentRoute !=
            Screen.Privacy.route &&

        currentRoute !=
            Screen.VehicleDisclaimer.route &&

        currentRoute !=
            Screen.AffiliateDisclosure.route &&

        currentRoute !=
            Screen.AccountDeletion.route &&

        currentRoute !=
            Screen.InteractiveAccountDeletion.route &&

        currentRoute
            ?.startsWith(
                "mot_detail"
            ) == false &&

        currentRoute
            ?.startsWith(
                "vehicle_report_payment"
            ) == false &&

        currentRoute
            ?.startsWith(
                "vehicle_report"
            ) == false &&

        currentRoute
            ?.startsWith(
                "advert_analyzer"
            ) == false &&

        currentRoute !=
            Screen.FreeVehicleSearch.route &&

        currentRoute !=
            Screen.ProVehicleSearch.route

    val items =
        listOf(
            Screen.Home,
            Screen.Explore,
            Screen.SavedReports,
            Screen.Settings
        )

    Scaffold(

        modifier =
            Modifier.fillMaxSize(),

        bottomBar = {

            if (
                showBottomBar
            ) {

                NavigationBar {

                    items.forEach { screen ->

                        NavigationBarItem(

                            icon = {

                                Icon(
                                    imageVector =
                                        screen.icon,
                                    contentDescription =
                                        screen.title
                                )
                            },

                            label = {

                                Text(
                                    screen.title
                                )
                            },

                            selected =
                                currentRoute ==
                                    screen.route,

                            onClick = {

                                if (
                                    currentRoute !=
                                    screen.route
                                ) {

                                    navController.navigate(
                                        screen.route
                                    ) {

                                        popUpTo(

                                            navController
                                                .graph
                                                .findStartDestination()
                                                .id

                                        ) {

                                            saveState =
                                                true
                                        }

                                        launchSingleTop =
                                            true

                                        restoreState =
                                            true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

    ) { innerPadding ->

        NavHost(

            navController =
                navController,

            startDestination =
                startDestination,

            modifier =
                Modifier.padding(
                    innerPadding
                )

        ) {

            composable(
                Screen.Login.route
            ) {

                LoginScreen(

                    onLoginSuccess = {

                        navController.navigate(
                            Screen.Home.route
                        ) {

                            popUpTo(
                                Screen.Login.route
                            ) {

                                inclusive =
                                    true
                            }
                        }
                    }
                )
            }

            composable(
                Screen.Home.route
            ) {

                HomeScreen()
            }

            composable(
                Screen.Explore.route
            ) {

                VehicleSearchGatewayScreen(

                    onNavigateToFreeSearch = {

                        navController.navigate(
                            Screen.FreeVehicleSearch.route
                        )
                    },

                    onNavigateToProSearch = {

                        CoroutineScope(Dispatchers.IO).launch {

                            val availableResult =
                                tokenManager.getAvailableTokenCount()

                            val available =
                                availableResult.getOrElse {
                                    0
                                }

                            withContext(Dispatchers.Main) {

                                if (
                                    available > 0
                                ) {

                                    navController.navigate(
                                        Screen.ProVehicleSearch.route
                                    )

                                } else {

                                    navController.navigate(
                                        Screen.VehicleReportPayment.route
                                    )
                                }
                            }
                        }
                    },

                    onNavigateBack = {

                        navController.popBackStack()
                    }
                )
            }

            composable(
                Screen.FreeVehicleSearch.route
            ) {

                val freeSearchViewModel:
                    FreeVehicleSearchViewModel =
                    viewModel()

                FreeVehicleSearchScreen(

                    viewModel =
                        freeSearchViewModel,

                    onNavigateToMotDetails = {
                        motTest,
                        vehicle ->

                        motViewModel.selectMotTest(
                            motTest =
                                motTest,
                            vehicle =
                                vehicle
                        )

                        navController.navigate(
                            "mot_detail/${
                                Uri.encode(
                                    motTest.id ?: ""
                                )
                            }"
                        )
                    },

                    onNavigateToSavedReports = {

                        navController.navigate(
                            Screen.SavedReports.route
                        )
                    },

                    onNavigateBack = {

                        navController.popBackStack()
                    }
                )
            }

            composable(
                Screen.ProVehicleSearch.route
            ) {

                ProVehicleSearchScreen(

                    onNavigateToMotDetails = {
                            motTest:
                                com.hiddenhistory.models.MotTest,
                            vehicle:
                                com.hiddenhistory.models.Vehicle ->

                        motViewModel.selectMotTest(
                            motTest =
                                motTest,
                            vehicle =
                                vehicle
                        )

                        navController.navigate(
                            "mot_detail/${
                                Uri.encode(
                                    motTest.id ?: ""
                                )
                            }"
                        )
                    },

                    onNavigateBack = {

                        navController.popBackStack()
                    }
                )
            }

            composable(

                route =
                    Screen.AdvertAnalyzer.route,

                arguments =
                    listOf(

                        navArgument(
                            "advertText"
                        ) {

                            type =
                                NavType.StringType
                        }
                    )

            ) { backStackEntry ->

                val advertText =
                    backStackEntry
                        .arguments
                        ?.getString(
                            "advertText"
                        )
                        .orEmpty()

                AdvertAnalyzerScreen(

                    initialAdvertText =
                        advertText
                )
            }

            composable(
                Screen.SavedReports.route
            ) {

                SavedVehicleReportsScreen(

                    onNavigateBack = {

                        navController.popBackStack()
                    }
                )
            }

            composable(

                route =
                    Screen.MotDetail.route,

                arguments =
                    listOf(

                        navArgument(
                            "motId"
                        ) {

                            type =
                                NavType.StringType
                        }
                    )

            ) {

                MotScreen(

                    viewModel =
                        motViewModel,

                    onBackClick = {

                        navController.popBackStack()
                    }
                )
            }

            composable(
                route =
                    Screen.VehicleReportPayment.route
            ) {

                val activity =
                    LocalActivity.current

                if (
                    activity != null
                ) {

                    VehicleReportPaymentScreen(

                        activity =
                            activity,

                        billingManager =
                            billingManager,

                        onPurchaseConfirmed = {

                            navController.navigate(
                                Screen.ProVehicleSearch.route
                            ) {

                                popUpTo(
                                    Screen
                                        .VehicleReportPayment
                                        .route
                                ) {

                                    inclusive =
                                        true
                                }

                                launchSingleTop =
                                    true
                            }
                        },

                        onNavigateBack = {

                            navController.popBackStack()
                        }
                    )

                } else {

                    Box(

                        modifier =
                            Modifier.fillMaxSize(),

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Text(
                            text =
                                "Unable to open Google Play billing."
                        )
                    }
                }
            }

            composable(

                route =
                    Screen.VehicleReport.route,

                arguments =
                    listOf(

                        navArgument(
                            "advertText"
                        ) {

                            type =
                                NavType.StringType
                        }
                    )

            ) { backStackEntry ->

                val advertText =
                    backStackEntry
                        .arguments
                        ?.getString(
                            "advertText"
                        )
                        .orEmpty()

                val advertAnalysisViewModel:
                    AdvertAnalysisViewModel =
                    viewModel()

                val advertUiState by
                    advertAnalysisViewModel
                        .uiState
                        .collectAsStateWithLifecycle()

                LaunchedEffect(
                    advertText
                ) {

                    if (
                        advertText.isNotBlank()
                    ) {

                        advertAnalysisViewModel
                            .analyzeAdvert(
                                advertText
                            )
                    }
                }

                when (
                    val state =
                        advertUiState
                ) {

                    AdvertAnalysisUiState.Idle -> {

                        VehicleReportScreen(

                            reportText =
                                null,

                            isLoading =
                                false,

                            errorMessage =
                                null,

                            onNavigateBack = {

                                navController
                                    .popBackStack()
                            }
                        )
                    }

                    AdvertAnalysisUiState.Analysing -> {

                        VehicleReportScreen(

                            reportText =
                                null,

                            isLoading =
                                true,

                            errorMessage =
                                null,

                            onNavigateBack = {

                                navController
                                    .popBackStack()
                            }
                        )
                    }

                    is AdvertAnalysisUiState.Error -> {

                        VehicleReportScreen(

                            reportText =
                                null,

                            isLoading =
                                false,

                            errorMessage =
                                state.message,

                            onNavigateBack = {

                                navController
                                    .popBackStack()
                            }
                        )
                    }

                    is AdvertAnalysisUiState.Success -> {

                        VehicleReportScreen(

                            reportText =
                                formatAdvertAnalysisForReport(
                                    state.analysis
                                ),

                            isLoading =
                                false,

                            errorMessage =
                                null,

                            onNavigateBack = {

                                navController
                                    .popBackStack()
                            }
                        )
                    }
                }
            }

            composable(
                Screen.Profile.route
            ) {

                val profileContext =
                    LocalContext.current

                val profileDao =
                    remember {

                        AppDatabase
                            .getDatabase(
                                profileContext
                            )
                            .profileDao()
                    }

                val profileViewModel:
                    ProfileViewModel =
                    viewModel(

                        factory =
                            ProfileViewModel.Factory(
                                profileDao
                            )
                    )

                UserProfileScreen(

                    viewModel =
                        profileViewModel,

                    onNavigateToEditProfile = {

                        navController.navigate(
                            Screen.EditProfile.route
                        )
                    }
                )
            }

            composable(
                Screen.EditProfile.route
            ) {

                val profileContext =
                    LocalContext.current

                val profileDao =
                    remember {

                        AppDatabase
                            .getDatabase(
                                profileContext
                            )
                            .profileDao()
                    }

                val profileViewModel:
                    ProfileViewModel =
                    viewModel(

                        factory =
                            ProfileViewModel.Factory(
                                profileDao
                            )
                    )

                UserProfileEditScreen(

                    viewModel =
                        profileViewModel,

                    onNavigateBack = {

                        navController.popBackStack()
                    }
                )
            }

            composable(
                Screen.Settings.route
            ) {

                val settingsContext =
                    LocalContext.current

                val settingsDao =
                    remember {
                        SettingsDao(
                            settingsContext
                        )
                    }

                val settingsViewModel:
                    SettingsViewModel =
                    viewModel(
                        factory =
                            SettingsViewModel.Factory(
                                settingsDao
                            )
                    )

                SettingsScreen(

                    viewModel =
                        settingsViewModel,

                    onNavigateToProfile = {

                        navController.navigate(
                            Screen.Profile.route
                        ) {

                            launchSingleTop =
                                true
                        }
                    },

                    onNavigateToTerms = {

                        navController.navigate(
                            Screen.Terms.route
                        )
                    },

                    onNavigateToPrivacy = {

                        navController.navigate(
                            Screen.Privacy.route
                        )
                    },

                    onNavigateToVehicleDisclaimer = {

                        navController.navigate(
                            Screen.VehicleDisclaimer.route
                        )
                    },

                    onNavigateToAffiliateDisclosure = {

                        navController.navigate(
                            Screen.AffiliateDisclosure.route
                        )
                    },

                    onNavigateToAccountDeletion = {

                        navController.navigate(
                            Screen.AccountDeletion.route
                        )
                    },

                    onNavigateToInteractiveAccountDeletion = {

                        navController.navigate(
                            Screen.InteractiveAccountDeletion.route
                        )
                    }
                )
            }

            composable(
                Screen.Terms.route
            ) {

                LegalDocumentScreen(
                    title =
                        "Terms & Conditions",
                    assetPath =
                        "legal/TERMS_AND_CONDITIONS.md",
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                Screen.Privacy.route
            ) {

                LegalDocumentScreen(
                    title =
                        "Privacy Policy",
                    assetPath =
                        "legal/PRIVACY_POLICY.md",
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                Screen.VehicleDisclaimer.route
            ) {

                LegalDocumentScreen(
                    title =
                        "Vehicle Data & Analysis",
                    assetPath =
                        "legal/VEHICLE_DATA_AND_ANALYSIS_DISCLAIMER.md",
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                Screen.AffiliateDisclosure.route
            ) {

                LegalDocumentScreen(
                    title =
                        "Affiliate Disclosure",
                    assetPath =
                        "legal/AFFILIATE_DISCLOSURE.md",
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                Screen.AccountDeletion.route
            ) {

                LegalDocumentScreen(
                    title =
                        "Account Deletion",
                    assetPath =
                        "legal/ACCOUNT_DELETION_AND_DATA_RETENTION.md",
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                Screen.InteractiveAccountDeletion.route
            ) {

                DeleteAccountScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onAccountDeleted = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                Screen.DebugInspector.route
            ) {

                DebugInspectorScreen(

                    onNavigateBack = {

                        navController.popBackStack()
                    }
                )
            }
        }
    }
}


private fun formatAdvertAnalysisForReport(
    analysis:
        com.hiddenhistory.models.AdvertAnalysis
): String {

    return buildString {

        analysis.advertTitle?.let {

            appendLine(it)
            appendLine()
        }

        analysis.registrationNumber?.let {

            appendLine("REGISTRATION")
            appendLine(it)
            appendLine()
        }

        analysis.price?.let {

            appendLine("ADVERT PRICE")
            appendLine(it)
            appendLine()
        }

        analysis.mileage?.let {

            appendLine("MILEAGE")
            appendLine(it)
            appendLine()
        }

        if (
            analysis.vehicleDetails
                ?.isNotEmpty() == true
        ) {

            appendLine(
                "VEHICLE DETAILS"
            )

            analysis.vehicleDetails
                ?.forEach { (key, value) ->

                    appendLine(
                        "• $key: $value"
                    )
                }

            appendLine()
        }

        analysis.sellerInformation?.let {

            appendLine(
                "WHAT THE SELLER IS SAYING"
            )

            appendLine(it)
            appendLine()
        }

        if (
            analysis.claimsMadeBySeller
                .isNotEmpty()
        ) {

            appendLine(
                "CLAIMS MADE BY SELLER"
            )

            analysis.claimsMadeBySeller
                .forEach {

                    appendLine(
                        "• $it"
                    )
                }

            appendLine()
        }

        if (
            analysis.notableWording
                .isNotEmpty()
        ) {

            appendLine(
                "NOTABLE WORDING"
            )

            analysis.notableWording
                .forEach {

                    appendLine(
                        "• $it"
                    )
                }

            appendLine()
        }

        if (
            analysis.thingsWorthVerifying
                .isNotEmpty()
        ) {

            appendLine(
                "THINGS TO CHECK"
            )

            analysis.thingsWorthVerifying
                .forEach {

                    appendLine(
                        "• $it"
                    )
                }

            appendLine()
        }

        if (
            analysis.missingInformation
                .isNotEmpty()
        ) {

            appendLine(
                "MISSING INFORMATION"
            )

            analysis.missingInformation
                .forEach {

                    appendLine(
                        "• $it"
                    )
                }

            appendLine()
        }

        if (
            analysis.inconsistencies
                .isNotEmpty()
        ) {

            appendLine(
                "INCONSISTENCIES FOUND"
            )

            analysis.inconsistencies
                .forEach {

                    appendLine(
                        "• $it"
                    )
                }

            appendLine()
        }

        if (
            analysis.questionsTheBuyerShouldAsk
                .isNotEmpty()
        ) {

            appendLine(
                "QUESTIONS TO ASK THE SELLER"
            )

            analysis.questionsTheBuyerShouldAsk
                .forEach {

                    appendLine(
                        "• $it"
                    )
                }

            appendLine()
        }

        analysis.overallSummary?.let {

            appendLine(
                "OVERALL SUMMARY"
            )

            appendLine(it)
            appendLine()
        }

        if (
            isBlank()
        ) {

            appendLine(
                "The vehicle analysis completed successfully, " +
                    "but no additional advert-analysis text was returned."
            )
        }

    }.trim()
}


@Composable
fun HomeScreen() {

    Box(

        modifier =
            Modifier.fillMaxSize(),

        contentAlignment =
            Alignment.Center

    ) {

        Text(
            text =
                "Welcome to your Home Screen!"
        )
    }
}
