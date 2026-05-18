package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
import com.misw4203.vinilos.presentation.common.DomainResult
import com.misw4203.vinilos.presentation.common.runCatchingDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicianListViewModel @Inject constructor(
    private val getMusicians: GetMusiciansUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MusicianListUiState>(MusicianListUiState.Loading)
    val uiState: StateFlow<MusicianListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        _uiState.value = MusicianListUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val r = runCatchingDomain { getMusicians() }) {
                is DomainResult.Ok ->
                    if (r.value.isEmpty()) MusicianListUiState.Empty else MusicianListUiState.Success(r.value)
                DomainResult.Network -> MusicianListUiState.Error(isNetworkError = true)
                // Una lista no devuelve 404; un Http inesperado = error de servidor.
                DomainResult.NotFound -> MusicianListUiState.Error(isNetworkError = false)
                DomainResult.Server -> MusicianListUiState.Error(isNetworkError = false)
            }
        }
    }
}
