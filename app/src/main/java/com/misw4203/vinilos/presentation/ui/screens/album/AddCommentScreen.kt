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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misw4203.vinilos.R
import com.misw4203.vinilos.presentation.ui.components.RatingBar
import com.misw4203.vinilos.presentation.viewmodel.AddCommentFormState
import com.misw4203.vinilos.presentation.viewmodel.AddCommentUiState
import com.misw4203.vinilos.presentation.viewmodel.AddCommentViewModel
import com.misw4203.vinilos.presentation.viewmodel.FormError

private const val MaxDescriptionChars = 500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCommentScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddCommentViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val networkErrorMessage = stringResource(R.string.add_comment_error_network)
    val serverErrorMessage = stringResource(R.string.add_comment_error_server)

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AddCommentUiState.Success -> onSuccess()
            is AddCommentUiState.Error -> {
                val message = if (state.isNetworkError) networkErrorMessage else serverErrorMessage
                snackbarHostState.showSnackbar(message)
                viewModel.resetError()
            }
            else -> Unit
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("add_comment_screen"),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_comment_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(CircleShape)
                            .testTag("add_comment_back"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        AddCommentContent(
            form = form,
            uiState = uiState,
            onDescriptionChange = viewModel::onDescriptionChange,
            onRatingChange = viewModel::onRatingChange,
            onSubmit = viewModel::submit,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        )
    }
}

@Composable
private fun AddCommentContent(
    form: AddCommentFormState,
    uiState: AddCommentUiState,
    onDescriptionChange: (String) -> Unit,
    onRatingChange: (Int) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading = uiState is AddCommentUiState.Loading

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        SectionHeading(text = stringResource(R.string.add_comment_section))
        Spacer(Modifier.height(20.dp))

        FieldLabel(text = stringResource(R.string.add_comment_label_rating))
        Spacer(Modifier.height(10.dp))
        RatingBar(
            rating = form.rating,
            onRatingChange = onRatingChange,
        )
        if (form.ratingError == FormError.InvalidRating) {
            Spacer(Modifier.height(6.dp))
            ErrorText(stringResource(R.string.add_comment_error_rating))
        }

        Spacer(Modifier.height(24.dp))

        FieldLabel(text = stringResource(R.string.add_comment_label_description))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = form.description,
            onValueChange = { value ->
                if (value.length <= MaxDescriptionChars) onDescriptionChange(value)
            },
            placeholder = {
                Text(
                    text = stringResource(R.string.add_comment_placeholder),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            },
            isError = form.descriptionError == FormError.EmptyDescription,
            supportingText = {
                val errorText = if (form.descriptionError == FormError.EmptyDescription) {
                    stringResource(R.string.add_comment_error_description)
                } else null
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = errorText.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(
                            R.string.add_comment_max_chars,
                            MaxDescriptionChars,
                        ),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp)
                .testTag("add_comment_description"),
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onSubmit,
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("add_comment_submit"),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stringResource(R.string.add_comment_submit).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeading(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}
