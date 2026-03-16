package org.kalpeshbkundanani.burnmate.presentation.shared

sealed interface LoadableUiState {
    data object Loading : LoadableUiState
    data object Content : LoadableUiState
    data object Empty : LoadableUiState
    data object Error : LoadableUiState
}
