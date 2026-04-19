package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.GetCollectorDetailUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
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
class CollectorDetailViewModel @Inject constructor(
    private val getCollectorDetail: GetCollectorDetailUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val collectorId: Int = checkNotNull(savedStateHandle[Destinations.CollectorDetailArg])

    private val _uiState = MutableStateFlow<CollectorDetailUiState>(CollectorDetailUiState.Loading)
    val uiState: StateFlow<CollectorDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        _uiState.value = CollectorDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                CollectorDetailUiState.Success(getCollectorDetail(collectorId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() == 404) CollectorDetailUiState.NotFound
                else CollectorDetailUiState.Error(isNetworkError = false)
            } catch (e: IOException) {
                CollectorDetailUiState.Error(isNetworkError = true)
            } catch (e: Exception) {
                CollectorDetailUiState.Error(isNetworkError = false)
            }
        }
    }
}
