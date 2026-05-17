package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.CreatePrizeUseCase
import com.misw4203.vinilos.domain.usecase.GetPrizesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class CreatePrizeViewModel @Inject constructor(
    private val getPrizes: GetPrizesUseCase,
    private val createPrize: CreatePrizeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreatePrizeUiState>(CreatePrizeUiState.LoadingPrizes)
    val uiState: StateFlow<CreatePrizeUiState> = _uiState.asStateFlow()

    init {
        loadPrizes()
    }

    fun loadPrizes() {
        _uiState.value = CreatePrizeUiState.LoadingPrizes
        viewModelScope.launch {
            _uiState.value = try {
                CreatePrizeUiState.Ready(getPrizes())
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                CreatePrizeUiState.Ready(emptyList())
            } catch (e: Exception) {
                CreatePrizeUiState.Ready(emptyList())
            }
        }
    }

    fun submit(name: String, description: String, organization: String) {
        val current = _uiState.value
        val existingPrizes = if (current is CreatePrizeUiState.Ready) current.existingPrizes else emptyList()

        if (isDuplicate(name, organization, existingPrizes)) {
            _uiState.value = CreatePrizeUiState.Error("Ya existe un premio con ese nombre y organización.")
            return
        }

        _uiState.value = CreatePrizeUiState.Submitting
        viewModelScope.launch {
            _uiState.value = try {
                createPrize(name.trim(), description.trim(), organization.trim())
                CreatePrizeUiState.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                CreatePrizeUiState.Error("Sin conexión a internet. Inténtalo de nuevo.")
            } catch (e: HttpException) {
                CreatePrizeUiState.Error("Error del servidor (${e.code()}). Verifica los datos.")
            } catch (e: Exception) {
                CreatePrizeUiState.Error("Ocurrió un error inesperado.")
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
