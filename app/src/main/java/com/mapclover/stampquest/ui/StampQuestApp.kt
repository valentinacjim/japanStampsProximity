package com.mapclover.stampquest.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


@Composable
fun StampQuestApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "start"
    ) {
        composable("start") {
            StartScreen(
                onStartClick = {
                    navController.navigate("map")
                }
            )
        }

        composable("map") {
            MapScreen()
        }
    }
}
