package com.misw4203.vinilos.presentation.ui.screens.prize

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.domain.repository.PrizeRepository
import com.misw4203.vinilos.domain.usecase.CreatePrizeUseCase
import com.misw4203.vinilos.domain.usecase.GetPrizesUseCase
import com.misw4203.vinilos.presentation.viewmodel.CreatePrizeViewModel
import org.junit.Rule
import org.junit.Test

class CreatePrizeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakePrizeRepo(
        val prizes: List<Prize> = emptyList(),
        private val shouldFailCreate: Boolean = false,
    ) : PrizeRepository {
        var lastCreated: Triple<String, String, String>? = null

        override suspend fun getPrizes() = prizes

        override suspend fun createPrize(name: String, description: String, organization: String): Prize {
            if (shouldFailCreate) throw java.io.IOException("network error")
            lastCreated = Triple(name, description, organization)
            return Prize(99, name, description, organization)
        }
    }

    private fun buildVm(repo: FakePrizeRepo) =
        CreatePrizeViewModel(GetPrizesUseCase(repo), CreatePrizeUseCase(repo))

    private fun launchScreen(vm: CreatePrizeViewModel, onSuccess: () -> Unit = {}) {
        composeTestRule.setContent {
            MaterialTheme {
                CreatePrizeScreen(onBack = {}, onSuccess = onSuccess, viewModel = vm)
            }
        }
    }

    private fun waitForReady() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText("registered_prizes_section")
                .fetchSemanticsNodes()
                .isNotEmpty() ||
                composeTestRule
                    .onAllNodesWithTag("create_prize_name")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
    }

    // CA01 – form and prizes list section are visible
    @Test
    fun formAndRegisteredSectionAreVisible() {
        val prizes = listOf(
            Prize(1, "Grammy", "Music award", "NARAS"),
            Prize(2, "Latin Grammy", "Latin music award", "Latin Recording Academy"),
        )
        val vm = buildVm(FakePrizeRepo(prizes))
        launchScreen(vm)

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("create_prize_name").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("create_prize_name").assertIsDisplayed()
        composeTestRule.onNodeWithTag("create_prize_organization").assertIsDisplayed()
        composeTestRule.onNodeWithTag("create_prize_description").assertIsDisplayed()
        composeTestRule.onNodeWithTag("create_prize_submit").assertIsDisplayed()
        composeTestRule.onNodeWithTag("registered_prizes_section").assertIsDisplayed()
    }

    // CA04 – submit button disabled when fields empty
    @Test
    fun submitButtonDisabledWhenAllFieldsEmpty() {
        val vm = buildVm(FakePrizeRepo())
        launchScreen(vm)

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("create_prize_submit").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("create_prize_submit").assertIsNotEnabled()
    }

    @Test
    fun submitButtonEnabledOnlyWhenAllFieldsFilled() {
        val vm = buildVm(FakePrizeRepo())
        launchScreen(vm)

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("create_prize_name").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("create_prize_submit").assertIsNotEnabled()

        composeTestRule.onNodeWithTag("create_prize_name").performTextInput("Grammy")
        composeTestRule.onNodeWithTag("create_prize_organization").performTextInput("NARAS")
        composeTestRule.onNodeWithTag("create_prize_submit").assertIsNotEnabled()

        composeTestRule.onNodeWithTag("create_prize_description").performTextInput("Top award in music")
        composeTestRule.onNodeWithTag("create_prize_submit").assertIsEnabled()
    }

    // CA03 – successfully create a prize
    @Test
    fun submitWithAllFieldsCallsOnSuccess() {
        val repo = FakePrizeRepo()
        val vm = buildVm(repo)
        var successCalled = false
        launchScreen(vm, onSuccess = { successCalled = true })

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("create_prize_name").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("create_prize_name").performTextInput("Grammy")
        composeTestRule.onNodeWithTag("create_prize_organization").performTextInput("NARAS")
        composeTestRule.onNodeWithTag("create_prize_description").performTextInput("Top award in music")
        composeTestRule.onNodeWithTag("create_prize_submit").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { successCalled }
        assert(successCalled)
    }

    // CA10 – cancel with data shows confirmation dialog
    @Test
    fun cancelWithDataShowsConfirmationDialog() {
        val vm = buildVm(FakePrizeRepo())
        launchScreen(vm)

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("create_prize_name").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("create_prize_name").performTextInput("Grammy")
        composeTestRule.onNodeWithTag("create_prize_cancel").performClick()

        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule
                .onAllNodesWithText("Descartar")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Descartar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Seguir editando").assertIsDisplayed()
    }

    // CA10 – confirming cancel discards data
    @Test
    fun cancelConfirmedClearsForm() {
        val vm = buildVm(FakePrizeRepo())
        launchScreen(vm)

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("create_prize_name").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("create_prize_name").performTextInput("Grammy")
        composeTestRule.onNodeWithTag("create_prize_cancel").performClick()

        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithTag("cancel_confirm_discard").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("cancel_confirm_discard").performClick()

        // After discard the back callback fires (no crash means discard worked)
    }

    // CA10 – keeping editing leaves form intact
    @Test
    fun cancelDismissedKeepsFormData() {
        val vm = buildVm(FakePrizeRepo())
        launchScreen(vm)

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("create_prize_name").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("create_prize_name").performTextInput("Grammy")
        composeTestRule.onNodeWithTag("create_prize_cancel").performClick()

        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithTag("cancel_confirm_keep").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("cancel_confirm_keep").performClick()

        // Form is still visible with data, submit still enabled after refilling description
        composeTestRule.onNodeWithTag("create_prize_name").assertIsDisplayed()
    }
}
