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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.Track
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.LoadingState
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
    viewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is AlbumDetailUiState.Loading -> LoadingState()
            is AlbumDetailUiState.Success -> AlbumDetailContent(album = state.album, onBack = onBack)
            is AlbumDetailUiState.NotFound -> NotFoundState(onBack = onBack)
            is AlbumDetailUiState.Error -> ErrorState(
                onRetry = viewModel::retry,
                isNetworkError = state.isNetworkError,
            )
        }
    }
}

@Composable
private fun AlbumDetailContent(album: AlbumDetail, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Cover image
            Box(modifier = Modifier.fillMaxWidth().height(CoverHeight)) {
                AsyncImage(
                    model = album.coverUrl,
                    contentDescription = stringResource(R.string.cd_album_cover),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Content card overlapping the image
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = -CardOverlap),
                shape = RoundedCornerShape(topStart = CardRadius, topEnd = CardRadius),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                    // Title + artist
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
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

                    // Performers
                    if (album.performers.isNotEmpty()) {
                        Spacer(Modifier.height(28.dp))
                        PerformersSection(performers = album.performers)
                    }

                    // Tracks
                    if (album.tracks.isNotEmpty()) {
                        Spacer(Modifier.height(28.dp))
                        TracksSection(tracks = album.tracks)
                    }

                    // Comments
                    if (album.comments.isNotEmpty()) {
                        Spacer(Modifier.height(28.dp))
                        CommentsSection(comments = album.comments)
                    }

                    // Record label
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

                    Spacer(Modifier.height(32.dp))
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
                .size(40.dp),
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
private fun TracksSection(tracks: List<Track>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionHeader(stringResource(R.string.detail_section_tracks))
        Text(
            text = stringResource(R.string.detail_tracks_total, tracks.size).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
    Spacer(Modifier.height(12.dp))
    tracks.forEachIndexed { index, track ->
        TrackRow(index = index + 1, track = track)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TrackRow(index: Int, track: Track) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
    }
}

@Composable
private fun PerformersSection(performers: List<Performer>) {
    SectionHeader(stringResource(R.string.detail_section_performers))
    Spacer(Modifier.height(12.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(performers, key = { it.id }) { performer ->
            PerformerChip(performer)
        }
    }
}

@Composable
private fun PerformerChip(performer: Performer) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
private fun CommentsSection(comments: List<Comment>) {
    SectionHeader(stringResource(R.string.detail_section_comments))
    Spacer(Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        comments.forEach { comment ->
            CommentCard(comment)
        }
    }
}

@Composable
private fun CommentCard(comment: Comment) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
