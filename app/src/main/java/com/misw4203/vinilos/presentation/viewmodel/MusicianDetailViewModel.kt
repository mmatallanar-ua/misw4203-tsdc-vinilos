package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.usecase.GetMusicianDetailUseCase
import com.misw4203.vinilos.presentation.common.DomainResult
import com.misw4203.vinilos.presentation.common.runCatchingDomain
import com.misw4203.vinilos.presentation.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MusicianDetailUiState {
    data object Loading : MusicianDetailUiState
    data class Success(val musician: Musician) : MusicianDetailUiState
    data object NotFound : MusicianDetailUiState
    data class Error(val isNetworkError: Boolean) : MusicianDetailUiState
}

@HiltViewModel
class MusicianDetailViewModel @Inject constructor(
    private val getMusicianDetail: GetMusicianDetailUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val musicianId: Int = checkNotNull(savedStateHandle[Destinations.ArtistDetailArg])

    private val _uiState = MutableStateFlow<MusicianDetailUiState>(MusicianDetailUiState.Loading)
    val uiState: StateFlow<MusicianDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun refresh() {
        load()
    }

    private fun load() {
        _uiState.value = MusicianDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val r = runCatchingDomain { getMusicianDetail(musicianId) }) {
                is DomainResult.Ok -> MusicianDetailUiState.Success(r.value)
                DomainResult.Network -> MusicianDetailUiState.Error(isNetworkError = true)
                DomainResult.NotFound -> MusicianDetailUiState.NotFound
                DomainResult.Server -> MusicianDetailUiState.Error(isNetworkError = false)
            }
        }
    }
}
