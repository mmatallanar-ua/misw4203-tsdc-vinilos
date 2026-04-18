package com.misw4203.vinilos.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.misw4203.vinilos.R

enum class VinilosDestination { Albums, Artists, Collectors }

@Composable
fun VinilosBottomNav(
    selected: VinilosDestination,
    onSelect: (VinilosDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        NavTab(
            label = stringResource(R.string.nav_albums),
            icon = if (selected == VinilosDestination.Albums) Icons.Filled.Album else Icons.Outlined.Album,
            active = selected == VinilosDestination.Albums,
            onClick = { onSelect(VinilosDestination.Albums) },
        )
        NavTab(
            label = stringResource(R.string.nav_artists),
            icon = Icons.Outlined.PersonSearch,
            active = selected == VinilosDestination.Artists,
            onClick = { onSelect(VinilosDestination.Artists) },
        )
        NavTab(
            label = stringResource(R.string.nav_collectors),
            icon = Icons.Outlined.Group,
            active = selected == VinilosDestination.Collectors,
            onClick = { onSelect(VinilosDestination.Collectors) },
        )
    }
}

@Composable
private fun NavTab(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (active) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.size(4.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.15.em,
            ),
            color = tint,
        )
    }
}
