package com.misw4203.vinilos.presentation.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.MusicianSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MusicianRowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val sample = MusicianSummary(10, "Freddie Mercury", "", "1946-09-05")

    @Test
    fun rendersMusicianName() {
        composeTestRule.setContent {
            MaterialTheme { MusicianRow(musician = sample, isAdding = false, onAdd = {}) }
        }
        composeTestRule.onNodeWithText("Freddie Mercury").assertIsDisplayed()
    }

    @Test
    fun clickPlusInvokesOnAddWithMusicianId() {
        var addedId: Int? = null
        composeTestRule.setContent {
            MaterialTheme {
                MusicianRow(musician = sample, isAdding = false, onAdd = { addedId = it })
            }
        }
        val cd = composeTestRule.activity.getString(R.string.cd_add_musician_to_band, "Freddie Mercury")
        composeTestRule.onNodeWithContentDescription(cd).performClick()
        assertEquals(10, addedId)
    }

    @Test
    fun whenAddingShowsLoadingIndicatorAndDisablesButton() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                MusicianRow(musician = sample, isAdding = true, onAdd = { clicked = true })
            }
        }
        val cd = composeTestRule.activity.getString(R.string.cd_adding_musician)
        composeTestRule.onNodeWithContentDescription(cd).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(cd).performClick()
        assertEquals(false, clicked)
    }
}
