package com.misw4203.vinilos.presentation.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.misw4203.vinilos.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EmptyMembersStateTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rendersEmptyTitleAndBody() {
        composeTestRule.setContent {
            MaterialTheme { EmptyMembersState(onAddFirst = {}) }
        }
        val title = composeTestRule.activity.getString(R.string.band_members_empty_title)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun clickCtaInvokesCallback() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme { EmptyMembersState(onAddFirst = { clicked = true }) }
        }
        val cta = composeTestRule.activity.getString(R.string.add_first_member_cta)
        composeTestRule.onNodeWithText(cta).performClick()
        assertTrue(clicked)
    }
}
