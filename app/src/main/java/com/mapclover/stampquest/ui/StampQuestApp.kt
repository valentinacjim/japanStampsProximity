package com.mapclover.stampquest.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mapclover.stampquest.ui.collection.CollectionScreen
import com.mapclover.stampquest.ui.map.MapScreen


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

        composable(
            route = "map?stampId={stampId}",
            arguments = listOf(navArgument("stampId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { entry ->
            MapScreen(
                onCollectionClick = { navController.navigate("collection") },
                focusStampId = entry.arguments?.getString("stampId")
            )
        }

        composable("collection") {
            CollectionScreen(
                onBackClick = { navController.popBackStack() },
                onStampClick = { stamp ->
                    navController.navigate("map?stampId=${android.net.Uri.encode(stamp.id)}") {
                        popUpTo("map") { inclusive = false }
                    }
                }
            )
        }
    }
}
