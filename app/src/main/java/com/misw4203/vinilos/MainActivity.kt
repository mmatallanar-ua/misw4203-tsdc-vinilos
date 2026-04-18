package com.misw4203.vinilos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.misw4203.vinilos.presentation.ui.components.BottomNavigationBar
import com.misw4203.vinilos.presentation.ui.screens.artist.MusicianDetailScreen
import com.misw4203.vinilos.presentation.ui.screens.artist.MusicianListScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                VinilosApp()
            }
        }
    }
}

@Composable
fun VinilosApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = "artists"
            ) {
                composable("albums") {
                    Box(modifier = Modifier.fillMaxSize())
                }
                composable("artists") {
                    MusicianListScreen(
                        onMusicianClick = { id -> navController.navigate("artist/$id") }
                    )
                }
                composable(
                    route = "artist/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("id") ?: return@composable
                    MusicianDetailScreen(
                        musicianId = id,
                        onBack = { navController.navigateUp() }
                    )
                }
                composable("collectors") {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
