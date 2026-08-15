package com.humraahi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.humraahi.ui.theme.HumraahiTheme
import com.humraahi.navigation.AppNavGraph



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HumraahiTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}