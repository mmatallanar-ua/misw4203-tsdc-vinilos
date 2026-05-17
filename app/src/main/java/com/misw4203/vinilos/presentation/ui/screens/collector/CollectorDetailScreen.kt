package com.misw4203.vinilos.presentation.ui.screens.collector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.CollectorAlbum
import com.misw4203.vinilos.domain.model.CollectorComment
import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.viewmodel.CollectorDetailEvent
import com.misw4203.vinilos.presentation.viewmodel.CollectorDetailUiState
import com.misw4203.vinilos.presentation.viewmodel.CollectorDetailViewModel

private val HeaderHeight = 260.dp
private val CardOverlap = 32.dp
private val CardRadius = 24.dp

@Composable
fun CollectorDetailScreen(
    collectorId: Int,
    onBack: () -> Unit,
    onAddAlbum: () -> Unit = {},
    refreshKey: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    modifier: Modifier = Modifier,
    onAddFavoritePerformer: () -> Unit = {},
    viewModel: CollectorDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(refreshKey) {
        if (refreshKey) {
            viewModel.retry()
            onRefreshHandled()
        }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val removeSuccess = stringResource(R.string.collector_remove_success)
    val removeErrorNetwork = stringResource(R.string.collector_remove_error_network)
    val removeErrorServer = stringResource(R.string.collector_remove_error_server)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is CollectorDetailEvent.Removed -> removeSuccess.format(event.name)
                is CollectorDetailEvent.RemoveFailed ->
                    if (event.isNetworkError) removeErrorNetwork else removeErrorServer
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    var pending by remember { mutableStateOf<PendingRemoval?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is CollectorDetailUiState.Loading -> LoadingState()
            is CollectorDetailUiState.Success -> CollectorDetailContent(
                collector = state.collector,
                onBack = onBack,
                onAddAlbum = onAddAlbum,
                onAddFavoritePerformer = onAddFavoritePerformer,
                onRemoveFavorite = { pending = PendingRemoval.Favorite(it) },
                onRemoveAlbum = { pending = PendingRemoval.AlbumInCollection(it) },
            )
            is CollectorDetailUiState.NotFound -> NotFoundState(onBack)
            is CollectorDetailUiState.Error -> ErrorState(
                onRetry = viewModel::retry,
                isNetworkError = state.isNetworkError,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    pending?.let { removal ->
        val (body, name) = when (removal) {
            is PendingRemoval.Favorite ->
                stringResource(
                    R.string.collector_remove_favorite_confirm_body,
                    removal.performer.name,
                ) to removal.performer.name
            is PendingRemoval.AlbumInCollection ->
                stringResource(
                    R.string.collector_remove_album_confirm_body,
                    removal.collectorAlbum.album?.name.orEmpty(),
                ) to removal.collectorAlbum.album?.name.orEmpty()
        }
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.collector_remove_confirm_title)) },
            text = { Text(body) },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (removal) {
                            is PendingRemoval.Favorite ->
                                viewModel.removeFavorite(removal.performer)
                            is PendingRemoval.AlbumInCollection ->
                                viewModel.removeAlbum(removal.collectorAlbum)
                        }
                        pending = null
                    },
                    modifier = Modifier.testTag("collector_remove_confirm"),
                ) {
                    Text(stringResource(R.string.collector_remove_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

private sealed interface PendingRemoval {
    data class Favorite(val performer: Performer) : PendingRemoval
    data class AlbumInCollection(val collectorAlbum: CollectorAlbum) : PendingRemoval
}

@Composable
private fun CollectorDetailContent(
    collector: CollectorDetail,
    onBack: () -> Unit,
    onAddAlbum: () -> Unit,
    onAddFavoritePerformer: () -> Unit,
    onRemoveFavorite: (Performer) -> Unit,
    onRemoveAlbum: (CollectorAlbum) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().testTag("collector_detail_root")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Header background with avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeaderHeight)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = stringResource(R.string.cd_collector_avatar),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(72.dp),
                    )
                }
            }

            // Content card overlapping the header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = -CardOverlap),
                shape = RoundedCornerShape(topStart = CardRadius, topEnd = CardRadius),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                ) {
                    // Header — name, email, phone centrados
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Name
                        Text(
                            text = collector.name,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics { heading() },
                        )

                        // Email
                        val emailDesc = stringResource(R.string.cd_collector_email, collector.email)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.semantics(mergeDescendants = true) {
                                contentDescription = emailDesc
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mail,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = collector.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Telephone
                        val phoneDesc = stringResource(R.string.cd_collector_phone, collector.telephone)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.semantics(mergeDescendants = true) {
                                contentDescription = phoneDesc
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = collector.telephone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Description
                        if (collector.description.isNotBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = collector.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    // Sections — left-aligned
                    Spacer(Modifier.height(28.dp))
                    AlbumsSection(
                        albums = collector.collectorAlbums,
                        onAddAlbum = onAddAlbum,
                        onRemoveAlbum = onRemoveAlbum,
                    )

                    Spacer(Modifier.height(28.dp))
                    PerformersSection(
                        performers = collector.favoritePerformers,
                        onAddFavoritePerformer = onAddFavoritePerformer,
                        onRemoveFavorite = onRemoveFavorite,
                    )

                    if (collector.comments.isNotEmpty()) {
                        Spacer(Modifier.height(28.dp))
                        CommentsSection(collector.comments)
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        // Back button floating on header
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 8.dp, start = 8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                .size(40.dp)
                .testTag("collector_detail_back"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ─── Section header (same pattern as AlbumDetailScreen) ──────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() },
    )
}

// ─── Albums ───────────────────────────────────────────────────────────────────

@Composable
private fun AlbumsSection(
    albums: List<CollectorAlbum>,
    onAddAlbum: () -> Unit,
    onRemoveAlbum: (CollectorAlbum) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SectionHeader(stringResource(R.string.collector_section_albums))
        Button(
            onClick = onAddAlbum,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 14.dp,
                vertical = 6.dp,
            ),
            modifier = Modifier.testTag("collector_add_album_cta"),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.add_album_collector_cta),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    if (albums.isEmpty()) {
        Text(
            text = stringResource(R.string.add_album_collector_empty_collection),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(albums, key = { it.id }) { collectorAlbum ->
                CollectorAlbumCard(collectorAlbum, onRemoveAlbum)
            }
        }
    }
}

@Composable
private fun CollectorAlbumCard(
    collectorAlbum: CollectorAlbum,
    onRemove: (CollectorAlbum) -> Unit,
) {
    val albumName = collectorAlbum.album?.name.orEmpty()
    Column(
        modifier = Modifier
            .width(120.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Box(modifier = Modifier.size(120.dp)) {
            AsyncImage(
                model = collectorAlbum.album?.coverUrl,
                contentDescription = if (albumName.isNotBlank()) {
                    stringResource(R.string.cd_album_cover_of, albumName)
                } else null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
            IconButton(
                onClick = { onRemove(collectorAlbum) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .testTag("collector_remove_album_${collectorAlbum.id}"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(
                        R.string.collector_remove_album_cd,
                        albumName,
                    ),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = collectorAlbum.album?.name.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (collectorAlbum.album?.artistName?.isNotBlank() == true) {
            Text(
                text = collectorAlbum.album.artistName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$${collectorAlbum.price.toInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val isActive = collectorAlbum.status == "Active"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = collectorAlbum.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── Performers ───────────────────────────────────────────────────────────────

@Composable
private fun PerformersSection(
    performers: List<Performer>,
    onAddFavoritePerformer: () -> Unit,
    onRemoveFavorite: (Performer) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionHeader(stringResource(R.string.collector_section_performers))
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onAddFavoritePerformer,
            modifier = Modifier
                .size(32.dp)
                .testTag("collector_detail_add_favorite_performer"),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.cd_add_favorite_performer),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    if (performers.isEmpty()) {
        Text(
            text = stringResource(R.string.collector_no_favorite_performers),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("collector_detail_no_favorites"),
        )
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(performers, key = { "${it.kind}_${it.id}" }) { performer ->
                PerformerChip(performer, onRemoveFavorite)
            }
        }
    }
}

@Composable
private fun PerformerChip(performer: Performer, onRemove: (Performer) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AsyncImage(
            model = performer.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Text(
            text = performer.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(
            onClick = { onRemove(performer) },
            modifier = Modifier
                .size(28.dp)
                .testTag("collector_remove_favorite_${performer.kind}_${performer.id}"),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(
                    R.string.collector_remove_favorite_cd,
                    performer.name,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ─── Comments ─────────────────────────────────────────────────────────────────

@Composable
private fun CommentsSection(comments: List<CollectorComment>) {
    SectionHeader(stringResource(R.string.collector_section_comments))
    Spacer(Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        comments.forEach { comment ->
            CommentCard(comment)
        }
    }
}

@Composable
private fun CommentCard(comment: CollectorComment) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val ratingDesc = stringResource(R.string.cd_rating, comment.rating)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.semantics { contentDescription = ratingDesc },
        ) {
            repeat(5) { index ->
                Text(
                    text = if (index < comment.rating) "★" else "☆",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (index < comment.rating)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
        if (comment.albumName.isNotBlank()) {
            Text(
                text = stringResource(R.string.collector_comment_album_ref, comment.albumName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (comment.description.isNotBlank()) {
            Text(
                text = comment.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ─── Not found ────────────────────────────────────────────────────────────────

@Composable
private fun NotFoundState(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.collector_not_found_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.collector_not_found_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 8.dp, start = 8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .size(40.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
            )
        }
    }
}
