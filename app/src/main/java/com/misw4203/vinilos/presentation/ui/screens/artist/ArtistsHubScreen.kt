package com.misw4203.vinilos.presentation.ui.screens.artist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.misw4203.vinilos.R
import com.misw4203.vinilos.presentation.ui.components.VinilosTopBar
import com.misw4203.vinilos.presentation.ui.screens.band.BandListContent

@Composable
fun ArtistsHubScreen(
    onMusicianClick: (Int) -> Unit,
    onBandClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        VinilosTopBar(title = stringResource(R.string.artists_title))
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.testTag("artists_tab_musicians"),
                text = { Text(stringResource(R.string.artists_tab_musicians)) },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.testTag("artists_tab_bands"),
                text = { Text(stringResource(R.string.artists_tab_bands)) },
            )
        }
        when (selectedTab) {
            0 -> MusicianListContent(onMusicianClick = onMusicianClick)
            1 -> BandListContent(onBandClick = onBandClick)
        }
    }
}
