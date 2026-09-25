package com.insangram.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.domain.model.SessionState
import com.insangram.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Screen state shared by the login and sign-up forms.
 *
 * [signedIn] is a one-shot success signal the composable consumes to navigate;
 * [error] carries the user-facing copy from `InsangramError.userMessage`.
 */
/**
 * Server/unknown failures also carry the underlying reason so a failing
 * sign-up is diagnosable on a real device instead of showing only a generic
 * "something went wrong".
 */
private fun InsangramError.formMessage(): String = when (this) {
    is InsangramError.RemoteFailure, is InsangramError.Unknown -> {
        val detail = cause?.message?.takeIf { it.isNotBlank() }
        if (detail != null) userMessage + "\n" + detail else userMessage
    }
    else -> userMessage
}

data class AuthUiState(
    val submitting: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val signedIn: Boolean = false,
)

/** Backs the login form, the sign-up form and the forgot-password action. */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (_uiState.value.submitting) return
        _uiState.value = AuthUiState(submitting = true)
        viewModelScope.launch {
            when (val result = authRepository.signIn(email, password)) {
                is InsangramResult.Success ->
                    _uiState.value = AuthUiState(signedIn = true)
                is InsangramResult.Failure ->
                    _uiState.value = AuthUiState(error = result.error.formMessage())
            }
        }
    }

    fun register(
        fullName: String,
        username: String,
        email: String,
        password: String,
    ) {
        if (_uiState.value.submitting) return
        _uiState.value = AuthUiState(submitting = true)
        viewModelScope.launch {
            val result = authRepository.register(
                fullName = fullName,
                username = username,
                email = email,
                password = password,
                // The mock-up sign-up form collects no birthday; the account is
                // created without one and Settings can add it later.
                dateOfBirthMillis = 0L,
                profilePictureUri = null,
            )
            _uiState.value = when (result) {
                is InsangramResult.Success -> AuthUiState(signedIn = true)
                is InsangramResult.Failure -> AuthUiState(error = result.error.formMessage())
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (_uiState.value.submitting) return
        _uiState.value = AuthUiState(submitting = true)
        viewModelScope.launch {
            _uiState.value = when (val result = authRepository.sendPasswordReset(email)) {
                is InsangramResult.Success ->
                    AuthUiState(info = "Password reset link sent to $email")
                is InsangramResult.Failure ->
                    AuthUiState(error = result.error.formMessage())
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    /** Called after the composable has navigated away from the form. */
    fun consumeNavigation() {
        _uiState.value = _uiState.value.copy(signedIn = false)
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(error = null, info = null)
    }
}

/** Splash decides between the auth flow and the app shell. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = authRepository.sessionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionState.Unknown,
        )
}
