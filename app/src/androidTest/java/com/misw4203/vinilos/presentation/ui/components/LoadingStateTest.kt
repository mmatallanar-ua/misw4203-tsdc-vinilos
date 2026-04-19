package com.misw4203.vinilos.presentation.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.misw4203.vinilos.R
import org.junit.Rule
import org.junit.Test

class LoadingStateTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rendersLoadingText() {
        composeTestRule.setContent {
            MaterialTheme { LoadingState() }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_loading)).assertIsDisplayed()
    }
}
