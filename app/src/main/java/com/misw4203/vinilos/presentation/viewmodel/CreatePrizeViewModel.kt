package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.domain.usecase.CreatePrizeUseCase
import com.misw4203.vinilos.domain.usecase.GetPrizesUseCase
import com.misw4203.vinilos.presentation.common.DomainFailure
import com.misw4203.vinilos.presentation.common.DomainResult
import com.misw4203.vinilos.presentation.common.runCatchingDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePrizeViewModel @Inject constructor(
    private val getPrizes: GetPrizesUseCase,
    private val createPrize: CreatePrizeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreatePrizeUiState>(CreatePrizeUiState.LoadingPrizes)
    val uiState: StateFlow<CreatePrizeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CreatePrizeEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CreatePrizeEvent> = _events.asSharedFlow()

    init {
        loadPrizes()
    }

    fun loadPrizes() {
        _uiState.value = CreatePrizeUiState.LoadingPrizes
        viewModelScope.launch {
            // fallo silencioso: la lista de premios es opcional para el formulario
            val r = runCatchingDomain { getPrizes() }
            _uiState.value = when (r) {
                is DomainResult.Ok -> CreatePrizeUiState.Ready(r.value)
                DomainResult.Network -> CreatePrizeUiState.Ready(emptyList())
                DomainResult.NotFound -> CreatePrizeUiState.Ready(emptyList())
                DomainResult.Server -> CreatePrizeUiState.Ready(emptyList())
            }
        }
    }

    fun submit(name: String, description: String, organization: String) {
        if (name.isBlank() || organization.isBlank() || description.isBlank()) {
            _events.tryEmit(CreatePrizeEvent.ValidationFailed(PrizeValidation.BLANK_FIELDS))
            return
        }

        val current = _uiState.value
        val existingPrizes = if (current is CreatePrizeUiState.Ready) current.existingPrizes else emptyList()

        if (isDuplicate(name, organization, existingPrizes)) {
            _events.tryEmit(CreatePrizeEvent.ValidationFailed(PrizeValidation.DUPLICATE))
            return
        }

        _uiState.value = CreatePrizeUiState.Submitting
        viewModelScope.launch {
            when (runCatchingDomain { createPrize(name.trim(), description.trim(), organization.trim()) }) {
                is DomainResult.Ok -> {
                    _uiState.value = CreatePrizeUiState.Ready(existingPrizes)
                    _events.tryEmit(CreatePrizeEvent.Submitted)
                }
                DomainResult.Network -> _uiState.value = CreatePrizeUiState.Error(DomainFailure.NETWORK)
                DomainResult.NotFound -> _uiState.value = CreatePrizeUiState.Error(DomainFailure.NOT_FOUND)
                DomainResult.Server -> _uiState.value = CreatePrizeUiState.Error(DomainFailure.SERVER)
            }
        }
    }

    fun resetState() {
        loadPrizes()
    }

    private fun isDuplicate(name: String, organization: String, prizes: List<Prize>): Boolean =
        prizes.any {
            it.name.trim().equals(name.trim(), ignoreCase = true) &&
                it.organization.trim().equals(organization.trim(), ignoreCase = true)
        }
}
