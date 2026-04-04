package info.meuse24.m24bikestats.presentation.login

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import info.meuse24.m24bikestats.data.auth.AuthFlowCoordinator
import info.meuse24.m24bikestats.domain.usecase.ClearAuthenticationUseCase
import info.meuse24.m24bikestats.domain.usecase.IsAuthenticatedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(
    private val authFlowCoordinator: AuthFlowCoordinator,
    private val isAuthenticated: IsAuthenticatedUseCase,
    private val clearAuthentication: ClearAuthenticationUseCase,
    private val stringResolver: LoginStringResolver,
) : ViewModel() {

    private val _status = MutableStateFlow<LoginStatus>(
        if (isAuthenticated()) LoginStatus.Authenticated else LoginStatus.Idle
    )
    val status: StateFlow<LoginStatus> = _status.asStateFlow()

    fun buildAuthIntent(): Intent = authFlowCoordinator.buildAuthIntent()

    fun handleAuthResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            _status.value = LoginStatus.Error(stringResolver.cancelled())
            return
        }
        _status.value = LoginStatus.Loading
        authFlowCoordinator.handleAuthResponse(
            intent = data,
            onSuccess = { _status.value = LoginStatus.Authenticated },
            onError = { _status.value = LoginStatus.Error(it) }
        )
    }

    fun buildLogoutIntent(): Intent? = authFlowCoordinator.buildLogoutIntent()

    fun handleLogoutResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            clearAuthentication()
            _status.value = LoginStatus.Idle
            return
        }

        authFlowCoordinator.handleLogoutResponse(
            intent = data,
            onComplete = { _status.value = LoginStatus.Idle },
            onError = {
                clearAuthentication()
                _status.value = LoginStatus.Idle
            }
        )
    }

    fun logoutLocally() {
        clearAuthentication()
        _status.value = LoginStatus.Idle
    }
    // onCleared() entfernt: AuthManager ist Koin-Singleton und darf nicht
    // vom ViewModel disposed werden – Lifecycle-Mismatch vermieden.
}
