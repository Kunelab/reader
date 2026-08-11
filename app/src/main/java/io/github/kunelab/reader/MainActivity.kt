package io.github.kunelab.reader

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
import io.github.kunelab.reader.ui.screens.HomeScreen
import io.github.kunelab.reader.ui.screens.ReaderScreen
import io.github.kunelab.reader.ui.screens.SettingsScreen
import io.github.kunelab.reader.ui.theme.AppTheme
import io.github.kunelab.reader.ui.theme.KuneReaderTheme
import io.github.kunelab.reader.viewmodel.ReaderViewModel
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

            KuneReaderTheme(appTheme = settings.theme) {
                val incoming by pendingBook.collectAsState()
                LaunchedEffect(incoming) {
                    incoming?.let {
                        viewModel.openBook(it)
                        pendingBook.value = null
                    }
                }
                KuneReaderNavigation(viewModel)
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
fun KuneReaderNavigation(viewModel: ReaderViewModel) {
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
