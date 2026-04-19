package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.usecase.GetMusicianDetailUseCase
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

sealed interface MusicianDetailUiState {
    data object Loading : MusicianDetailUiState
    data class Success(val musician: Musician) : MusicianDetailUiState
    data object NotFound : MusicianDetailUiState
    data class Error(val isNetworkError: Boolean) : MusicianDetailUiState
}

@HiltViewModel
class MusicianDetailViewModel @Inject constructor(
    private val getMusicianDetail: GetMusicianDetailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MusicianDetailUiState>(MusicianDetailUiState.Loading)
    val uiState: StateFlow<MusicianDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currentId: Int? = null

    fun loadMusician(id: Int) {
        currentId = id
        loadJob?.cancel()
        _uiState.value = MusicianDetailUiState.Loading
        loadJob = viewModelScope.launch {
            _uiState.value = try {
                MusicianDetailUiState.Success(getMusicianDetail(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() == 404) MusicianDetailUiState.NotFound
                else MusicianDetailUiState.Error(isNetworkError = false)
            } catch (e: IOException) {
                MusicianDetailUiState.Error(isNetworkError = true)
            } catch (e: Exception) {
                MusicianDetailUiState.Error(isNetworkError = false)
            }
        }
    }

    fun retry() {
        currentId?.let { loadMusician(it) }
    }
}
