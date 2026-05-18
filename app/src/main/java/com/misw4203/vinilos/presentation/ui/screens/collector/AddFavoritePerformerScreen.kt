package com.misw4203.vinilos.presentation.ui.screens.collector

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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.pluralStringResource
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
import com.misw4203.vinilos.presentation.viewmodel.AddFavoritePerformerEvent
import com.misw4203.vinilos.presentation.viewmodel.AddFavoritePerformerUiState
import com.misw4203.vinilos.presentation.viewmodel.AddFavoritePerformerViewModel
import com.misw4203.vinilos.presentation.viewmodel.PerformerType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFavoritePerformerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddFavoritePerformerViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val successTemplate = stringResource(R.string.add_favorite_performer_success)
    val networkErrorMessage = stringResource(R.string.add_favorite_performer_error_network)
    val serverErrorMessage = stringResource(R.string.add_favorite_performer_error_server)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddFavoritePerformerEvent.AddedSuccessfully ->
                    snackbarHostState.showSnackbar(successTemplate.format(event.performerName))
                is AddFavoritePerformerEvent.AddFailed -> {
                    val msg = if (event.isNetworkError) networkErrorMessage else serverErrorMessage
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag("add_favorite_performer_screen_root"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_favorite_performer_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("add_favorite_performer_back")) {
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
                AddFavoritePerformerUiState.Loading -> LoadingState()
                is AddFavoritePerformerUiState.Error -> if (s.type == null) {
                    ErrorState(onRetry = viewModel::retry, isNetworkError = s.isNetworkError)
                } else {
                    Content(
                        collectorName = form.collectorName,
                        selectedTab = form.selectedTab,
                        onTabChange = viewModel::onTabChange,
                        queryValue = form.query,
                        onQueryChange = viewModel::onQueryChange,
                        availableMusicians = form.filteredMusicians,
                        availableBands = form.filteredBands,
                        currentMusicians = form.allMusicians.filter { it.id in form.favoriteMusicianIds },
                        currentBands = form.allBands.filter { it.id in form.favoriteBandIds },
                        addingMusicianId = null,
                        addingBandId = null,
                        onAddMusician = viewModel::onAddMusician,
                        onAddBand = viewModel::onAddBand,
                    )
                }
                else -> {
                    val adding = uiState as? AddFavoritePerformerUiState.Adding
                    Content(
                        collectorName = form.collectorName,
                        selectedTab = form.selectedTab,
                        onTabChange = viewModel::onTabChange,
                        queryValue = form.query,
                        onQueryChange = viewModel::onQueryChange,
                        availableMusicians = form.filteredMusicians,
                        availableBands = form.filteredBands,
                        currentMusicians = form.allMusicians.filter { it.id in form.favoriteMusicianIds },
                        currentBands = form.allBands.filter { it.id in form.favoriteBandIds },
                        addingMusicianId = adding?.takeIf { it.type == PerformerType.MUSICIAN }?.performerId,
                        addingBandId = adding?.takeIf { it.type == PerformerType.BAND }?.performerId,
                        onAddMusician = viewModel::onAddMusician,
                        onAddBand = viewModel::onAddBand,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    collectorName: String,
    selectedTab: PerformerType,
    onTabChange: (PerformerType) -> Unit,
    queryValue: String,
    onQueryChange: (String) -> Unit,
    availableMusicians: List<MusicianSummary>,
    availableBands: List<BandSummary>,
    currentMusicians: List<MusicianSummary>,
    currentBands: List<BandSummary>,
    addingMusicianId: Int?,
    addingBandId: Int?,
    onAddMusician: (Int) -> Unit,
    onAddBand: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = if (selectedTab == PerformerType.MUSICIAN) 0 else 1,
            modifier = Modifier.testTag("add_favorite_performer_tabs"),
        ) {
            Tab(
                selected = selectedTab == PerformerType.MUSICIAN,
                onClick = { onTabChange(PerformerType.MUSICIAN) },
                text = { Text(stringResource(R.string.add_favorite_performer_tab_musicians)) },
                modifier = Modifier.testTag("add_favorite_performer_tab_musicians"),
            )
            Tab(
                selected = selectedTab == PerformerType.BAND,
                onClick = { onTabChange(PerformerType.BAND) },
                text = { Text(stringResource(R.string.add_favorite_performer_tab_bands)) },
                modifier = Modifier.testTag("add_favorite_performer_tab_bands"),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { CollectorContextCard(collectorName) }
            item {
                OutlinedTextField(
                    value = queryValue,
                    onValueChange = onQueryChange,
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
                        .testTag("add_favorite_performer_search"),
                )
            }
            when (selectedTab) {
                PerformerType.MUSICIAN -> musicianTabItems(
                    available = availableMusicians,
                    current = currentMusicians,
                    addingId = addingMusicianId,
                    onAdd = onAddMusician,
                )
                PerformerType.BAND -> bandTabItems(
                    available = availableBands,
                    current = currentBands,
                    addingId = addingBandId,
                    onAdd = onAddBand,
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.musicianTabItems(
    available: List<MusicianSummary>,
    current: List<MusicianSummary>,
    addingId: Int?,
    onAdd: (Int) -> Unit,
) {
    item { SectionHeader(text = sectionHeaderText(R.string.add_favorite_performer_available_musicians)) }
    if (available.isEmpty()) {
        item {
            EmptyText(
                textRes = R.string.add_favorite_performer_empty_filter,
                tag = "add_favorite_performer_empty_musicians",
            )
        }
    } else {
        items(available, key = { it.id }) { musician ->
            FavoriteMusicianRow(
                musician = musician,
                isAdding = addingId == musician.id,
                onAdd = onAdd,
                modifier = Modifier.testTag("available_favorite_performer_${musician.id}"),
            )
        }
    }
    item { SectionHeader(text = sectionHeaderText(R.string.add_favorite_performer_current_musicians)) }
    item {
        CountText(
            count = current.size,
            pluralRes = R.plurals.favorite_musicians_count,
        )
    }
    items(current, key = { it.id }) { musician ->
        CurrentMusicianItem(
            musician = musician,
            modifier = Modifier.testTag("current_favorite_performer_${musician.id}"),
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.bandTabItems(
    available: List<BandSummary>,
    current: List<BandSummary>,
    addingId: Int?,
    onAdd: (Int) -> Unit,
) {
    item { SectionHeader(text = sectionHeaderText(R.string.add_favorite_performer_available_bands)) }
    if (available.isEmpty()) {
        item {
            EmptyText(
                textRes = R.string.add_favorite_performer_empty_filter,
                tag = "add_favorite_performer_empty_bands",
            )
        }
    } else {
        items(available, key = { it.id }) { band ->
            FavoriteBandRow(
                band = band,
                isAdding = addingId == band.id,
                onAdd = onAdd,
                modifier = Modifier.testTag("available_favorite_band_${band.id}"),
            )
        }
    }
    item { SectionHeader(text = sectionHeaderText(R.string.add_favorite_performer_current_bands)) }
    item {
        CountText(
            count = current.size,
            pluralRes = R.plurals.favorite_bands_count,
        )
    }
    items(current, key = { it.id }) { band ->
        CurrentBandItem(
            band = band,
            modifier = Modifier.testTag("current_favorite_band_${band.id}"),
        )
    }
}

@Composable
private fun sectionHeaderText(@androidx.annotation.StringRes id: Int): String = stringResource(id)

@Composable
private fun EmptyText(
    @androidx.annotation.StringRes textRes: Int,
    tag: String,
) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(tag),
    )
}

@Composable
private fun CountText(count: Int, @androidx.annotation.PluralsRes pluralRes: Int) {
    Text(
        text = pluralStringResource(pluralRes, count, count),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun FavoriteMusicianRow(
    musician: MusicianSummary,
    isAdding: Boolean,
    onAdd: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    MusicianRow(
        musician = musician,
        isAdding = isAdding,
        onAdd = onAdd,
        modifier = modifier,
        addContentDescription = stringResource(R.string.cd_add_musician_to_favorites, musician.name),
        addingContentDescription = stringResource(R.string.cd_adding_favorite_performer),
    )
}

@Composable
private fun FavoriteBandRow(
    band: BandSummary,
    isAdding: Boolean,
    onAdd: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val addCd = stringResource(R.string.cd_add_band_to_favorites, band.name)
    val addingCd = stringResource(R.string.cd_adding_favorite_performer)
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
private fun CurrentMusicianItem(
    musician: MusicianSummary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
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
        Column {
            Text(
                text = musician.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (musician.birthDate.isNotBlank()) {
                Text(
                    text = musician.birthDate.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CurrentBandItem(
    band: BandSummary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
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
private fun CollectorContextCard(collectorName: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("add_favorite_performer_context"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.add_favorite_performer_context_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = collectorName.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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

