package com.misw4203.vinilos.presentation.ui.screens.band

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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misw4203.vinilos.R
import com.misw4203.vinilos.presentation.ui.components.BandCard
import com.misw4203.vinilos.presentation.ui.components.EmptyState
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.ListCounter
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.ui.components.SearchBarStatic
import com.misw4203.vinilos.presentation.viewmodel.BandListUiState
import com.misw4203.vinilos.presentation.viewmodel.BandListViewModel

@Composable
fun BandListContent(
    onBandClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BandListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is BandListUiState.Loading -> LoadingState()
            is BandListUiState.Error -> ErrorState(
                onRetry = viewModel::retry,
                isNetworkError = state.isNetworkError,
            )
            is BandListUiState.Empty -> Column {
                BandHeaderSection()
                EmptyState()
            }
            is BandListUiState.Success -> LazyColumn(
                modifier = Modifier.testTag("bands_list"),
                state = listState,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item { BandHeaderSection() }
                item {
                    ListCounter(
                        text = pluralStringResource(
                            R.plurals.bands_record_count,
                            state.bands.size,
                            state.bands.size,
                        ),
                        testTag = "bands_record_count",
                    )
                }
                items(state.bands, key = { it.id }) { band ->
                    BandCard(
                        band = band,
                        onClick = { onBandClick(band.id) },
                        modifier = Modifier.testTag("band_card_${band.id}"),
                    )
                }
                item { Spacer(Modifier.size(24.dp)) }
            }
        }
    }
}

@Composable
private fun BandHeaderSection() {
    Column {
        SearchBarStatic(placeholder = stringResource(R.string.search_placeholder_bands))
        Spacer(Modifier.size(8.dp))
    }
}
