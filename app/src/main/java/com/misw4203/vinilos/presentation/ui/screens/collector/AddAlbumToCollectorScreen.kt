package com.misw4203.vinilos.presentation.ui.screens.collector

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.CollectorAlbum
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.viewmodel.AddAlbumToCollectorEvent
import com.misw4203.vinilos.presentation.viewmodel.AddAlbumToCollectorFormState
import com.misw4203.vinilos.presentation.viewmodel.AddAlbumToCollectorUiState
import com.misw4203.vinilos.presentation.viewmodel.AddAlbumToCollectorViewModel
import com.misw4203.vinilos.presentation.viewmodel.isFormReady
import com.misw4203.vinilos.presentation.viewmodel.isPriceValid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlbumToCollectorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddAlbumToCollectorViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler { onBack() }

    val snackbarHostState = remember { SnackbarHostState() }

    val successTemplate = stringResource(R.string.add_album_collector_success)
    val networkErrorMessage = stringResource(R.string.add_album_collector_error_network)
    val serverErrorMessage = stringResource(R.string.add_album_collector_error_server)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddAlbumToCollectorEvent.AddedSuccessfully ->
                    snackbarHostState.showSnackbar(successTemplate.format(event.albumName))
                is AddAlbumToCollectorEvent.AddFailed -> {
                    val msg = if (event.isNetworkError) networkErrorMessage else serverErrorMessage
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag("add_album_collector_screen"),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_album_collector_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("add_album_collector_back"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = uiState) {
                AddAlbumToCollectorUiState.Loading -> LoadingState()
                is AddAlbumToCollectorUiState.Error -> if (s.albumId == null) {
                    ErrorState(
                        onRetry = viewModel::retry,
                        isNetworkError = s.isNetworkError,
                    )
                } else {
                    ScreenContent(
                        form = form,
                        addingId = null,
                        onQueryChange = viewModel::onQueryChange,
                        onAlbumSelected = viewModel::onAlbumSelected,
                        onAlbumDeselected = viewModel::onAlbumDeselected,
                        onPriceChange = viewModel::onPriceChange,
                        onStatusChange = viewModel::onStatusChange,
                        onConfirm = viewModel::onConfirm,
                    )
                }
                else -> ScreenContent(
                    form = form,
                    addingId = (uiState as? AddAlbumToCollectorUiState.Adding)?.albumId,
                    onQueryChange = viewModel::onQueryChange,
                    onAlbumSelected = viewModel::onAlbumSelected,
                    onAlbumDeselected = viewModel::onAlbumDeselected,
                    onPriceChange = viewModel::onPriceChange,
                    onStatusChange = viewModel::onStatusChange,
                    onConfirm = viewModel::onConfirm,
                )
            }
        }
    }
}

@Composable
private fun ScreenContent(
    form: AddAlbumToCollectorFormState,
    addingId: Long?,
    onQueryChange: (String) -> Unit,
    onAlbumSelected: (Album) -> Unit,
    onAlbumDeselected: () -> Unit,
    onPriceChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(form.selectedAlbum?.id) {
        if (form.selectedAlbum != null) {
            // form item is at: 2 (header + search) + albums in list
            val formIndex = 2 + form.filteredAvailable.size
            scope.launch { listState.animateScrollToItem(formIndex) }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Sección: Buscar álbum ──────────────────────────────────────────
        item {
            SectionHeader(stringResource(R.string.add_album_collector_search_section))
        }

        item {
            OutlinedTextField(
                value = form.query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.add_album_collector_search_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
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
                    .testTag("add_album_collector_search"),
            )
        }

        // ── Lista de álbumes disponibles ───────────────────────────────────
        if (form.filteredAvailable.isEmpty() && form.query.isNotBlank()) {
            item {
                Text(
                    text = stringResource(R.string.add_album_collector_available_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .testTag("add_album_collector_empty_filter"),
                )
            }
        } else {
            items(form.filteredAvailable, key = { it.id }) { album ->
                AlbumRow(
                    album = album,
                    isSelected = form.selectedAlbum?.id == album.id,
                    isAdding = addingId == album.id,
                    onSelect = { onAlbumSelected(album) },
                    modifier = Modifier.testTag("available_album_${album.id}"),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 72.dp),
                )
            }
        }

        // ── Formulario condicional (aparece al seleccionar un álbum) ───────
        item {
            AnimatedVisibility(
                visible = form.selectedAlbum != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                form.selectedAlbum?.let { selected ->
                    SaleConditionsForm(
                        selectedAlbum = selected,
                        price = form.price,
                        status = form.status,
                        isPriceValid = form.isPriceValid,
                        isFormReady = form.isFormReady,
                        isAdding = addingId != null,
                        onPriceChange = onPriceChange,
                        onStatusChange = onStatusChange,
                        onConfirm = onConfirm,
                        onDeselect = onAlbumDeselected,
                    )
                }
            }
        }

        // ── Sección: Mi colección actual ───────────────────────────────────
        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader(stringResource(R.string.add_album_collector_current_section))
        }

        val currentInCollection = form.allAlbums.filter { it.id in form.currentAlbumIds }

        if (currentInCollection.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.add_album_collector_empty_collection),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            item {
                Text(
                    text = pluralStringResource(
                        R.plurals.collector_albums_count,
                        currentInCollection.size,
                        currentInCollection.size,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            items(currentInCollection, key = { "current_${it.id}" }) { album ->
                CurrentAlbumItem(
                    album = album,
                    modifier = Modifier.testTag("current_album_${album.id}"),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 72.dp),
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ─── Formulario de condiciones ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaleConditionsForm(
    selectedAlbum: Album,
    price: String,
    status: String,
    isPriceValid: Boolean,
    isFormReady: Boolean,
    isAdding: Boolean,
    onPriceChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDeselect: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("add_album_collector_form"),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.add_album_collector_form_section),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Álbum seleccionado (modo lectura)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(8.dp)
                    .clickable { onDeselect() },
            ) {
                AsyncImage(
                    model = selectedAlbum.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedAlbum.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (selectedAlbum.artistName.isNotBlank()) {
                        Text(
                            text = selectedAlbum.artistName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }

            // Estado del álbum
            StatusDropdown(
                selected = status,
                onSelect = onStatusChange,
            )

            // Precio estimado
            val priceError = price.isNotBlank() && !isPriceValid
            OutlinedTextField(
                value = price,
                onValueChange = onPriceChange,
                label = { Text(stringResource(R.string.add_album_collector_field_price)) },
                leadingIcon = {
                    Text(
                        text = "$",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                },
                isError = priceError,
                supportingText = if (priceError) {
                    { Text(stringResource(R.string.add_album_collector_price_error)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_album_collector_price"),
            )

            // Botón confirmar
            Button(
                onClick = onConfirm,
                enabled = isFormReady && !isAdding,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("add_album_collector_submit"),
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.surface,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.add_album_collector_submitting),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.add_album_collector_submit),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf("Active", "Inactive")
    val labelMap = mapOf(
        "Active" to "Activo",
        "Inactive" to "Inactivo",
    )
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = labelMap[selected] ?: selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.add_album_collector_field_status)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .testTag("add_album_collector_status"),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelMap[option] ?: option) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

// ─── Fila de álbum disponible ─────────────────────────────────────────────────

@Composable
private fun AlbumRow(
    album: Album,
    isSelected: Boolean,
    isAdding: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    else
        MaterialTheme.colorScheme.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(enabled = !isAdding) { onSelect() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = album.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (album.artistName.isNotBlank()) {
                Text(
                    text = album.artistName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (album.releaseYear.isNotBlank()) {
                Text(
                    text = album.releaseYear,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        if (isAdding) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_add_album_to_collector, album.name),
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ─── Fila de álbum en colección actual ───────────────────────────────────────

@Composable
private fun CurrentAlbumItem(
    album: Album,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = album.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (album.artistName.isNotBlank()) {
                Text(
                    text = album.artistName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

// ─── Section header ───────────────────────────────────────────────────────────

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
