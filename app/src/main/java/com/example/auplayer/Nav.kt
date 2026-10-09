package com.example.auplayer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.util.ArrayDeque
import com.example.auplayer.routing.ReaderKind

/** Destinations (MS-006 shell; full screens land in MS-012/MS-020/MS-038). */
sealed interface NavDest {
    val route: String

    data object Home : NavDest {
        override val route = "home"
    }

    data object Player : NavDest {
        override val route = "player"
    }

    data object Reader : NavDest {
        override val route = "reader"
    }

    data object Web : NavDest {
        override val route = "web"
    }

    data object Settings : NavDest {
        override val route = "settings"
    }
}

/**
 * Pure back-stack (§4: pure logic is JVM-tested). The Compose [NavHost] below
 * mirrors this stack; system predictive back is on via
 * `enableOnBackInvokedCallback` (manifest, §2).
 */
class Navigator {
    private val stack = ArrayDeque<NavDest>(listOf(NavDest.Home))

    val current: NavDest get() = stack.last()

    fun navigate(dest: NavDest) {
        stack.addLast(dest)
    }

    /** Returns false when the stack is at root (caller may exit). */
    fun goBack(): Boolean {
        if (stack.size <= 1) return false
        stack.removeLast()
        return true
    }
}

@Composable
fun AuNav(container: AppContainer) {
    val controller = rememberNavController()
    NavHost(navController = controller, startDestination = NavDest.Home.route) {
        composable(NavDest.Home.route) {
            HomeScreen(
                webEnabled = container.webSourceEnabled,
                onWeb = { controller.navigate(NavDest.Web.route) }
            )
        }
        composable(NavDest.Player.route) { StubScreen("Player") }
        composable(NavDest.Reader.route) {
            val dir = container.readerDir
            if (dir != null) {
                ReaderScreen(uri = dir, kind = ReaderKind.URLS)
            } else {
                StubScreen("Reader")
            }
        }
        composable(NavDest.Web.route) {
            if (container.webSourceEnabled) {
                WebScreen(onOpenReader = { dir ->
                    container.readerDir = dir
                    controller.navigate(NavDest.Reader.route)
                })
            } else {
                StubScreen("Web unavailable in this build")
            }
        }
        composable(NavDest.Settings.route) { StubScreen("Settings") }
    }
}

@Composable
private fun StubScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = name)
    }
}
