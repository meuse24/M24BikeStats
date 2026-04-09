package info.meuse24.m24bikestats

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.meuse24.m24bikestats.domain.model.AppSettings
import info.meuse24.m24bikestats.domain.usecase.ObserveAppSettingsUseCase
import info.meuse24.m24bikestats.domain.usecase.RecordExplanationTextsPromptForegroundUsageUseCase
import info.meuse24.m24bikestats.presentation.navigation.AppNavigation
import info.meuse24.m24bikestats.presentation.theme.M24BikeStatsTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val observeAppSettingsUseCase: ObserveAppSettingsUseCase by inject()
    private val recordExplanationTextsPromptForegroundUsageUseCase: RecordExplanationTextsPromptForegroundUsageUseCase by inject()
    private val foregroundSessionStartedAtElapsedRealtime = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appSettings = observeAppSettingsUseCase().collectAsStateWithLifecycle(initialValue = AppSettings()).value
            M24BikeStatsTheme(displayMode = appSettings.displayMode) {
                AppNavigation(
                    appSettings = appSettings,
                    foregroundSessionStartedAtElapsedRealtime = foregroundSessionStartedAtElapsedRealtime.value,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        foregroundSessionStartedAtElapsedRealtime.value = SystemClock.elapsedRealtime()
    }

    override fun onStop() {
        val sessionStartedAt = foregroundSessionStartedAtElapsedRealtime.value
        foregroundSessionStartedAtElapsedRealtime.value = null

        if (sessionStartedAt != null) {
            val sessionDurationMillis = (SystemClock.elapsedRealtime() - sessionStartedAt).coerceAtLeast(0L)
            if (sessionDurationMillis > 0L) {
                lifecycleScope.launch {
                    recordExplanationTextsPromptForegroundUsageUseCase(sessionDurationMillis)
                }
            }
        }

        super.onStop()
    }
}
