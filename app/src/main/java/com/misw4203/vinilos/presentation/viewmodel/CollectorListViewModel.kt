package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.GetCollectorsUseCase
import com.misw4203.vinilos.presentation.common.DomainResult
import com.misw4203.vinilos.presentation.common.runCatchingDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectorListViewModel @Inject constructor(
    private val getCollectors: GetCollectorsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CollectorListUiState>(CollectorListUiState.Loading)
    val uiState: StateFlow<CollectorListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        _uiState.value = CollectorListUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val r = runCatchingDomain { getCollectors() }) {
                is DomainResult.Ok ->
                    if (r.value.isEmpty()) CollectorListUiState.Empty else CollectorListUiState.Success(r.value)
                DomainResult.Network -> CollectorListUiState.Error(isNetworkError = true)
                DomainResult.NotFound -> CollectorListUiState.Error(isNetworkError = false)
                DomainResult.Server -> CollectorListUiState.Error(isNetworkError = false)
            }
        }
    }
}
