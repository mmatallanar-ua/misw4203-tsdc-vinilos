package com.misw4203.vinilos.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.data.remote.dto.CreateTrackRequest
import com.misw4203.vinilos.domain.usecase.AddTrackUseCase
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
class AddTrackViewModel @Inject constructor(
    private val addTrack: AddTrackUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val albumId: Long = checkNotNull(savedStateHandle[Destinations.AddTrackAlbumArg])

    var name by mutableStateOf("")
    var nameError by mutableStateOf<String?>(null)

    var duration by mutableStateOf("")
    var durationError by mutableStateOf<String?>(null)

    private val _uiState = MutableStateFlow<AddTrackUiState>(AddTrackUiState.Idle)
    val uiState: StateFlow<AddTrackUiState> = _uiState.asStateFlow()

    fun submit() {
        nameError = if (name.isBlank()) "El nombre del track es obligatorio" else null
        durationError = if (!isValidDuration(duration)) "Formato: MM:SS (ej: 03:45)" else null

        if (nameError != null || durationError != null) return

        _uiState.value = AddTrackUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val track = addTrack(albumId, CreateTrackRequest(name.trim(), duration.trim()))
                AddTrackUiState.Success(track)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                val message = if (e.code() == 404) "Álbum no encontrado"
                              else "Error al agregar track"
                AddTrackUiState.Error(message, isNetworkError = false)
            } catch (e: IOException) {
                AddTrackUiState.Error("Sin conexión. Intenta de nuevo", isNetworkError = true)
            } catch (e: Exception) {
                AddTrackUiState.Error("Error al agregar track", isNetworkError = false)
            }
        }
    }

    internal fun isValidDuration(value: String): Boolean {
        val match = Regex("""^(\d{1,2}):(\d{2})$""").matchEntire(value) ?: return false
        return match.groupValues[2].toInt() < 60
    }
}
