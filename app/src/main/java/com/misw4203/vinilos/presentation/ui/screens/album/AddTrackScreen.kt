package com.misw4203.vinilos.presentation.ui.screens.album

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.presentation.viewmodel.AddTrackUiState
import com.misw4203.vinilos.presentation.viewmodel.AddTrackViewModel
import com.misw4203.vinilos.presentation.viewmodel.AlbumDetailUiState
import com.misw4203.vinilos.presentation.viewmodel.AlbumDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrackScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddTrackViewModel = hiltViewModel(),
    albumDetailViewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val albumUiState by albumDetailViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var durationFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AddTrackUiState.Success -> onSuccess()
            is AddTrackUiState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_track_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .testTag("add_track_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (albumUiState is AlbumDetailUiState.Success) {
                AlbumHeaderCard((albumUiState as AlbumDetailUiState.Success).album)
            }

            Text(
                text = stringResource(R.string.add_track_section_new).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = viewModel.name,
                onValueChange = {
                    viewModel.name = it
                    viewModel.nameError = null
                },
                label = { Text(stringResource(R.string.add_track_label_name)) },
                isError = viewModel.nameError != null,
                supportingText = viewModel.nameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_track_name"),
            )

            OutlinedTextField(
                value = durationFieldValue,
                onValueChange = { incoming ->
                    val digits = incoming.text.filter { it.isDigit() }.take(4)
                    val formatted = if (digits.length >= 3) {
                        "${digits.substring(0, 2)}:${digits.substring(2)}"
                    } else {
                        digits
                    }
                    durationFieldValue = TextFieldValue(
                        text = formatted,
                        selection = TextRange(formatted.length),
                    )
                    viewModel.duration = formatted
                    viewModel.durationError = null
                },
                label = { Text(stringResource(R.string.add_track_label_duration)) },
                isError = viewModel.durationError != null,
                supportingText = viewModel.durationError?.let { { Text(it) } },
                placeholder = { Text("03:45") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_track_duration"),
            )

            Button(
                onClick = viewModel::submit,
                enabled = uiState !is AddTrackUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_track_submit"),
            ) {
                if (uiState is AddTrackUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.add_track_button))
                }
            }
        }
    }
}

@Composable
private fun AlbumHeaderCard(album: AlbumDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = album.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Column {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (album.artistName.isNotBlank()) {
                Spacer(Modifier.width(0.dp))
                Text(
                    text = album.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
