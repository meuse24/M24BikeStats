package info.meuse24.m24bikestats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.meuse24.m24bikestats.domain.model.AppSettings
import info.meuse24.m24bikestats.domain.usecase.ObserveAppSettingsUseCase
import info.meuse24.m24bikestats.presentation.navigation.AppNavigation
import info.meuse24.m24bikestats.presentation.theme.M24BikeStatsTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val observeAppSettingsUseCase: ObserveAppSettingsUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appSettings = observeAppSettingsUseCase().collectAsStateWithLifecycle(initialValue = AppSettings()).value
            M24BikeStatsTheme(displayMode = appSettings.displayMode) {
                AppNavigation()
            }
        }
    }
}
