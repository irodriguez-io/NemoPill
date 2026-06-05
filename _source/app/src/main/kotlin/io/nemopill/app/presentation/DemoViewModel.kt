package io.nemopill.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.nemopill.core.result.Result
import io.nemopill.scheduling.application.ScheduleDemoReminderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Drives the demo screen: runs [ScheduleDemoReminderUseCase] in `viewModelScope` and
 * reflects the [Result] in [uiState]. Minimal by design — the Adherence counter and
 * observing a `Confirmation` via `StateFlow` are M-002 item (5), out of scope for T-008.
 */
@HiltViewModel
class DemoViewModel
    @Inject
    constructor(
        private val scheduleDemoReminder: ScheduleDemoReminderUseCase,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow<DemoUiState>(DemoUiState.Idle)
        val uiState: StateFlow<DemoUiState> = mutableUiState.asStateFlow()

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
