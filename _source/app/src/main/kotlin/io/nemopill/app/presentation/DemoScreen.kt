package io.nemopill.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.nemopill.app.R

/**
 * The single demo screen (M-002 walking skeleton): one "Schedule the Demo Reminder" button, a
 * status line bound to [DemoUiState] (scheduling leg), and the "doses Taken" counter bound to
 * [confirmedCount] (the item-(5) observe leg — recomposes 0 → 1 when a `Confirmation` persists).
 * Stateless — the activity owns the [DemoViewModel] and passes both `state` and `confirmedCount`
 * down, keeping this composable trivial to render and to test.
 *
 * `@Suppress(function-naming)`: Compose UI functions use PascalCase by convention, which
 * the ktlint standard rule does not auto-exempt in this version.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun DemoScreen(
    state: DemoUiState,
    confirmedCount: Int,
    onSchedule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Button(onClick = onSchedule) {
            Text(text = stringResource(R.string.schedule_demo_reminder))
        }
        Text(text = statusLine(state))
        Text(text = stringResource(R.string.demo_confirmed_count, confirmedCount))
    }
}

@Composable
private fun statusLine(state: DemoUiState): String =
    when (state) {
        DemoUiState.Idle -> stringResource(R.string.demo_reminder_idle)
        is DemoUiState.Scheduled -> stringResource(R.string.demo_reminder_scheduled, state.at.toString())
        DemoUiState.Error -> stringResource(R.string.demo_reminder_error)
    }
