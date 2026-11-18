package com.gem.steereo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gem.steereo.ui.MainViewModel
import com.gem.steereo.ui.MainViewModel.UiState
import com.gem.steereo.ui.setup.SetupScreen
import com.gem.steereo.ui.theme.SteereoTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SteereoTheme {
                LifecycleResumeEffect(Unit) {
                    viewModel.recheckPermission()
                    onPauseOrDispose { }
                }

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val modifier = Modifier.padding(innerPadding)
                    when (uiState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is UiState.NeedsSetup -> {
                            val state = uiState as UiState.NeedsSetup
                            SetupScreen(
                                onFolderChosen = { uri -> viewModel.onFolderChosen(uri) },
                                modifier = modifier,
                                message = state.message
                            )
                        }
                        is UiState.Ready -> {
                            val scanState by viewModel.scanState.collectAsStateWithLifecycle()
                            val trackCount by viewModel.trackCount.collectAsStateWithLifecycle()

                            Box(
                                modifier = modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Tracks: $trackCount")
                                    Spacer(modifier = Modifier.height(16.dp))
                                    when (val s = scanState) {
                                        is MainViewModel.ScanState.Scanning -> {
                                            LinearProgressIndicator()
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Scanning… ${s.count} files")
                                        }
                                        is MainViewModel.ScanState.Error -> {
                                            Text("Error: ${s.message}")
                                        }
                                        else -> {}
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TextButton(onClick = { viewModel.rescan() }) {
                                        Text("Rescan")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
