package com.misw4203.vinilos.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.misw4203.vinilos.R

@Composable
fun RatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 36.dp,
    interactive: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (1..5).forEach { star ->
            val isFilled = star <= rating
            val cd = stringResource(R.string.cd_rating, star)
            val starModifier = Modifier
                .size(starSize)
                .testTag("rating_star_$star")
                .let {
                    if (interactive) it.clickable { onRatingChange(star) } else it
                }
            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = cd,
                tint = if (isFilled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                modifier = starModifier,
            )
        }
    }
}
