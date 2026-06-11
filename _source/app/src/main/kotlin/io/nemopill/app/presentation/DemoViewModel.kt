package io.nemopill.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.nemopill.adherencetracking.application.ObserveConfirmedDoseCountUseCase
import io.nemopill.core.result.Result
import io.nemopill.scheduling.application.ScheduleDemoReminderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Drives the demo screen. Two `StateFlow`s feed the stateless [DemoScreen]:
 *  - [uiState] reflects the [Result] of [ScheduleDemoReminderUseCase] (the T-008 scheduling leg);
 *  - [confirmedCount] is the M-002 item (5) observe leg — the on-screen "doses Taken" count
 *    collected from [ObserveConfirmedDoseCountUseCase]'s Room-backed `Flow<Int>`, incrementing
 *    0 → 1 when a `Confirmation` is persisted (the T-010 write path). It is a raw Taken tally,
 *    **not** the BR-008 Adherence percentage (M-005).
 */
@HiltViewModel
class DemoViewModel
    @Inject
    constructor(
        private val scheduleDemoReminder: ScheduleDemoReminderUseCase,
        private val observeConfirmedDoseCount: ObserveConfirmedDoseCountUseCase,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow<DemoUiState>(DemoUiState.Idle)
        val uiState: StateFlow<DemoUiState> = mutableUiState.asStateFlow()

        /**
         * The on-screen "doses Taken" count. `WhileSubscribed(5_000)` keeps the upstream Room query
         * active only while the screen observes it and re-reads the durable row on re-subscription,
         * so the count is correct after backgrounding or process death (the realistic
         * tap-from-lock-screen → open-app demo flow — the reason the observe seam is the Room `Flow`,
         * not the in-memory `DoseConfirmed` event). Initial value 0 until the first emission.
         */
        val confirmedCount: StateFlow<Int> =
            observeConfirmedDoseCount()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = 0,
                )

        fun schedule() {
            viewModelScope.launch {
                mutableUiState.value =
                    when (val result = scheduleDemoReminder()) {
                        is Result.Ok -> DemoUiState.Scheduled(result.value)
                        is Result.Err -> DemoUiState.Error
                    }
            }
        }
    }

sealed interface DemoUiState {
    data object Idle : DemoUiState

    data class Scheduled(val at: Instant) : DemoUiState

    data object Error : DemoUiState
}
