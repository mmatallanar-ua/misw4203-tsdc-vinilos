package com.misw4203.vinilos.presentation.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.misw4203.vinilos.domain.model.BandSummary
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BandCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sample = BandSummary(id = 1, name = "Queen", image = "")

    @Test
    fun rendersBandName() {
        composeTestRule.setContent {
            MaterialTheme { BandCard(band = sample, onClick = {}) }
        }
        composeTestRule.onNodeWithText("Queen").assertIsDisplayed()
    }

    @Test
    fun rendersBandBadge() {
        composeTestRule.setContent {
            MaterialTheme { BandCard(band = sample, onClick = {}) }
        }
        composeTestRule.onNodeWithText("BANDA").assertIsDisplayed()
    }

    @Test
    fun clickTriggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme { BandCard(band = sample, onClick = { clicked = true }) }
        }
        composeTestRule.onNodeWithText("Queen").performClick()
        assertTrue(clicked)
    }
}
