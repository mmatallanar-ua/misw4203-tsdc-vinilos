package com.misw4203.vinilos.presentation.ui.screens.artist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MusicianListScreen(onMusicianClick: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onMusicianClick(2) }
    ) {
        Text(
            text = "Rubén Blades Bellido de Luna",
            modifier = Modifier.padding(16.dp)
        )
    }
}
