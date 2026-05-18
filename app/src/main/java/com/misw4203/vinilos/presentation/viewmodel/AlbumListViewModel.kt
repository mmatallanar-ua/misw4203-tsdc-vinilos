package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.GetAlbumsUseCase
import com.misw4203.vinilos.presentation.common.DomainResult
import com.misw4203.vinilos.presentation.common.runCatchingDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    fun refresh() {
        load()
    }

    private fun load() {
        _uiState.value = AlbumListUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val r = runCatchingDomain { getAlbums() }) {
                is DomainResult.Ok ->
                    if (r.value.isEmpty()) AlbumListUiState.Empty else AlbumListUiState.Success(r.value)
                DomainResult.Network -> AlbumListUiState.Error(isNetworkError = true)
                DomainResult.NotFound -> AlbumListUiState.Error(isNetworkError = false)
                DomainResult.Server -> AlbumListUiState.Error(isNetworkError = false)
            }
        }
    }
}
