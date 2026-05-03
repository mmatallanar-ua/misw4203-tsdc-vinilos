package com.misw4203.vinilos.presentation.ui.screens.collector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misw4203.vinilos.R
import androidx.compose.ui.res.pluralStringResource
import com.misw4203.vinilos.presentation.ui.components.CollectorCard
import com.misw4203.vinilos.presentation.ui.components.EmptyState
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.ListCounter
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.ui.components.SearchBarStatic
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
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        VinilosTopBar(title = stringResource(R.string.collectors_title))
        when (val state = uiState) {
            is CollectorListUiState.Loading -> LoadingState()
            is CollectorListUiState.Error -> ErrorState(
                onRetry = viewModel::retry,
                isNetworkError = state.isNetworkError,
            )
            is CollectorListUiState.Empty -> Column {
                HeaderSection()
                EmptyState()
            }
            is CollectorListUiState.Success -> LazyColumn(
                modifier = Modifier.testTag("collectors_list"),
                state = listState,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item { HeaderSection() }
                item {
                    ListCounter(
                        text = pluralStringResource(
                            R.plurals.collectors_record_count,
                            state.collectors.size,
                            state.collectors.size,
                        ),
                        testTag = "collectors_record_count",
                    )
                }
                items(state.collectors, key = { it.id }) { collector ->
                    CollectorCard(
                        collector = collector,
                        onClick = { onCollectorClick(collector.id) },
                        modifier = Modifier.testTag("collector_card_${collector.id}"),
                    )
                }
                item { Spacer(Modifier.size(24.dp)) }
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Column {
        SearchBarStatic(placeholder = stringResource(R.string.search_placeholder_collectors))
        Spacer(Modifier.size(8.dp))
    }
}
