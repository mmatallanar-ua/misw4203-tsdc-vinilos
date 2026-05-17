package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            _uiState.value = try {
                val prizes = getPrizes()
                if (prizes.isEmpty()) PrizesUiState.Empty else PrizesUiState.Success(prizes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                PrizesUiState.Error(isNetworkError = true)
            } catch (e: Exception) {
                PrizesUiState.Error(isNetworkError = false)
            }
        }
    }
}
