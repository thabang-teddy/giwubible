package com.giwu.bible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.giwu.bible.ui.auth.LoginScreen
import com.giwu.bible.ui.bookmarks.BookmarksScreen
import com.giwu.bible.ui.reader.ReadScreen
import com.giwu.bible.ui.settings.SettingsScreen
import com.giwu.bible.ui.theme.GiwuTheme
import com.giwu.bible.ui.welcome.WelcomeScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GiwuApp() }
    }

    /**
     * Reading aloud is a foreground activity here: there is no media
     * notification to control it from, so leaving the app stops the voice
     * rather than talking over whatever comes next.
     */
    override fun onStop() {
        super.onStop()
        (application as GiwuApplication).container.tts.stop()
    }
}

object Routes {
    const val WELCOME = "welcome"
    const val READ = "read"
    const val SETTINGS = "settings"
    const val LOGIN = "login"
    const val BOOKMARKS = "bookmarks"
}

@Composable
private fun GiwuApp() {
    val container = rememberAppContainer()
    val darkMode by container.prefs.darkMode.collectAsStateWithLifecycle()

    // The stored server URL and the restored session have to be in place
    // before the first screen asks for anything.
    val graph by produceState<AppGraph?>(initialValue = null, container) {
        value = container.startup.await()
    }

    GiwuTheme(darkTheme = darkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val resolved = graph
            if (resolved == null) {
                SplashScreen()
            } else {
                GiwuNavHost(container, resolved)
            }
        }
    }
}

@Composable
private fun GiwuNavHost(container: AppContainer, graph: AppGraph) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (graph.showWelcome) Routes.WELCOME else Routes.READ,
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                container = container,
                onReady = {
                    navController.navigate(Routes.READ) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onBack = if (navController.previousBackStackEntry != null) {
                    { navController.popBackStack() }
                } else {
                    null
                },
            )
        }
        composable(Routes.READ) {
            ReadScreen(
                container = container,
                graph = graph,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                onOpenLogin = { navController.navigate(Routes.LOGIN) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                graph = graph,
                onBack = { navController.popBackStack() },
                onManageTranslations = { navController.navigate(Routes.WELCOME) },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                auth = graph.auth,
                onBack = { navController.popBackStack() },
                onSignedIn = { navController.popBackStack() },
            )
        }
        composable(Routes.BOOKMARKS) {
            BookmarksScreen(
                graph = graph,
                onBack = { navController.popBackStack() },
                onOpenVerse = { bookmark ->
                    container.prefs.setPrimaryBible(bookmark.bible)
                    container.prefs.setBook(bookmark.book)
                    container.prefs.setChapter(bookmark.chapter)
                    container.prefs.setActiveVerse(bookmark.verse)
                    navController.popBackStack(Routes.READ, inclusive = false)
                },
            )
        }
    }
}

@Composable
private fun SplashScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.giwu_logo),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(20.dp))
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
    }
}
