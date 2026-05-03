package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.CreateAlbumInput
import com.misw4203.vinilos.domain.usecase.CreateAlbumUseCase
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
class CreateAlbumViewModel @Inject constructor(
    private val createAlbum: CreateAlbumUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateAlbumUiState>(CreateAlbumUiState.Idle)
    val uiState: StateFlow<CreateAlbumUiState> = _uiState.asStateFlow()

    fun submit(input: CreateAlbumInput) {
        _uiState.value = CreateAlbumUiState.Submitting
        viewModelScope.launch {
            _uiState.value = try {
                createAlbum(input)
                CreateAlbumUiState.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                CreateAlbumUiState.Error("Sin conexión a internet. Intenta de nuevo.")
            } catch (e: HttpException) {
                CreateAlbumUiState.Error("Error del servidor (${e.code()}). Verifica los datos.")
            } catch (e: Exception) {
                CreateAlbumUiState.Error("Ocurrió un error inesperado.")
            }
        }
    }

    fun resetState() {
        _uiState.value = CreateAlbumUiState.Idle
    }
}
