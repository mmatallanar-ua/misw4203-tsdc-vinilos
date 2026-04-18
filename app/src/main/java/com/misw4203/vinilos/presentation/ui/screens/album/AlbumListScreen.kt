package com.misw4203.vinilos.presentation.ui.screens.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misw4203.vinilos.R
import com.misw4203.vinilos.presentation.ui.components.AlbumCard
import com.misw4203.vinilos.presentation.ui.components.EmptyState
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.ui.components.SearchBarStatic
import com.misw4203.vinilos.presentation.ui.components.VinilosBottomNav
import com.misw4203.vinilos.presentation.ui.components.VinilosDestination
import com.misw4203.vinilos.presentation.ui.components.VinilosTopBar
import com.misw4203.vinilos.presentation.viewmodel.AlbumListUiState
import com.misw4203.vinilos.presentation.viewmodel.AlbumListViewModel

@Composable
fun AlbumListScreen(
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { VinilosTopBar(title = stringResource(R.string.albums_title)) },
        bottomBar = {
            VinilosBottomNav(
                selected = VinilosDestination.Albums,
                onSelect = { /* Artists & Collectors out of scope for HU-001 */ },
            )
        },
    ) { innerPadding ->
        AlbumListContent(
            state = uiState,
            onRetry = viewModel::retry,
            onAlbumClick = onAlbumClick,
            contentPadding = innerPadding,
        )
    }
}

@Composable
private fun AlbumListContent(
    state: AlbumListUiState,
    onRetry: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        when (state) {
            is AlbumListUiState.Loading -> LoadingState()
            is AlbumListUiState.Error -> ErrorState(
                onRetry = onRetry,
                isNetworkError = state.isNetworkError,
            )
            is AlbumListUiState.Empty -> Column {
                HeaderSection()
                EmptyState()
            }
            is AlbumListUiState.Success -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item { HeaderSection() }
                items(state.albums, key = { it.id }) { album ->
                    AlbumCard(album = album, onClick = { onAlbumClick(album.id) })
                }
                item { Spacer(Modifier.size(24.dp)) }
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(
        modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp),
    ) {
        SearchBarStatic()
        Spacer(Modifier.size(8.dp))
    }
}
