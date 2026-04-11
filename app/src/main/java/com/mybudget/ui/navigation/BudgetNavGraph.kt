package com.mybudget.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mybudget.data.local.prefs.SettingsManager
import com.mybudget.security.BiometricUtils
import com.mybudget.ui.screens.BiometricScreen
import com.mybudget.ui.screens.DashboardScreen
import com.mybudget.ui.screens.OnboardingScreen

sealed class Route(val route: String) {
    object Splash : Route("splash")
    object Onboarding : Route("onboarding")
    object Biometric : Route("biometric")
    object Dashboard : Route("dashboard")
    object Scanner : Route("scanner")
    object Settings : Route("settings")
    object ManualEntry : Route("manual_entry")
    object TransactionList : Route("transaction_list")
}

// Smooth slide + fade animations
private const val ANIM_DURATION = 350

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromRight(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
    ) + fadeIn(tween(ANIM_DURATION / 2))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToLeft(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
    ) + fadeOut(tween(ANIM_DURATION / 2))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromLeft(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
    ) + fadeIn(tween(ANIM_DURATION / 2))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToRight(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
    ) + fadeOut(tween(ANIM_DURATION / 2))

@Composable
fun BudgetNavGraph(activity: FragmentActivity) {
    val navController = rememberNavController()
    val settingsManager = remember { SettingsManager(activity) }

    val onboardingCompleted by settingsManager.onboardingCompletedFlow.collectAsState(initial = null)
    val biometricEnabled by settingsManager.biometricEnabledFlow.collectAsState(initial = null)

    NavHost(navController = navController, startDestination = Route.Splash.route) {
        composable(Route.Splash.route) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            LaunchedEffect(onboardingCompleted, biometricEnabled) {
                if (onboardingCompleted != null && biometricEnabled != null) {
                    val biometricAvailable = BiometricUtils.isAuthenticationAvailable(activity)
                    if (onboardingCompleted == false) {
                        navController.navigate(Route.Onboarding.route) { popUpTo(0) }
                    } else if (biometricEnabled == true && biometricAvailable) {
                        navController.navigate(Route.Biometric.route) { popUpTo(0) }
                    } else {
                        if (biometricEnabled == true && !biometricAvailable) {
                            settingsManager.setBiometricEnabled(false)
                        }
                        navController.navigate(Route.Dashboard.route) { popUpTo(0) }
                    }
                }
            }
        }

        composable(
            Route.Onboarding.route,
            enterTransition = { fadeIn(tween(500)) },
            exitTransition = { fadeOut(tween(300)) }
        ) {
            OnboardingScreen(
                settingsManager = settingsManager,
                onComplete = {
                    if (biometricEnabled == true && BiometricUtils.isAuthenticationAvailable(activity)) {
                        navController.navigate(Route.Biometric.route) { popUpTo(0) }
                    } else {
                        navController.navigate(Route.Dashboard.route) { popUpTo(0) }
                    }
                }
            )
        }

        composable(
            Route.Biometric.route,
            enterTransition = { fadeIn(tween(400)) },
            exitTransition = { fadeOut(tween(200)) }
        ) {
            BiometricScreen(
                activity = activity,
                onUnlocked = {
                    navController.navigate(Route.Dashboard.route) { popUpTo(0) }
                }
            )
        }

        composable(
            Route.Dashboard.route,
            enterTransition = { fadeIn(tween(400)) },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { fadeOut(tween(200)) }
        ) {
            DashboardScreen(
                settingsManager = settingsManager,
                onNavigateToScanner = { navController.navigate(Route.Scanner.route) },
                onNavigateToSettings = { navController.navigate(Route.Settings.route) },
                onNavigateToManualEntry = { navController.navigate(Route.ManualEntry.route) },
                onNavigateToAllTransactions = { navController.navigate(Route.TransactionList.route) }
            )
        }

        composable(
            Route.Scanner.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            com.mybudget.ui.screens.ScannerScreen(
                settingsManager = settingsManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Route.Settings.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            com.mybudget.ui.screens.SettingsScreen(
                settingsManager = settingsManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Route.ManualEntry.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            com.mybudget.ui.screens.ManualEntryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Route.TransactionList.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            com.mybudget.ui.screens.TransactionListScreen(
                settingsManager = settingsManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
