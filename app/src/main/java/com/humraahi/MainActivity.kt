package com.humraahi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.compose.rememberNavController
import com.humraahi.ui.theme.AquaSurfaceVariant
import com.humraahi.ui.theme.LagoonBackground
import com.humraahi.ui.theme.NightOceanBackground
import com.humraahi.ui.theme.NightOceanSurfaceVariant
import com.humraahi.ui.theme.HumraahiTheme
import com.humraahi.navigation.AppNavGraph



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = LagoonBackground.toArgb(),
                darkScrim = NightOceanBackground.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = AquaSurfaceVariant.toArgb(),
                darkScrim = NightOceanSurfaceVariant.toArgb()
            )
        )
        setContent {
            HumraahiTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}