package com.misw4203.vinilos.presentation.ui.screens.album

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.Track
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.viewmodel.AlbumDetailEvent
import com.misw4203.vinilos.presentation.viewmodel.AlbumDetailUiState
import com.misw4203.vinilos.presentation.viewmodel.AlbumDetailViewModel

private val CoverHeight = 300.dp
private val CardOverlap = 32.dp
private val CardRadius = 24.dp

@Composable
fun AlbumDetailScreen(
    albumId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onAddTrack: () -> Unit = {},
    onAddComment: () -> Unit = {},
    onAddPerformer: () -> Unit = {},
    refreshKey: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    viewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(refreshKey) {
        if (refreshKey) {
            viewModel.retry()
            onRefreshHandled()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val removeSuccess = stringResource(R.string.album_remove_success)
    val removeErrorNetwork = stringResource(R.string.album_remove_error_network)
    val removeErrorServer = stringResource(R.string.album_remove_error_server)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is AlbumDetailEvent.Removed -> removeSuccess
                is AlbumDetailEvent.RemoveFailed ->
                    if (event.isNetworkError) removeErrorNetwork else removeErrorServer
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    var pending by remember { mutableStateOf<PendingRemoval?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is AlbumDetailUiState.Loading -> LoadingState()
            is AlbumDetailUiState.Success -> AlbumDetailContent(
                album = state.album,
                onBack = onBack,
                onAddTrack = onAddTrack,
                onAddComment = onAddComment,
                onRemoveTrack = { pending = PendingRemoval.TrackRemoval(it) },
                onRemoveComment = { pending = PendingRemoval.CommentRemoval(it) },
                onAddPerformer = onAddPerformer,
            )
            is AlbumDetailUiState.NotFound -> NotFoundState(onBack = onBack)
            is AlbumDetailUiState.Error -> ErrorState(
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
        val body = when (removal) {
            is PendingRemoval.TrackRemoval ->
                stringResource(R.string.album_remove_track_confirm_body, removal.track.name)
            is PendingRemoval.CommentRemoval ->
                stringResource(R.string.album_remove_comment_confirm_body)
        }
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.album_remove_confirm_title)) },
            text = { Text(body) },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (removal) {
                            is PendingRemoval.TrackRemoval -> viewModel.removeTrack(removal.track)
                            is PendingRemoval.CommentRemoval -> viewModel.removeComment(removal.comment)
                        }
                        pending = null
                    },
                    modifier = Modifier.testTag("album_remove_confirm"),
                ) {
                    Text(stringResource(R.string.album_remove_action))
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
    data class TrackRemoval(val track: Track) : PendingRemoval
    data class CommentRemoval(val comment: Comment) : PendingRemoval
}

@Composable
private fun AlbumDetailContent(
    album: AlbumDetail,
    onBack: () -> Unit,
    onAddTrack: () -> Unit,
    onAddComment: () -> Unit,
    onRemoveTrack: (Track) -> Unit,
    onRemoveComment: (Comment) -> Unit,
    onAddPerformer: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().testTag("album_detail_root")) {
        // Surface card overlaps the cover by CardOverlap via a negative offset.
        // LazyColumn items are independent, so the single continuous surface is
        // reproduced by giving every content item the surface background +
        // horizontal padding and shifting them all up by CardOverlap; only the
        // first content item gets the rounded-top clip + top padding, and a
        // trailing spacer of (original bottom 32.dp + CardOverlap) keeps the net
        // scroll length unchanged with no gap at the bottom.
        val surfaceColor = MaterialTheme.colorScheme.surface
        val contentItemModifier = Modifier
            .fillMaxWidth()
            .offset(y = -CardOverlap)
            .background(surfaceColor)
            .padding(horizontal = 24.dp)
        val firstContentItemModifier = Modifier
            .fillMaxWidth()
            .offset(y = -CardOverlap)
            .clip(RoundedCornerShape(topStart = CardRadius, topEnd = CardRadius))
            .background(surfaceColor)
            .padding(horizontal = 24.dp)
            .padding(top = 28.dp)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Cover image
            item {
                Box(modifier = Modifier.fillMaxWidth().height(CoverHeight)) {
                    AsyncImage(
                        model = album.coverUrl,
                        contentDescription = stringResource(R.string.cd_album_cover_of, album.name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Static header content: title → add-comment button → performers,
            // plus the tracks section header (the track rows themselves are
            // virtualized below).
            item {
                Column(modifier = firstContentItemModifier) {
                    // Title + artist
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (album.artistName.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = album.artistName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Metadata chips
                    Spacer(Modifier.height(16.dp))
                    MetadataChips(album)

                    // Description
                    if (album.description.isNotBlank()) {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = album.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Action: comment
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onAddComment,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("album_detail_add_comment"),
                    ) {
                        Text(
                            text = stringResource(R.string.add_comment_action_cta).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Performers
                    Spacer(Modifier.height(28.dp))
                    PerformersSection(
                        performers = album.performers,
                        onAddPerformer = onAddPerformer,
                    )

                    // Tracks header
                    Spacer(Modifier.height(28.dp))
                    TracksHeader(trackCount = album.tracks.size, onAddTrack = onAddTrack)
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Tracks (virtualized)
            itemsIndexed(album.tracks, key = { _, t -> t.id }) { index, track ->
                Column(modifier = contentItemModifier) {
                    TrackRow(index = index + 1, track = track, onRemove = onRemoveTrack)
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Comments section header (only if comments non-empty)
            if (album.comments.isNotEmpty()) {
                item {
                    Column(modifier = contentItemModifier) {
                        Spacer(Modifier.height(28.dp))
                        SectionHeader(stringResource(R.string.detail_section_comments))
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // Comments (virtualized). Original list used Arrangement.spacedBy(12.dp);
                // reproduced with a 12.dp leading spacer on every card after the first.
                itemsIndexed(album.comments, key = { _, c -> c.id }) { index, comment ->
                    Column(modifier = contentItemModifier) {
                        if (index > 0) Spacer(Modifier.height(12.dp))
                        CommentCard(comment, onRemoveComment)
                    }
                }
            }

            // Record label + trailing spacer (compensates the negative offset)
            item {
                Column(modifier = contentItemModifier) {
                    if (album.recordLabel.isNotBlank()) {
                        Spacer(Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.detail_label_record_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = album.recordLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    // Original trailing Spacer(32.dp) + bottom padding(28.dp) of the
                    // card, plus CardOverlap to offset the negative shift so the net
                    // scroll length is identical and there is no gap at the bottom.
                    Spacer(Modifier.height(32.dp + 28.dp + CardOverlap))
                }
            }
        }

        // Back button overlaid on the cover image
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 8.dp, start = 8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                .size(40.dp)
                .testTag("album_detail_back"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetadataChips(album: AlbumDetail) {
    val year = album.releaseDate.take(4)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (year.isNotBlank()) MetadataChip(year)
        if (album.genre.isNotBlank()) MetadataChip(album.genre.uppercase())
    }
}

@Composable
private fun MetadataChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TracksHeader(trackCount: Int, onAddTrack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionHeader(stringResource(R.string.detail_section_tracks))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.detail_tracks_total, trackCount).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onAddTrack, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_track_button_album_detail),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun TrackRow(index: Int, track: Track, onRemove: (Track) -> Unit) {
    val rowDesc = if (track.duration.isNotBlank()) {
        stringResource(R.string.cd_track_with_duration, index, track.name, track.duration)
    } else {
        stringResource(R.string.cd_track_no_duration, index, track.name)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) { contentDescription = rowDesc },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = index.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.width(28.dp),
        )
        Text(
            text = track.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (track.duration.isNotBlank()) {
            Text(
                text = track.duration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = { onRemove(track) },
            modifier = Modifier
                .size(28.dp)
                .testTag("album_remove_track_${track.id}"),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.album_remove_track_cd, track.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun PerformersSection(performers: List<Performer>, onAddPerformer: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionHeader(stringResource(R.string.detail_section_performers))
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onAddPerformer,
            modifier = Modifier
                .size(32.dp)
                .testTag("album_detail_add_performer"),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.cd_add_performer_to_album_action),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    if (performers.isEmpty()) {
        Text(
            text = stringResource(R.string.album_performers_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("album_detail_no_performers"),
        )
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(performers, key = { it.id }) { performer ->
                PerformerChip(performer)
            }
        }
    }
}

@Composable
private fun PerformerChip(performer: Performer) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 10.dp)
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
    }
}

@Composable
private fun CommentCard(comment: Comment, onRemove: (Comment) -> Unit) {
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
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = ratingDesc },
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
            IconButton(
                onClick = { onRemove(comment) },
                modifier = Modifier
                    .size(28.dp)
                    .testTag("album_remove_comment_${comment.id}"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.album_remove_comment_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            text = comment.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() },
    )
}

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
                text = stringResource(R.string.detail_not_found_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.detail_not_found_body),
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
