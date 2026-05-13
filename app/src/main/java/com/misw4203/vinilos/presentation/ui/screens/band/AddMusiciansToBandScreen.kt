package com.misw4203.vinilos.presentation.ui.screens.band

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.ui.components.MusicianCard
import com.misw4203.vinilos.presentation.ui.components.MusicianRow
import com.misw4203.vinilos.presentation.viewmodel.AddMusiciansEvent
import com.misw4203.vinilos.presentation.viewmodel.AddMusiciansToBandViewModel
import com.misw4203.vinilos.presentation.viewmodel.AddMusiciansUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMusiciansToBandScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddMusiciansToBandViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val successTemplate = stringResource(R.string.add_musician_success)
    val networkErrorMessage = stringResource(R.string.add_musician_error_network)
    val serverErrorMessage = stringResource(R.string.add_musician_error_server)

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is AddMusiciansEvent.AddedSuccessfully ->
                    snackbarHostState.showSnackbar(successTemplate.format(event.musicianName))
            }
        }
    }
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is AddMusiciansUiState.Error && state.musicianId != null) {
            val msg = if (state.isNetworkError) networkErrorMessage else serverErrorMessage
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier.testTag("add_musicians_screen_root"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_musicians_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("add_musicians_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                AddMusiciansUiState.Loading -> LoadingState()
                else -> Content(
                    queryValue = form.query,
                    onQueryChange = viewModel::onQueryChange,
                    available = form.filteredAvailable,
                    currentMembers = form.allMusicians.filter { it.id in form.currentMemberIds },
                    addingId = (uiState as? AddMusiciansUiState.Adding)?.musicianId,
                    onAdd = viewModel::onAddMusician,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    queryValue: String,
    onQueryChange: (String) -> Unit,
    available: List<MusicianSummary>,
    currentMembers: List<MusicianSummary>,
    addingId: Int?,
    onAdd: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            OutlinedTextField(
                value = queryValue,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_musicians_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_musicians_search"),
            )
        }
        item {
            SectionHeader(stringResource(R.string.add_musicians_available_section))
        }
        if (available.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.add_musicians_empty_filter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(available, key = { it.id }) { musician ->
                MusicianRow(
                    musician = musician,
                    isAdding = addingId == musician.id,
                    onAdd = onAdd,
                    modifier = Modifier.testTag("available_musician_${musician.id}"),
                )
            }
        }
        item {
            SectionHeader(stringResource(R.string.add_musicians_current_section))
        }
        item {
            Text(
                text = pluralStringResource(
                    R.plurals.members_record_count,
                    currentMembers.size,
                    currentMembers.size,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        items(currentMembers, key = { it.id }) { musician ->
            MusicianCard(
                musician = musician,
                onClick = {},
                modifier = Modifier.testTag("current_member_${musician.id}"),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier.semantics { heading() },
    )
}
