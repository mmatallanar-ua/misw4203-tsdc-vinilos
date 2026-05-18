package com.misw4203.vinilos.presentation.ui.screens.album

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.ui.components.MusicianRow
import com.misw4203.vinilos.presentation.viewmodel.AddPerformerToAlbumEvent
import com.misw4203.vinilos.presentation.viewmodel.AddPerformerToAlbumUiState
import com.misw4203.vinilos.presentation.viewmodel.AddPerformerToAlbumViewModel
import com.misw4203.vinilos.presentation.viewmodel.PerformerType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPerformerToAlbumScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddPerformerToAlbumViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val successTemplate = stringResource(R.string.add_performer_album_success)
    val networkErrorMessage = stringResource(R.string.add_performer_album_error_network)
    val serverErrorMessage = stringResource(R.string.add_performer_album_error_server)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddPerformerToAlbumEvent.AddedSuccessfully ->
                    snackbarHostState.showSnackbar(successTemplate.format(event.performerName))
                is AddPerformerToAlbumEvent.AddFailed -> {
                    val msg = if (event.isNetworkError) networkErrorMessage else serverErrorMessage
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag("add_performer_album_screen_root"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_performer_album_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("add_performer_album_back")) {
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
            when (val s = uiState) {
                AddPerformerToAlbumUiState.Loading -> LoadingState()
                is AddPerformerToAlbumUiState.Error -> if (s.type == null) {
                    ErrorState(onRetry = viewModel::retry, isNetworkError = s.isNetworkError)
                } else {
                    Content(form, null, null, viewModel)
                }
                else -> {
                    val adding = uiState as? AddPerformerToAlbumUiState.Adding
                    Content(
                        form = form,
                        addingMusicianId = adding?.takeIf { it.type == PerformerType.MUSICIAN }?.performerId,
                        addingBandId = adding?.takeIf { it.type == PerformerType.BAND }?.performerId,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    form: com.misw4203.vinilos.presentation.viewmodel.AddPerformerToAlbumFormState,
    addingMusicianId: Int?,
    addingBandId: Int?,
    viewModel: AddPerformerToAlbumViewModel,
) {
    val currentMusicians = form.allMusicians.filter { it.id in form.currentMusicianIds }
    val currentBands = form.allBands.filter { it.id in form.currentBandIds }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = if (form.selectedTab == PerformerType.MUSICIAN) 0 else 1,
            modifier = Modifier.testTag("add_performer_album_tabs"),
        ) {
            Tab(
                selected = form.selectedTab == PerformerType.MUSICIAN,
                onClick = { viewModel.onTabChange(PerformerType.MUSICIAN) },
                text = { Text(stringResource(R.string.add_favorite_performer_tab_musicians)) },
                modifier = Modifier.testTag("add_performer_album_tab_musicians"),
            )
            Tab(
                selected = form.selectedTab == PerformerType.BAND,
                onClick = { viewModel.onTabChange(PerformerType.BAND) },
                text = { Text(stringResource(R.string.add_favorite_performer_tab_bands)) },
                modifier = Modifier.testTag("add_performer_album_tab_bands"),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                OutlinedTextField(
                    value = form.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text(stringResource(R.string.add_favorite_performer_search_placeholder)) },
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
                        .testTag("add_performer_album_search"),
                )
            }
            when (form.selectedTab) {
                PerformerType.MUSICIAN -> {
                    item { SectionHeader(stringResource(R.string.add_favorite_performer_available_musicians)) }
                    if (form.filteredMusicians.isEmpty()) {
                        item {
                            EmptyText(
                                R.string.add_favorite_performer_empty_filter,
                                "add_performer_album_empty_musicians",
                            )
                        }
                    } else {
                        items(form.filteredMusicians, key = { it.id }) { m ->
                            MusicianRow(
                                musician = m,
                                isAdding = addingMusicianId == m.id,
                                onAdd = viewModel::onAddMusician,
                                modifier = Modifier.testTag("available_performer_album_${m.id}"),
                                addContentDescription = stringResource(R.string.cd_add_performer_to_album, m.name),
                                addingContentDescription = stringResource(R.string.cd_adding_performer_album),
                            )
                        }
                    }
                    item { SectionHeader(stringResource(R.string.add_performer_album_current_musicians)) }
                    items(currentMusicians, key = { "cur_${it.id}" }) { m ->
                        CurrentMusician(m, Modifier.testTag("current_performer_album_${m.id}"))
                    }
                }
                PerformerType.BAND -> {
                    item { SectionHeader(stringResource(R.string.add_favorite_performer_available_bands)) }
                    if (form.filteredBands.isEmpty()) {
                        item {
                            EmptyText(
                                R.string.add_favorite_performer_empty_filter,
                                "add_performer_album_empty_bands",
                            )
                        }
                    } else {
                        items(form.filteredBands, key = { it.id }) { b ->
                            BandRow(
                                band = b,
                                isAdding = addingBandId == b.id,
                                onAdd = viewModel::onAddBand,
                                modifier = Modifier.testTag("available_band_album_${b.id}"),
                            )
                        }
                    }
                    item { SectionHeader(stringResource(R.string.add_performer_album_current_bands)) }
                    items(currentBands, key = { "cur_${it.id}" }) { b ->
                        CurrentBand(b, Modifier.testTag("current_band_album_${b.id}"))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyText(textRes: Int, tag: String) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(tag),
    )
}

@Composable
private fun BandRow(
    band: BandSummary,
    isAdding: Boolean,
    onAdd: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val addCd = stringResource(R.string.cd_add_performer_to_album, band.name)
    val addingCd = stringResource(R.string.cd_adding_performer_album)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                AsyncImage(
                    model = band.image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = band.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        if (isAdding) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = addingCd },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable { onAdd(band.id) }
                    .semantics {
                        role = Role.Button
                        contentDescription = addCd
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun CurrentMusician(musician: MusicianSummary, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            AsyncImage(
                model = musician.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = musician.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CurrentBand(band: BandSummary, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            AsyncImage(
                model = band.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = band.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
