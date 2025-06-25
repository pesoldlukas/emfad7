package com.emfad.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emfad.app.ui.screens.*

// Navigation routes matching original EMFAD Windows programs
sealed class EMFADScreen(val route: String, val title: String) {
    object Start : EMFADScreen("start", "EMFAD Start")
    object Survey : EMFADScreen("survey", "Survey")
    object Setup : EMFADScreen("setup", "Setup")
    object Plot : EMFADScreen("plot", "Plot")
    object Spec : EMFADScreen("spec", "Spec")
    object Profile : EMFADScreen("profile", "Profile")
    object Map : EMFADScreen("map", "Map")
    object Settings : EMFADScreen("settings", "Settings")
    object AR : EMFADScreen("ar", "AR View")
    object Visualization : EMFADScreen("visualization", "Visualization")
}

@Composable
fun EMFADNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = EMFADScreen.Start.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Start Screen - Device Connection (USB/Bluetooth)
        composable(EMFADScreen.Start.route) {
            StartScreen(
                onNavigateToSurvey = { navController.navigate(EMFADScreen.Survey.route) },
                onNavigateToSetup = { navController.navigate(EMFADScreen.Setup.route) },
                onNavigateToSettings = { navController.navigate(EMFADScreen.Settings.route) }
            )
        }
        
        // Survey Screen - Signal bars, step/auto buttons
        composable(EMFADScreen.Survey.route) {
            SurveyScreen(
                onNavigateToPlot = { navController.navigate(EMFADScreen.Plot.route) },
                onNavigateToSpec = { navController.navigate(EMFADScreen.Spec.route) },
                onNavigateToProfile = { navController.navigate(EMFADScreen.Profile.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Setup Screen - Frequency selection, Gain/Offset, Mode selection
        composable(EMFADScreen.Setup.route) {
            SetupScreen(
                onNavigateToSurvey = { navController.navigate(EMFADScreen.Survey.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Plot Screen - Realtime LineChart with export
        composable(EMFADScreen.Plot.route) {
            PlotScreen(
                onNavigateToSpec = { navController.navigate(EMFADScreen.Spec.route) },
                onNavigateToProfile = { navController.navigate(EMFADScreen.Profile.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Spec Screen - Frequency spectrum with interactive buttons
        composable(EMFADScreen.Spec.route) {
            SpecScreen(
                onNavigateToPlot = { navController.navigate(EMFADScreen.Plot.route) },
                onNavigateToProfile = { navController.navigate(EMFADScreen.Profile.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Profile Screen - 2D/3D evaluation after scan
        composable(EMFADScreen.Profile.route) {
            ProfileScreen(
                onNavigateToMap = { navController.navigate(EMFADScreen.Map.route) },
                onNavigateToAR = { navController.navigate(EMFADScreen.AR.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Map Screen - GPS + OSM integration
        composable(EMFADScreen.Map.route) {
            MapScreen(
                onNavigateToProfile = { navController.navigate(EMFADScreen.Profile.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Settings Screen
        composable(EMFADScreen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // AR Screen
        composable(EMFADScreen.AR.route) {
            ARScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Visualization Screen
        composable(EMFADScreen.Visualization.route) {
            VisualizationPage(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
