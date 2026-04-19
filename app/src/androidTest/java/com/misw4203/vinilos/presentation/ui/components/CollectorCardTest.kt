package com.misw4203.vinilos.presentation.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.misw4203.vinilos.domain.model.CollectorSummary
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CollectorCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sample = CollectorSummary(
        id = 100,
        name = "Manolo Bellon",
        email = "manollo@caracol.com.co",
        telephone = "3502457896",
    )

    @Test
    fun rendersNameAndEmail() {
        composeTestRule.setContent {
            MaterialTheme {
                CollectorCard(collector = sample, onClick = {})
            }
        }

        composeTestRule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
        composeTestRule.onNodeWithText("manollo@caracol.com.co").assertIsDisplayed()
    }

    @Test
    fun clickTriggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                CollectorCard(collector = sample, onClick = { clicked = true })
            }
        }

        composeTestRule.onNodeWithText("Manolo Bellon").performClick()
        assertTrue(clicked)
    }
}
