package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
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
class MusicianListViewModel @Inject constructor(
    private val getMusicians: GetMusiciansUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MusicianListUiState>(MusicianListUiState.Loading)
    val uiState: StateFlow<MusicianListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        _uiState.value = MusicianListUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val musicians = getMusicians()
                if (musicians.isEmpty()) MusicianListUiState.Empty
                else MusicianListUiState.Success(musicians)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                MusicianListUiState.Error(isNetworkError = true)
            } catch (e: HttpException) {
                MusicianListUiState.Error(isNetworkError = false)
            } catch (e: Exception) {
                MusicianListUiState.Error(isNetworkError = false)
            }
        }
    }
}
