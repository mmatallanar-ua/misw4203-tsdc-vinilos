package com.misw4203.vinilos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.misw4203.vinilos.presentation.navigation.VinilosNavHost
import com.misw4203.vinilos.presentation.ui.theme.VinilosTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            VinilosTheme {
                VinilosNavHost()
            }
        }
    }
}
