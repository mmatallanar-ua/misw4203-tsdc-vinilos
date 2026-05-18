package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.GetBandsUseCase
import com.misw4203.vinilos.presentation.common.DomainResult
import com.misw4203.vinilos.presentation.common.runCatchingDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BandListViewModel @Inject constructor(
    private val getBands: GetBandsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BandListUiState>(BandListUiState.Loading)
    val uiState: StateFlow<BandListUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() { load() }

    private fun load() {
        _uiState.value = BandListUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val r = runCatchingDomain { getBands() }) {
                is DomainResult.Ok ->
                    if (r.value.isEmpty()) BandListUiState.Empty else BandListUiState.Success(r.value)
                DomainResult.Network -> BandListUiState.Error(isNetworkError = true)
                DomainResult.NotFound -> BandListUiState.Error(isNetworkError = false)
                DomainResult.Server -> BandListUiState.Error(isNetworkError = false)
            }
        }
    }
}
