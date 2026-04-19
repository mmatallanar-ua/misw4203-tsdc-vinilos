package com.misw4203.vinilos.presentation.ui.screens.artist

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
import com.misw4203.vinilos.presentation.viewmodel.MusicianListViewModel
import org.junit.Rule
import org.junit.Test

class MusicianListScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    
}
