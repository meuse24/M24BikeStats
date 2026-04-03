package info.meuse24.m24bikestats.presentation.login

import android.app.Activity
import android.content.Intent
import info.meuse24.m24bikestats.auth.AuthFlowCoordinator
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.usecase.ClearAuthenticationUseCase
import info.meuse24.m24bikestats.domain.usecase.IsAuthenticatedUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginViewModelTest {

    @Test
    fun `initial status is authenticated when auth use case returns true`() {
        val authRepository = FakeAuthRepository(isAuthenticated = true)

        val viewModel = LoginViewModel(
            authFlowCoordinator = FakeAuthFlowCoordinator(),
            isAuthenticated = IsAuthenticatedUseCase(authRepository),
            clearAuthentication = ClearAuthenticationUseCase(authRepository),
        )

        assertTrue(viewModel.status.value is LoginStatus.Authenticated)
    }

    @Test
    fun `logout locally clears authentication and sets idle status`() {
        val authRepository = FakeAuthRepository(isAuthenticated = true)

        val viewModel = LoginViewModel(
            authFlowCoordinator = FakeAuthFlowCoordinator(),
            isAuthenticated = IsAuthenticatedUseCase(authRepository),
            clearAuthentication = ClearAuthenticationUseCase(authRepository),
        )

        viewModel.logoutLocally()

        assertEquals(1, authRepository.clearTokensCalls)
        assertTrue(viewModel.status.value is LoginStatus.Idle)
    }

    @Test
    fun `cancelled auth result sets error state`() {
        val viewModel = LoginViewModel(
            authFlowCoordinator = FakeAuthFlowCoordinator(),
            isAuthenticated = IsAuthenticatedUseCase(FakeAuthRepository(isAuthenticated = false)),
            clearAuthentication = ClearAuthenticationUseCase(FakeAuthRepository(isAuthenticated = false)),
        )

        viewModel.handleAuthResult(resultCode = Activity.RESULT_CANCELED, data = null)

        assertEquals(
            LoginStatus.Error("Anmeldung abgebrochen"),
            viewModel.status.value,
        )
    }

    private class FakeAuthRepository(
        private val isAuthenticated: Boolean,
    ) : AuthRepository {
        var clearTokensCalls: Int = 0

        override fun getAccessToken(): String? = null

        override suspend fun getValidAccessToken(): Result<String> =
            Result.failure(IllegalStateException("not used"))

        override fun isAuthenticated(): Boolean = isAuthenticated

        override fun clearTokens() {
            clearTokensCalls += 1
        }
    }

    private class FakeAuthFlowCoordinator : AuthFlowCoordinator {
        override fun buildAuthIntent(): Intent = Intent("auth")

        override fun handleAuthResponse(
            intent: Intent,
            onSuccess: () -> Unit,
            onError: (String) -> Unit,
        ) = Unit

        override fun buildLogoutIntent(): Intent? = Intent("logout")

        override fun handleLogoutResponse(
            intent: Intent?,
            onComplete: () -> Unit,
            onError: (String) -> Unit,
        ) = Unit
    }
}
