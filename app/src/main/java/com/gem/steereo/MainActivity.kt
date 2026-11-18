package com.gem.steereo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gem.steereo.ui.MainViewModel
import com.gem.steereo.ui.MainViewModel.UiState
import com.gem.steereo.ui.library.LibraryScreen
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

                val changeFolderLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    if (uri != null) {
                        viewModel.onFolderChosen(uri)
                    }
                }

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                when (uiState) {
                    is UiState.Loading -> {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is UiState.NeedsSetup -> {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            val state = uiState as UiState.NeedsSetup
                            SetupScreen(
                                onFolderChosen = { uri -> viewModel.onFolderChosen(uri) },
                                modifier = Modifier.padding(innerPadding),
                                message = state.message
                            )
                        }
                    }
                    is UiState.Ready -> {
                        LibraryScreen(
                            viewModel = viewModel,
                            onChangeFolder = { changeFolderLauncher.launch(null) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
