package com.misw4203.vinilos.presentation.ui.screens.collector

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
import com.misw4203.vinilos.presentation.ui.components.CollectorCard
import com.misw4203.vinilos.presentation.ui.components.EmptyState
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.ui.components.VinilosTopBar
import com.misw4203.vinilos.presentation.viewmodel.CollectorListUiState
import com.misw4203.vinilos.presentation.viewmodel.CollectorListViewModel

@Composable
fun CollectorListScreen(
    onCollectorClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectorListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        VinilosTopBar(title = stringResource(R.string.collectors_title))
        when (val state = uiState) {
            is CollectorListUiState.Loading -> LoadingState()
            is CollectorListUiState.Error -> ErrorState(
                onRetry = viewModel::retry,
                isNetworkError = state.isNetworkError,
            )
            is CollectorListUiState.Empty -> EmptyState()
            is CollectorListUiState.Success -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                items(state.collectors, key = { it.id }) { collector ->
                    CollectorCard(
                        collector = collector,
                        onClick = { onCollectorClick(collector.id) },
                    )
                }
                item { Spacer(Modifier.size(24.dp)) }
            }
        }
    }
}
