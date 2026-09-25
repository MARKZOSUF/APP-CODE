package com.insangram.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.insangram.app.core.designsystem.theme.InsangramTheme
import com.insangram.app.core.designsystem.theme.ThemeMode
import com.insangram.app.core.navigation.InsangramRoot
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. Every screen is a Compose destination inside
 * [InsangramRoot]; deep links declared in the manifest resolve to the same
 * navigation graph.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate to hand off from the splash theme.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            // The product design is dark-first, so the shell pins the dark theme.
            InsangramTheme(themeMode = ThemeMode.DARK) {
                InsangramRoot(startRoute = intent?.data?.let(::routeForDeepLink))
            }
        }
    }

    /**
     * Maps insangram://<host>/<id> links onto in-app routes. Unknown hosts fall
     * through to the default start destination.
     */
    private fun routeForDeepLink(uri: android.net.Uri): String? {
        val id = uri.pathSegments?.firstOrNull()
        return when (uri.host) {
            "post" -> id?.let { com.insangram.app.core.navigation.Routes.postDetail(it) }
            "user" -> id?.let { com.insangram.app.core.navigation.Routes.profile(it) }
            "reel" -> com.insangram.app.core.navigation.Routes.REELS
            "chat" -> id?.let { com.insangram.app.core.navigation.Routes.chat(it) }
            "notifications" -> com.insangram.app.core.navigation.Routes.NOTIFICATIONS
            else -> null
        }
    }
}
