package info.meuse24.m24bikestats.presentation.login

import android.content.Intent

/**
 * Koordiniert den Android-spezifischen OAuth-Flow.
 * Kein Repository: die Schnittstelle kapselt nur Intent-basierte Interaktionen.
 */
interface AuthFlowCoordinator {
    fun buildAuthIntent(): Intent
    fun handleAuthResponse(intent: Intent, onSuccess: () -> Unit, onError: (String) -> Unit)
    fun buildLogoutIntent(): Intent?
    fun handleLogoutResponse(intent: Intent?, onComplete: () -> Unit, onError: (String) -> Unit)
}
