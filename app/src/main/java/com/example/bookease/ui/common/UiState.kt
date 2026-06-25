package com.example.bookease.ui.common

/**
 * A generic UI State interface to represent the 5 essential states of a production app.
 */
sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    object Empty : UiState<Nothing>
    object Offline : UiState<Nothing>
}
