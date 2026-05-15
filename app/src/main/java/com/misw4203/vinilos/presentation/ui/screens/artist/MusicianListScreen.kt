package com.misw4203.vinilos.presentation.ui.screens.artist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.misw4203.vinilos.presentation.viewmodel.MusicianListViewModel

@Composable
fun MusicianListScreen(
    onMusicianClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MusicianListViewModel = hiltViewModel(),
) {
    MusicianListContent(
        onMusicianClick = onMusicianClick,
        modifier = modifier,
        viewModel = viewModel,
    )
}
