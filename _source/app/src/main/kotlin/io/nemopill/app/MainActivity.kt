package io.nemopill.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import io.nemopill.app.presentation.DemoScreen
import io.nemopill.app.presentation.DemoViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: DemoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val state by viewModel.uiState.collectAsState()
                DemoScreen(state = state, onSchedule = viewModel::schedule)
            }
        }
    }
}
