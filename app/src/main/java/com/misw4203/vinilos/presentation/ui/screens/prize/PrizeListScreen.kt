package com.misw4203.vinilos.presentation.ui.screens.prize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.presentation.ui.components.EmptyState
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.ListCounter
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.ui.components.VinilosTopBar
import com.misw4203.vinilos.presentation.viewmodel.PrizesUiState
import com.misw4203.vinilos.presentation.viewmodel.PrizesViewModel

@Composable
fun PrizeListScreen(
    onCreatePrize: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrizesViewModel = hiltViewModel(),
    refreshKey: Boolean = false,
    onRefreshHandled: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(refreshKey) {
        if (refreshKey) {
            viewModel.refresh()
            onRefreshHandled()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            VinilosTopBar(title = stringResource(R.string.prizes_title))
            when (val state = uiState) {
                is PrizesUiState.Loading -> LoadingState(modifier = Modifier.testTag("prizes_loading"))
                is PrizesUiState.Error -> ErrorState(
                    onRetry = viewModel::retry,
                    isNetworkError = state.isNetworkError,
                )
                is PrizesUiState.Empty -> EmptyState(modifier = Modifier.testTag("prizes_empty"))
                is PrizesUiState.Success -> LazyColumn(
                    modifier = Modifier.testTag("prizes_list"),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        ListCounter(
                            text = pluralStringResource(
                                R.plurals.prizes_record_count,
                                state.prizes.size,
                                state.prizes.size,
                            ),
                            testTag = "prizes_record_count",
                        )
                    }
                    items(state.prizes, key = { it.id }) { prize ->
                        PrizeCard(
                            prize = prize,
                            modifier = Modifier.testTag("prize_card_${prize.id}"),
                        )
                    }
                    item { Spacer(Modifier.size(80.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = onCreatePrize,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("create_prize_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.cd_create_prize),
            )
        }
    }
}

@Composable
internal fun PrizeCard(
    prize: Prize,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            androidx.compose.material3.Text(
                text = prize.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            androidx.compose.material3.Text(
                text = prize.organization,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
