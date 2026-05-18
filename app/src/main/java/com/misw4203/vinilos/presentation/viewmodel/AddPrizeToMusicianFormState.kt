package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.Prize

enum class DateValidationError { FUTURE, DUPLICATE }

data class AddPrizeToMusicianFormState(
    val query: String = "",
    val allPrizes: List<Prize> = emptyList(),
    val filteredPrizes: List<Prize> = emptyList(),
    val selectedPrize: Prize? = null,
    val premiationDate: String? = null,
    val dateError: DateValidationError? = null,
    val currentPrizes: List<MusicianPrize> = emptyList(),
    val musicianName: String = "",
    val musicianImage: String = "",
)

val AddPrizeToMusicianFormState.isFormReady: Boolean
    get() = selectedPrize != null && premiationDate != null && dateError == null
