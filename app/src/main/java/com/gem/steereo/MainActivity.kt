package com.gem.steereo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
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
import com.gem.steereo.util.PureRules

// AndroidX default nav-bar scrims (minSdk 26 < API 29, where contrast enforcement starts).
private val NAV_LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val NAV_DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val mode = themeMode ?: return@setContent // not loaded yet: render nothing
            val dark = PureRules.resolveDarkTheme(mode, isSystemInDarkTheme())

            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(NAV_LIGHT_SCRIM, NAV_DARK_SCRIM) { dark }
                )
            }

            SteereoTheme(darkTheme = dark) {
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
                            themeMode = mode,
                            onThemeModeChange = viewModel::setThemeMode,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
