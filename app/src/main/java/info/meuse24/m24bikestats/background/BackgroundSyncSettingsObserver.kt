package info.meuse24.m24bikestats.background

import info.meuse24.m24bikestats.domain.usecase.ObserveAppSettingsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BackgroundSyncSettingsObserver(
    private val observeAppSettingsUseCase: ObserveAppSettingsUseCase,
    private val scheduler: BackgroundSyncScheduler,
) {
    fun start(scope: CoroutineScope): Job =
        scope.launch {
            observeAppSettingsUseCase().collectLatest { settings ->
                scheduler.update(
                    mode = settings.backgroundSyncMode,
                    detailMode = settings.cloudSyncDetailMode,
                )
            }
        }
}
