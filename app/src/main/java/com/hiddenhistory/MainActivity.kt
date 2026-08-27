package com.hiddenhistory

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.database.AppDatabase
import com.hiddenhistory.screens.AdvertAnalyzerScreen
import com.hiddenhistory.screens.FreeVehicleSearchScreen
import com.hiddenhistory.screens.LoginScreen
import com.hiddenhistory.screens.MotScreen
import com.hiddenhistory.screens.ProVehicleSearchScreen
import com.hiddenhistory.screens.SavedVehicleReportsScreen
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
import io.github.jan.supabase.auth.auth


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
    val icon: ImageVector
) {

    object Home : Screen(
        "home",
        "Home",
        Icons.Default.Home
    )

    /*
     * =========================================================
     * VEHICLE SEARCH GATEWAY
     * =========================================================
     */

    object Explore : Screen(
        "explore",
        "Vehicle Search",
        Icons.Default.Search
    )

    /*
     * =========================================================
     * FREE VEHICLE SEARCH
     * =========================================================
     */

    object FreeVehicleSearch : Screen(
        "free_vehicle_search",
        "Free Vehicle Search",
        Icons.Default.Search
    )

    /*
     * =========================================================
     * PRO VEHICLE SEARCH
     * =========================================================
     */

    object ProVehicleSearch : Screen(
        "pro_vehicle_search",
        "Pro Vehicle Search",
        Icons.Default.Search
    )

    /*
     * =========================================================
     * SAVED REPORTS
     * =========================================================
     */

    object SavedReports : Screen(
        "saved_reports",
        "Saved Reports",
        Icons.Default.Favorite
    )

    /*
     * =========================================================
     * PROFILE
     * =========================================================
     */

    object Profile : Screen(
        "profile",
        "Profile",
        Icons.Default.Person
    )

    object EditProfile : Screen(
        "edit_profile",
        "Edit Profile",
        Icons.Default.Edit
    )

    /*
     * =========================================================
     * LOGIN
     * =========================================================
     */

    object Login : Screen(
        "login",
        "Login",
        Icons.Default.Home
    )

    /*
     * =========================================================
     * MOT DETAIL
     * =========================================================
     *
     * motId remains in the route for navigation compatibility.
     *
     * The actual selected MotTest is held by the shared MotViewModel.
     *
     * MotScreen therefore does NOT need to retrieve the MotTest
     * from the route.
     */

    object MotDetail : Screen(
        "mot_detail/{motId}",
        "MOT Detail",
        Icons.Default.Search
    )

    /*
     * =========================================================
     * PAID VEHICLE REPORT PAYMENT
     * =========================================================
     */

    object VehicleReportPayment : Screen(
        "vehicle_report_payment/{advertText}",
        "AI Vehicle Report",
        Icons.Default.Search
    )

    /*
     * =========================================================
     * FULL VEHICLE REPORT
     * =========================================================
     */

    object VehicleReport : Screen(
        "vehicle_report/{advertText}",
        "Full AI Vehicle Report",
        Icons.Default.Search
    )

    /*
     * =========================================================
     * FREE ADVERT ANALYSER
     * =========================================================
     */

    object AdvertAnalyzer : Screen(
        "advert_analyzer/{advertText}",
        "Advert Analyzer",
        Icons.Default.Search
    )

    /*
     * =========================================================
     * DEBUG INSPECTOR
     * =========================================================
     */

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

    /*
     * =========================================================
     * SHARED MOT VIEWMODEL
     * =========================================================
     *
     * This ViewModel is owned by MainScreen.
     *
     * It therefore survives navigation between:
     *
     * FreeVehicleSearchScreen
     *          ↓
     * MotScreen
     *
     * The selected MotTest is placed into this ViewModel BEFORE
     * navigation takes place.
     */

    val motViewModel:
        MotViewModel =
        viewModel()

    /*
     * =========================================================
     * CURRENT ROUTE
     * =========================================================
     */

    val navBackStackEntry by
        navController
            .currentBackStackEntryAsState()

    val currentRoute =
        navBackStackEntry
            ?.destination
            ?.route

    /*
     * =========================================================
     * LOGIN / START DESTINATION
     * =========================================================
     */

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

    /*
     * =========================================================
     * BILLING MANAGER
     * =========================================================
     */

    val context =
        LocalContext.current

    val billingManager =
        remember {

            VehicleReportBillingManager(
                context
            )
        }

    /*
     * =========================================================
     * BOTTOM NAVIGATION VISIBILITY
     * =========================================================
     */

    val showBottomBar =
        currentRoute !=
            Screen.Login.route &&

        currentRoute !=
            Screen.EditProfile.route &&

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
            Screen.Profile,
            Screen.DebugInspector
        )

    /*
     * =========================================================
     * MAIN SCAFFOLD
     * =========================================================
     */

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

            /*
             * =========================================================
             * LOGIN
             * =========================================================
             */

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

            /*
             * =========================================================
             * HOME
             * =========================================================
             */

            composable(
                Screen.Home.route
            ) {

                HomeScreen()
            }

            /*
             * =========================================================
             * VEHICLE SEARCH GATEWAY
             * =========================================================
             */

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

                        navController.navigate(
                            Screen.ProVehicleSearch.route
                        )
                    },

                    onNavigateBack = {

                        navController.popBackStack()
                    }
                )
            }

            /*
             * =========================================================
             * FREE VEHICLE SEARCH
             * =========================================================
             */

            composable(
                Screen.FreeVehicleSearch.route
            ) {

                val freeSearchViewModel:
                    FreeVehicleSearchViewModel =
                    viewModel()

                FreeVehicleSearchScreen(

                    viewModel =
                        freeSearchViewModel,

                    onNavigateToMotDetails = { motTest, vehicle ->

                        motViewModel.selectMotTest(
                            motTest = motTest,
                            vehicle = vehicle
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

            /*
             * =========================================================
             * PRO VEHICLE SEARCH
             * =========================================================
             */

            composable(
                Screen.ProVehicleSearch.route
            ) {

                ProVehicleSearchScreen(

                    onNavigateBack = {

                        navController.popBackStack()
                    }
                )
            }

            /*
             * =========================================================
             * FREE ADVERT ANALYSER
             * =========================================================
             */

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

            /*
             * =========================================================
             * SAVED REPORTS
             * =========================================================
             */

            composable(
                Screen.SavedReports.route
            ) {

                SavedVehicleReportsScreen(

                    onNavigateBack = {

                        navController.popBackStack()
                    }
                )
            }

            /*
             * =========================================================
             * MOT DETAIL
             * =========================================================
             */

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

            /*
             * =========================================================
             * PAID AI VEHICLE REPORT PAYMENT
             * =========================================================
             */

            composable(

                route =
                    Screen.VehicleReportPayment.route,

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

                val activity =
                    LocalContext.current
                        as? ComponentActivity

                if (
                    activity != null
                ) {

                    VehicleReportPaymentScreen(

                        activity =
                            activity,

                        billingManager =
                            billingManager,

                        onPurchaseConfirmed = {
                            purchaseToken ->

                            navController.navigate(

                                "vehicle_report/${
                                    Uri.encode(
                                        advertText
                                    )
                                }"

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

            /*
             * =========================================================
             * FULL AI VEHICLE REPORT
             * =========================================================
             */

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

            /*
             * =========================================================
             * PROFILE
             * =========================================================
             */

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

            /*
             * =========================================================
             * EDIT PROFILE
             * =========================================================
             */

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

            /*
             * =========================================================
             * DEBUG INSPECTOR
             * =========================================================
             */

            composable(
                route =
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


/*
 * =====================================================================
 * PAID AI REPORT FORMATTER
 * =====================================================================
 */

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

                "The AI analysis completed successfully, " +
                    "but no report text was returned."
            )
        }

    }.trim()
}


/*
 * =====================================================================
 * HOME SCREEN
 * =====================================================================
 */

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