package com.misw4203.vinilos.presentation.ui.screens.artist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misw4203.vinilos.R
import com.misw4203.vinilos.presentation.ui.components.EmptyState
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.ui.components.MusicianCard
import com.misw4203.vinilos.presentation.ui.components.VinilosTopBar
import com.misw4203.vinilos.presentation.viewmodel.MusicianListUiState
import com.misw4203.vinilos.presentation.viewmodel.MusicianListViewModel

@Composable
fun MusicianListScreen(
    onMusicianClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MusicianListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        VinilosTopBar(title = stringResource(R.string.artists_title))
        when (val state = uiState) {
            is MusicianListUiState.Loading -> LoadingState()
            is MusicianListUiState.Error -> ErrorState(
                onRetry = viewModel::retry,
                isNetworkError = state.isNetworkError,
            )
            is MusicianListUiState.Empty -> EmptyState()
            is MusicianListUiState.Success -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                items(state.musicians, key = { it.id }) { musician ->
                    MusicianCard(
                        musician = musician,
                        onClick = { onMusicianClick(musician.id) },
                    )
                }
                item { Spacer(Modifier.size(24.dp)) }
            }
        }
    }
}
