package com.misw4203.vinilos.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.misw4203.vinilos.R

private val MinTouchTarget = 48.dp

@Composable
fun RatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 36.dp,
    interactive: Boolean = true,
) {
    val rowModifier = if (interactive) {
        modifier
    } else {
        val summary = stringResource(R.string.cd_rating_summary, rating)
        modifier.clearAndSetSemantics { contentDescription = summary }
    }
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (1..5).forEach { star ->
            val isFilled = star <= rating
            if (interactive) {
                val actionDesc = stringResource(R.string.cd_rate_with_stars, star)
                val stateDesc = stringResource(
                    if (isFilled) R.string.cd_state_selected else R.string.cd_state_not_selected
                )
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = MinTouchTarget, minHeight = MinTouchTarget)
                        .clickable { onRatingChange(star) }
                        .semantics {
                            role = Role.Button
                            contentDescription = actionDesc
                            stateDescription = stateDesc
                        }
                        .testTag("rating_star_$star"),
                    contentAlignment = Alignment.Center,
                ) {
                    StarIcon(isFilled = isFilled, starSize = starSize)
                }
            } else {
                StarIcon(
                    isFilled = isFilled,
                    starSize = starSize,
                    modifier = Modifier.testTag("rating_star_$star"),
                )
            }
        }
    }
}

@Composable
private fun StarIcon(
    isFilled: Boolean,
    starSize: Dp,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarOutline,
        contentDescription = null,
        tint = if (isFilled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        modifier = modifier.size(starSize),
    )
}
