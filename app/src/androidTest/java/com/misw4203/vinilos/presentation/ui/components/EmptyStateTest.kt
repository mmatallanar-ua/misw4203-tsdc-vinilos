package com.misw4203.vinilos.presentation.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.activity.ComponentActivity
import com.misw4203.vinilos.R
import org.junit.Rule
import org.junit.Test

class EmptyStateTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rendersEmptyTitleAndBody() {
        composeTestRule.setContent {
            MaterialTheme { EmptyState() }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_empty_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_empty_body)).assertIsDisplayed()
    }
}
