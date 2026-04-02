package info.meuse24.m24bikestats.presentation.login

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import info.meuse24.m24bikestats.auth.LoginRepository

class LoginViewModel(private val authRepo: LoginRepository) : ViewModel() {

    var status: LoginStatus by mutableStateOf(
        if (authRepo.isAuthenticated()) LoginStatus.Authenticated else LoginStatus.Idle
    )
        private set

    fun buildAuthIntent(): Intent = authRepo.buildAuthIntent()

    fun handleAuthResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            status = LoginStatus.Error("Anmeldung abgebrochen")
            return
        }
        status = LoginStatus.Loading
        authRepo.handleAuthResponse(
            intent = data,
            onSuccess = { status = LoginStatus.Authenticated },
            onError = { status = LoginStatus.Error(it) }
        )
    }

    fun logout() {
        authRepo.clearTokens()
        status = LoginStatus.Idle
    }
    // onCleared() entfernt: AuthManager ist Koin-Singleton und darf nicht
    // vom ViewModel disposed werden – Lifecycle-Mismatch vermieden.
}
