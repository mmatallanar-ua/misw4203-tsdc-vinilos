package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.GetCollectorsUseCase
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
            _uiState.value = try {
                val collectors = getCollectors()
                if (collectors.isEmpty()) CollectorListUiState.Empty
                else CollectorListUiState.Success(collectors)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                CollectorListUiState.Error(isNetworkError = true)
            } catch (e: HttpException) {
                CollectorListUiState.Error(isNetworkError = false)
            } catch (e: Exception) {
                CollectorListUiState.Error(isNetworkError = false)
            }
        }
    }
}
