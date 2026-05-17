package com.misw4203.vinilos.presentation.ui.screens.prize

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.viewmodel.CreatePrizeUiState
import com.misw4203.vinilos.presentation.viewmodel.CreatePrizeViewModel

private const val MAX_NAME_LENGTH = 100
private const val MAX_ORG_LENGTH = 100
private const val MAX_DESC_LENGTH = 500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePrizeScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreatePrizeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by rememberSaveable { mutableStateOf("") }
    var organization by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    var showCancelDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = name.isNotBlank() || organization.isNotBlank() || description.isNotBlank()
    val isSubmitting = uiState is CreatePrizeUiState.Submitting
    val existingPrizes = (uiState as? CreatePrizeUiState.Ready)?.existingPrizes ?: emptyList()

    val successMessage = stringResource(R.string.create_prize_success)
    val retryLabel = stringResource(android.R.string.ok)

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is CreatePrizeUiState.Success -> {
                onSuccess()
                viewModel.resetState()
            }
            is CreatePrizeUiState.Error -> {
                val result = snackbarHostState.showSnackbar(
                    message = state.message,
                    actionLabel = "Reintentar",
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.submit(name, description, organization)
                } else {
                    viewModel.resetState()
                }
            }
            else -> Unit
        }
    }

    val navigateBack: () -> Unit = {
        if (hasUnsavedChanges) showCancelDialog = true else onBack()
    }

    BackHandler(enabled = hasUnsavedChanges) {
        showCancelDialog = true
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(R.string.create_prize_cancel_confirm_title)) },
            text = { Text(stringResource(R.string.create_prize_cancel_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        name = ""
                        organization = ""
                        description = ""
                        onBack()
                    },
                    modifier = Modifier.testTag("cancel_confirm_discard"),
                ) {
                    Text("Descartar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelDialog = false },
                    modifier = Modifier.testTag("cancel_confirm_keep"),
                ) {
                    Text("Seguir editando")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.create_prize_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigateBack,
                        modifier = Modifier.testTag("create_prize_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Form card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.create_prize_title).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() },
                        )
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= MAX_NAME_LENGTH) name = it },
                        label = { Text(stringResource(R.string.create_prize_field_name)) },
                        supportingText = { Text("${name.length}/$MAX_NAME_LENGTH") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_prize_name"),
                    )

                    OutlinedTextField(
                        value = organization,
                        onValueChange = { if (it.length <= MAX_ORG_LENGTH) organization = it },
                        label = { Text(stringResource(R.string.create_prize_field_organization)) },
                        supportingText = { Text("${organization.length}/$MAX_ORG_LENGTH") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_prize_organization"),
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= MAX_DESC_LENGTH) description = it },
                        label = { Text(stringResource(R.string.create_prize_field_description)) },
                        supportingText = { Text("${description.length}/$MAX_DESC_LENGTH") },
                        minLines = 4,
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_prize_description"),
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { viewModel.submit(name, description, organization) },
                            enabled = name.isNotBlank() && organization.isNotBlank() && description.isNotBlank() && !isSubmitting,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface,
                                contentColor = MaterialTheme.colorScheme.surface,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("create_prize_submit"),
                        ) {
                            AnimatedVisibility(visible = isSubmitting) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.surface,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                            }
                            Icon(
                                imageVector = Icons.Outlined.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isSubmitting) stringResource(R.string.create_prize_saving)
                                else stringResource(R.string.create_prize_save),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }

                        OutlinedButton(
                            onClick = navigateBack,
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("create_prize_cancel"),
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Existing prizes list section
            RegisteredPrizesSection(uiState = uiState, existingPrizes = existingPrizes)

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RegisteredPrizesSection(
    uiState: CreatePrizeUiState,
    existingPrizes: List<Prize>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.create_prize_registered).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = androidx.compose.ui.unit.TextUnit(0.12f, androidx.compose.ui.unit.TextUnitType.Em),
            modifier = Modifier
                .semantics { heading() }
                .testTag("registered_prizes_section"),
        )

        when (uiState) {
            is CreatePrizeUiState.LoadingPrizes -> LoadingState(modifier = Modifier.height(80.dp))
            else -> {
                if (existingPrizes.isEmpty()) {
                    Text(
                        text = "No hay premios registrados aún.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    existingPrizes.forEach { prize ->
                        PrizeCard(
                            prize = prize,
                            modifier = Modifier.testTag("registered_prize_${prize.id}"),
                        )
                    }
                }
            }
        }
    }
}
