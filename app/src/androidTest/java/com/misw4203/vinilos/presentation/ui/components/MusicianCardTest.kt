package com.misw4203.vinilos.presentation.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.misw4203.vinilos.domain.model.MusicianSummary
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MusicianCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sample = MusicianSummary(
        id = 100,
        name = "Rubén Blades",
        image = "",
    )

    @Test
    fun rendersMusicianName() {
        composeTestRule.setContent {
            MaterialTheme {
                MusicianCard(musician = sample, onClick = {})
            }
        }

        composeTestRule.onNodeWithText("Rubén Blades").assertIsDisplayed()
    }

    @Test
    fun clickTriggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                MusicianCard(musician = sample, onClick = { clicked = true })
            }
        }

        composeTestRule.onNodeWithText("Rubén Blades").performClick()
        assertTrue(clicked)
    }
}
