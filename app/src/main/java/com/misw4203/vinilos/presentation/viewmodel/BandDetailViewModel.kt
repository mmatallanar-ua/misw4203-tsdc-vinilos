package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
import com.misw4203.vinilos.presentation.common.DomainResult
import com.misw4203.vinilos.presentation.common.runCatchingDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BandDetailViewModel @Inject constructor(
    private val getBandDetail: GetBandDetailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BandDetailUiState>(BandDetailUiState.Loading)
    val uiState: StateFlow<BandDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currentId: Int? = null

    fun loadBand(id: Int) {
        currentId = id
        loadJob?.cancel()
        _uiState.value = BandDetailUiState.Loading
        loadJob = viewModelScope.launch {
            _uiState.value = when (val r = runCatchingDomain { getBandDetail(id) }) {
                is DomainResult.Ok -> BandDetailUiState.Success(r.value)
                DomainResult.Network -> BandDetailUiState.Error(isNetworkError = true)
                DomainResult.NotFound -> BandDetailUiState.NotFound
                DomainResult.Server -> BandDetailUiState.Error(isNetworkError = false)
            }
        }
    }

    fun retry() {
        currentId?.let { loadBand(it) }
    }
}
