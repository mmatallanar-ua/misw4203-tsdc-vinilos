package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.usecase.GetMusicianDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MusicianDetailUiState {
    object Loading : MusicianDetailUiState()
    data class Success(val musician: Musician) : MusicianDetailUiState()
    data class Error(val message: String) : MusicianDetailUiState()
}

@HiltViewModel
class MusicianDetailViewModel @Inject constructor(
    private val getMusicianDetail: GetMusicianDetailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MusicianDetailUiState>(MusicianDetailUiState.Loading)
    val uiState: StateFlow<MusicianDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadMusician(id: Int) {
        loadJob?.cancel()
        _uiState.value = MusicianDetailUiState.Loading
        loadJob = viewModelScope.launch {
            _uiState.value = try {
                MusicianDetailUiState.Success(getMusicianDetail(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MusicianDetailUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}
