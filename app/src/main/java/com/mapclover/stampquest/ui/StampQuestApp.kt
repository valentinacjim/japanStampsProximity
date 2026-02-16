package com.mapclover.stampquest.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController


@Composable
fun StampQuestApp(modifier: Modifier) {
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
