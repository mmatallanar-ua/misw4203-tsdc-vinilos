package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.GetPrizesUseCase
import com.misw4203.vinilos.presentation.common.DomainResult
import com.misw4203.vinilos.presentation.common.runCatchingDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrizesViewModel @Inject constructor(
    private val getPrizes: GetPrizesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PrizesUiState>(PrizesUiState.Loading)
    val uiState: StateFlow<PrizesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun refresh() = load()

    private fun load() {
        _uiState.value = PrizesUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val r = runCatchingDomain { getPrizes() }) {
                is DomainResult.Ok ->
                    if (r.value.isEmpty()) PrizesUiState.Empty else PrizesUiState.Success(r.value)
                DomainResult.Network -> PrizesUiState.Error(isNetworkError = true)
                DomainResult.NotFound -> PrizesUiState.Error(isNetworkError = false)
                DomainResult.Server -> PrizesUiState.Error(isNetworkError = false)
            }
        }
    }
}
