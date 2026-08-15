package com.humraahi.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.humraahi.data.AuthState
import com.humraahi.ui.auth.AuthViewModel
import com.humraahi.ui.auth.LoginScreen
import com.humraahi.ui.home.HomeScreen
import com.humraahi.ui.home.HomeViewModel
import com.humraahi.ui.newtrip.NewTripScreen
import com.humraahi.ui.profile.ProfileScreen
import com.humraahi.ui.tripdetail.TripDetailScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    
    val startDestination = when (authState) {
        is AuthState.Authenticated -> Routes.HOME
        else -> Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(navController = navController, viewModel = authViewModel)
        }
        composable(Routes.HOME) {
            HomeScreen(navController = navController, viewModel = homeViewModel)
        }
        composable(Routes.NEW_TRIP) {
            NewTripScreen(navController = navController, viewModel = homeViewModel)
        }
        composable(
            route = Routes.TRIP_DETAIL,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "humraahi://trip/{tripId}" })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            TripDetailScreen(tripId = tripId, navController)
        }
        composable(Routes.PROFILE) {
            ProfileScreen(navController = navController)
        }
    }
}