package com.maxreader.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.maxreader.app.ui.screens.HomeScreen
import com.maxreader.app.ui.screens.ReaderScreen
import com.maxreader.app.ui.screens.SettingsScreen
import com.maxreader.app.ui.theme.AppTheme
import com.maxreader.app.ui.theme.MaxReaderTheme
import com.maxreader.app.viewmodel.ReaderViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** An EPUB handed to us by another app via ACTION_VIEW, waiting to be opened. */
    private val pendingBook = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingBook.value = intent?.epubUri()

        setContent {
            val viewModel: ReaderViewModel = viewModel()
            val settings by viewModel.settings.collectAsState()
            val appTheme = try {
                AppTheme.valueOf(settings.theme)
            } catch (_: IllegalArgumentException) {
                AppTheme.DARK
            }

            MaxReaderTheme(appTheme = appTheme) {
                val incoming by pendingBook.collectAsState()
                LaunchedEffect(incoming) {
                    incoming?.let {
                        viewModel.openBook(it)
                        pendingBook.value = null
                    }
                }
                MaxReaderNavigation(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingBook.value = intent.epubUri()
    }

    private fun Intent.epubUri(): Uri? = if (action == Intent.ACTION_VIEW) data else null
}

@Composable
fun MaxReaderNavigation(viewModel: ReaderViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onBookLoaded = {
                    navController.navigate("reader") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onOpenSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable("reader") {
            ReaderScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
