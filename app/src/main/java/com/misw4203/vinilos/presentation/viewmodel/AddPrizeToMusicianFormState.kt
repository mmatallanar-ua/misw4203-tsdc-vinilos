package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.Prize

enum class YearValidationError { EMPTY, INVALID, FUTURE, DUPLICATE }

data class AddPrizeToMusicianFormState(
    val query: String = "",
    val allPrizes: List<Prize> = emptyList(),
    val filteredPrizes: List<Prize> = emptyList(),
    val selectedPrize: Prize? = null,
    val year: String = "",
    val yearError: YearValidationError? = null,
    val currentPrizes: List<MusicianPrize> = emptyList(),
    val musicianName: String = "",
    val musicianImage: String = "",
)

val AddPrizeToMusicianFormState.isFormReady: Boolean
    get() = selectedPrize != null && year.isNotBlank() && yearError == null
