package info.meuse24.m24bikestats.presentation.login

sealed class LoginStatus {
    object Idle : LoginStatus()
    object Loading : LoginStatus()
    object Authenticated : LoginStatus()
    data class Error(val message: String) : LoginStatus()
}
