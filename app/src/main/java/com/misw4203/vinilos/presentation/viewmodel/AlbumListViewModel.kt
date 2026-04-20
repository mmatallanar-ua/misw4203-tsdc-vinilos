package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.GetAlbumsUseCase
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
class AlbumListViewModel @Inject constructor(
    private val getAlbums: GetAlbumsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumListUiState>(AlbumListUiState.Loading)
    val uiState: StateFlow<AlbumListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        _uiState.value = AlbumListUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val albums = getAlbums()
                if (albums.isEmpty()) AlbumListUiState.Empty
                else AlbumListUiState.Success(albums)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                AlbumListUiState.Error(isNetworkError = true)
            } catch (e: HttpException) {
                AlbumListUiState.Error(isNetworkError = false)
            } catch (e: Exception) {
                AlbumListUiState.Error(isNetworkError = false)
            }
        }
    }
}
