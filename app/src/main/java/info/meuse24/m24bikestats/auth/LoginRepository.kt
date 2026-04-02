package info.meuse24.m24bikestats.auth

import android.content.Intent
import info.meuse24.m24bikestats.domain.repository.AuthRepository

/**
 * Erweitert [AuthRepository] um die plattformspezifischen OAuth2-Methoden.
 * Lebt im auth-Paket (nicht in domain), da es Android-Intent-Typen enthält.
 * Implementiert von [AuthManager].
 */
interface LoginRepository : AuthRepository {
    fun buildAuthIntent(): Intent
    fun handleAuthResponse(intent: Intent, onSuccess: () -> Unit, onError: (String) -> Unit)
    fun buildLogoutIntent(): Intent?
    fun handleLogoutResponse(intent: Intent?, onComplete: () -> Unit, onError: (String) -> Unit)
}
