package com.misw4203.vinilos.presentation.ui.screens.artist

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.viewmodel.AddPrizeToMusicianEvent
import com.misw4203.vinilos.presentation.viewmodel.AddPrizeToMusicianFormState
import com.misw4203.vinilos.presentation.viewmodel.AddPrizeToMusicianUiState
import com.misw4203.vinilos.presentation.viewmodel.AddPrizeToMusicianViewModel
import com.misw4203.vinilos.presentation.viewmodel.DateValidationError
import com.misw4203.vinilos.presentation.viewmodel.isFormReady

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPrizeToMusicianScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddPrizeToMusicianViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val successTemplate = stringResource(R.string.add_prize_musician_success)
    val networkError = stringResource(R.string.add_prize_musician_error_network)
    val serverError = stringResource(R.string.add_prize_musician_error_server)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddPrizeToMusicianEvent.AddedSuccessfully ->
                    snackbarHostState.showSnackbar(successTemplate.format(event.prizeName))
                is AddPrizeToMusicianEvent.AddFailed -> {
                    val msg = if (event.isNetworkError) networkError else serverError
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag("add_prize_musician_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_prize_musician_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("add_prize_musician_back")) {
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
                AddPrizeToMusicianUiState.Loading -> LoadingState()
                is AddPrizeToMusicianUiState.Error -> if (s.prizeId == null) {
                    ErrorState(onRetry = viewModel::retry, isNetworkError = s.isNetworkError)
                } else {
                    ScreenContent(
                        form = form,
                        isAdding = false,
                        onQueryChange = viewModel::onQueryChange,
                        onPrizeSelected = viewModel::onPrizeSelected,
                        onDateSelected = viewModel::onDateSelected,
                        onConfirm = viewModel::onConfirm,
                    )
                }
                else -> ScreenContent(
                    form = form,
                    isAdding = uiState is AddPrizeToMusicianUiState.Adding,
                    onQueryChange = viewModel::onQueryChange,
                    onPrizeSelected = viewModel::onPrizeSelected,
                    onDateSelected = viewModel::onDateSelected,
                    onConfirm = viewModel::onConfirm,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenContent(
    form: AddPrizeToMusicianFormState,
    isAdding: Boolean,
    onQueryChange: (String) -> Unit,
    onPrizeSelected: (Prize) -> Unit,
    onDateSelected: (Long) -> Unit,
    onConfirm: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ArtistContextCard(name = form.musicianName, imageUrl = form.musicianImage)
        }

        item {
            SectionHeader(stringResource(R.string.add_prize_musician_search_section))
        }

        item {
            OutlinedTextField(
                value = form.query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.add_prize_musician_search_placeholder)) },
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
                    .testTag("add_prize_musician_search"),
            )
        }

        if (form.filteredPrizes.isEmpty() && form.query.isNotBlank()) {
            item {
                Text(
                    text = stringResource(R.string.add_prize_musician_available_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .testTag("add_prize_musician_empty_filter"),
                )
            }
        } else {
            items(form.filteredPrizes, key = { it.id }) { prize ->
                PrizeRow(
                    prize = prize,
                    isSelected = form.selectedPrize?.id == prize.id,
                    onSelect = { onPrizeSelected(prize) },
                    modifier = Modifier.testTag("available_prize_${prize.id}"),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        item {
            AssociationForm(
                selectedPrize = form.selectedPrize,
                premiationDate = form.premiationDate,
                dateError = form.dateError,
                isFormReady = form.isFormReady,
                isAdding = isAdding,
                onDateSelected = onDateSelected,
                onConfirm = onConfirm,
            )
        }

        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader(stringResource(R.string.add_prize_musician_current_section))
        }

        if (form.currentPrizes.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.add_prize_musician_empty_prizes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(form.currentPrizes, key = { "${it.id}_${it.premiationDate}" }) { prize ->
                CurrentPrizeItem(
                    prize = prize,
                    modifier = Modifier.testTag("current_prize_${prize.id}"),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssociationForm(
    selectedPrize: Prize?,
    premiationDate: String?,
    dateError: DateValidationError?,
    isFormReady: Boolean,
    isAdding: Boolean,
    onDateSelected: (Long) -> Unit,
    onConfirm: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val today = remember { System.currentTimeMillis() }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("add_prize_date_picker_confirm"),
                ) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(stringResource(R.string.add_prize_musician_details_section))

        if (selectedPrize != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = selectedPrize.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        val displayDate = premiationDate?.let { isoToDisplay(it) } ?: ""
        val errorText = when (dateError) {
            DateValidationError.FUTURE -> stringResource(R.string.add_prize_musician_date_error_future)
            DateValidationError.DUPLICATE -> stringResource(R.string.add_prize_musician_date_error_duplicate)
            null -> null
        }

        OutlinedTextField(
            value = displayDate,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.add_prize_musician_date_label)) },
            placeholder = { Text(stringResource(R.string.add_prize_musician_date_placeholder)) },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = stringResource(R.string.add_prize_musician_date_placeholder),
                    )
                }
            },
            isError = dateError != null,
            supportingText = errorText?.let { msg -> { Text(msg) } },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
                .testTag("add_prize_musician_date"),
        )

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
                .testTag("add_prize_musician_submit"),
        ) {
            if (isAdding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.surface,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = if (isAdding) stringResource(R.string.add_prize_musician_submitting)
                else stringResource(R.string.add_prize_musician_submit),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ArtistContextCard(name: String, imageUrl: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.artist_badge),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@Composable
private fun PrizeRow(
    prize: Prize,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    else MaterialTheme.colorScheme.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .clickable { onSelect() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = prize.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = prize.organization,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.cd_add_prize_to_musician, prize.name),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CurrentPrizeItem(
    prize: MusicianPrize,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = prize.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = prize.organization,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = isoToDisplay(prize.premiationDate),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
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

private fun isoToDisplay(iso: String): String =
    iso.take(10).split("-").reversed().joinToString("/")
