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
 * The single demo screen (M-002 walking-skeleton, scheduling leg): one "Schedule the Demo
 * Reminder" button plus a status line bound to [DemoUiState]. Stateless — the activity owns
 * the [DemoViewModel] and passes state down, keeping this composable trivial to render.
 *
 * `@Suppress(function-naming)`: Compose UI functions use PascalCase by convention, which
 * the ktlint standard rule does not auto-exempt in this version.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun DemoScreen(
    state: DemoUiState,
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
    }
}

@Composable
private fun statusLine(state: DemoUiState): String =
    when (state) {
        DemoUiState.Idle -> stringResource(R.string.demo_reminder_idle)
        is DemoUiState.Scheduled -> stringResource(R.string.demo_reminder_scheduled, state.at.toString())
        DemoUiState.Error -> stringResource(R.string.demo_reminder_error)
    }
