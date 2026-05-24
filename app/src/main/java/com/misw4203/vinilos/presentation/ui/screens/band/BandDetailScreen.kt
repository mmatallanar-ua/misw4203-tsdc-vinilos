package com.misw4203.vinilos.presentation.ui.screens.band

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.presentation.ui.components.EmptyMembersState
import com.misw4203.vinilos.presentation.ui.components.ErrorState
import com.misw4203.vinilos.presentation.ui.components.LoadingState
import com.misw4203.vinilos.presentation.ui.components.MusicianCard
import com.misw4203.vinilos.presentation.ui.components.PerformerBadgeKind
import com.misw4203.vinilos.presentation.ui.components.PerformerHeader
import com.misw4203.vinilos.presentation.viewmodel.BandDetailUiState
import com.misw4203.vinilos.presentation.viewmodel.BandDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandDetailScreen(
    bandId: Int,
    onBack: () -> Unit,
    onMusicianClick: (Int) -> Unit,
    onAddMusicians: () -> Unit,
    modifier: Modifier = Modifier,
    onAddAlbum: () -> Unit = {},
    onAwardPrize: () -> Unit = {},
    refreshKey: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    viewModel: BandDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(bandId) { viewModel.loadBand(bandId) }
    LaunchedEffect(refreshKey) {
        if (refreshKey) {
            viewModel.retry()
            onRefreshHandled()
        }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.band_detail_title),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("band_detail_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is BandDetailUiState.Loading -> LoadingState()
                is BandDetailUiState.NotFound -> NotFoundContent()
                is BandDetailUiState.Error -> ErrorState(
                    onRetry = viewModel::retry,
                    isNetworkError = state.isNetworkError,
                )
                is BandDetailUiState.Success -> BandBody(
                    band = state.band,
                    onMusicianClick = onMusicianClick,
                    onAddMusicians = onAddMusicians,
                    onAddAlbum = onAddAlbum,
                    onAwardPrize = onAwardPrize,
                )
            }
        }
    }
}

@Composable
private fun BandBody(
    band: Band,
    onMusicianClick: (Int) -> Unit,
    onAddMusicians: () -> Unit,
    onAddAlbum: () -> Unit,
    onAwardPrize: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("band_detail_root"),
    ) {
        PerformerHeader(
            name = band.name,
            image = band.image,
            description = band.description,
            badgeKind = PerformerBadgeKind.Band,
            contentDescription = stringResource(R.string.cd_band_image_of, band.name),
        )
        Spacer(Modifier.height(8.dp))

        MembersSection(
            members = band.members,
            onMusicianClick = onMusicianClick,
            onAddMusicians = onAddMusicians,
        )

        Spacer(Modifier.height(24.dp))
        AlbumsSection(albums = band.albums, onAddAlbum = onAddAlbum)

        Spacer(Modifier.height(24.dp))
        PrizesSection(prizes = band.prizes, onAwardPrize = onAwardPrize)

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun MembersSection(
    members: List<MusicianSummary>,
    onMusicianClick: (Int) -> Unit,
    onAddMusicians: () -> Unit,
) {
    SectionHeader(stringResource(R.string.band_members_section_title))
    Spacer(Modifier.height(8.dp))
    if (members.isEmpty()) {
        EmptyMembersState(onAddFirst = onAddMusicians)
    } else {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            members.forEach { m ->
                MusicianCard(
                    musician = m,
                    onClick = { onMusicianClick(m.id) },
                    modifier = Modifier.testTag("current_member_${m.id}"),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onAddMusicians,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(48.dp)
                .testTag("band_detail_add_musicians"),
        ) {
            Icon(
                imageVector = Icons.Outlined.PersonAdd,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_musicians_cta))
        }
    }
}

@Composable
private fun AlbumsSection(albums: List<Album>, onAddAlbum: () -> Unit) {
    SectionHeader(stringResource(R.string.band_albums_section_title))
    Spacer(Modifier.height(8.dp))
    if (albums.isEmpty()) {
        Text(
            text = stringResource(R.string.band_albums_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(albums) { album ->
                Column(modifier = Modifier.width(120.dp)) {
                    AsyncImage(
                        model = album.coverUrl,
                        contentDescription = stringResource(R.string.cd_album_cover_of, album.name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (album.releaseYear.isNotBlank()) {
                        Text(
                            text = album.releaseYear,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onAddAlbum,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(48.dp)
            .testTag("band_detail_add_album"),
    ) {
        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.band_add_album_cta))
    }
}

@Composable
private fun PrizesSection(prizes: List<MusicianPrize>, onAwardPrize: () -> Unit) {
    SectionHeader(stringResource(R.string.band_prizes_section_title))
    Spacer(Modifier.height(8.dp))
    if (prizes.isEmpty()) {
        Text(
            text = stringResource(R.string.band_prizes_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .testTag("band_detail_no_prizes"),
        )
    } else {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            prizes.forEach { prize ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(12.dp)
                        .semantics(mergeDescendants = true) {}
                        .testTag("band_prize_${prize.id}"),
                    verticalAlignment = Alignment.CenterVertically,
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
                    if (prize.premiationDate.isNotBlank()) {
                        Text(
                            text = prize.premiationDate.take(10),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onAwardPrize,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(48.dp)
            .testTag("band_detail_award_prize"),
    ) {
        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.band_award_prize_cta))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun NotFoundContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.band_not_found_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.band_not_found_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
