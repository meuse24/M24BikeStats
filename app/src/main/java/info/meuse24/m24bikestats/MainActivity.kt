package info.meuse24.m24bikestats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import info.meuse24.m24bikestats.presentation.navigation.AppNavigation
import info.meuse24.m24bikestats.presentation.theme.M24BikeStatsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            M24BikeStatsTheme {
                AppNavigation()
            }
        }
    }
}
