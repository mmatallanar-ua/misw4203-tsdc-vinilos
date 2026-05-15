package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
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
            _uiState.value = try {
                BandDetailUiState.Success(getBandDetail(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() == 404) BandDetailUiState.NotFound
                else BandDetailUiState.Error(isNetworkError = false)
            } catch (e: IOException) {
                BandDetailUiState.Error(isNetworkError = true)
            } catch (e: Exception) {
                BandDetailUiState.Error(isNetworkError = false)
            }
        }
    }

    fun retry() {
        currentId?.let { loadBand(it) }
    }
}
