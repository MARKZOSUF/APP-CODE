package com.insangram.app.core.common

/**
 * Canonical loading / success / empty / error envelope for screen state.
 * Every ViewModel exposes its screen state through this type (or a data class
 * that embeds it) so the UI layer handles all four cases exactly once.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Success<out T>(val data: T, val refreshing: Boolean = false) : UiState<T>
    data class Error(val error: InsangramError, val cachedData: Any? = null) : UiState<Nothing>
}

/** Maps a list result to [UiState.Empty] when the list has no items. */
fun <T> List<T>.toUiState(refreshing: Boolean = false): UiState<List<T>> =
    if (isEmpty()) UiState.Empty else UiState.Success(this, refreshing)

fun <T> InsangramResult<List<T>>.toListUiState(): UiState<List<T>> = when (this) {
    is InsangramResult.Success -> data.toUiState()
    is InsangramResult.Failure -> UiState.Error(error)
}

/** One-shot UI signals (snackbars, navigation) that must not be re-emitted. */
sealed interface UiEvent {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val isError: Boolean = false,
    ) : UiEvent

    data class ShowError(val error: InsangramError) : UiEvent
    data class Navigate(val route: String) : UiEvent
    data object NavigateBack : UiEvent
}
