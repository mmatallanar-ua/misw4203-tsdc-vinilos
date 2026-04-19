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

class ErrorStateTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rendersNetworkErrorBodyWhenIsNetworkErrorTrue() {
        composeTestRule.setContent {
            MaterialTheme { ErrorState(onRetry = {}, isNetworkError = true) }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_error_body_network))
            .assertIsDisplayed()
    }

    @Test
    fun rendersServerErrorBodyWhenIsNetworkErrorFalse() {
        composeTestRule.setContent {
            MaterialTheme { ErrorState(onRetry = {}, isNetworkError = false) }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_error_body_server))
            .assertIsDisplayed()
    }

    @Test
    fun retryButtonTriggersCallback() {
        var retried = false
        composeTestRule.setContent {
            MaterialTheme { ErrorState(onRetry = { retried = true }) }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.action_retry)).performClick()
        assertTrue(retried)
    }
}
