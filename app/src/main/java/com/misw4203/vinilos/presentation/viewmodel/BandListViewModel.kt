package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.GetBandsUseCase
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
            _uiState.value = try {
                val bands = getBands()
                if (bands.isEmpty()) BandListUiState.Empty
                else BandListUiState.Success(bands)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                BandListUiState.Error(isNetworkError = true)
            } catch (e: HttpException) {
                BandListUiState.Error(isNetworkError = false)
            } catch (e: Exception) {
                BandListUiState.Error(isNetworkError = false)
            }
        }
    }
}
