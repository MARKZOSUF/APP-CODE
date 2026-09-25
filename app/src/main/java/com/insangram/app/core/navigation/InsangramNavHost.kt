package com.insangram.app.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.insangram.app.core.designsystem.theme.InsangramPalette
import com.insangram.app.feature.screens.*

/**
 * Root composable: a bottom navigation bar for the five primary destinations
 * plus the full navigation graph. The bar is only shown on top-level routes so
 * detail screens get the whole viewport.
 */
@Composable
fun InsangramRoot(startRoute: String? = null) {
    val navController = rememberNavController()

    // Deep links resolve after the graph is set, so navigate as a side effect.
    LaunchedEffect(startRoute) {
        if (!startRoute.isNullOrBlank()) {
            navController.navigate(startRoute)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topLevel = TopLevelDestination.entries.firstOrNull { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (topLevel != null) {
                NavigationBar(
                    containerColor = InsangramPalette.SurfaceDark,
                    contentColor = InsangramPalette.TextPrimaryDark,
                    tonalElevation = 0.dp,
                ) {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = destination == topLevel
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(destination.route) {
                                        // Keep a single copy of each tab on the back stack.
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) {
                                        destination.selectedIcon
                                    } else {
                                        destination.unselectedIcon
                                    },
                                    contentDescription = null,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = InsangramPalette.TextPrimaryDark,
                                unselectedIconColor = InsangramPalette.TextTertiaryDark,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        InsangramNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun InsangramNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val back: () -> Unit = {
        if (!navController.popBackStack()) {
            navController.navigate(Routes.HOME) { launchSingleTop = true }
        }
    }
    val navigate: (String) -> Unit = { route ->
        navController.navigate(route) { launchSingleTop = true }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier,
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.LOGIN) {
            LoginScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.REGISTER) {
            RegisterScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.EMAIL_VERIFICATION) {
            EmailVerificationScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.ACCOUNT_RECOVERY) {
            AccountRecoveryScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.ACCOUNT_SELECTION) {
            AccountSelectionScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.REAUTHENTICATE) {
            ReauthenticateScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.DELETE_ACCOUNT) {
            DeleteAccountScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.HOME) {
            HomeScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.EXPLORE) {
            ExploreScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.CREATE) {
            CreateScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.REELS) {
            ReelsScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.PROFILE_TAB) {
            ProfileTabScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.SEARCH) {
            SearchScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.SAVED) {
            SavedScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.ANALYTICS) {
            AnalyticsScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.ARCHIVE) {
            ArchiveScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.RECENTLY_DELETED) {
            RecentlyDeletedScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.MESSAGES) {
            MessagesScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.NEW_CONVERSATION) {
            NewConversationScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.NEW_GROUP) {
            NewGroupScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.SETTINGS_PRIVACY) {
            PrivacySettingsScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.SETTINGS_SECURITY) {
            SecuritySettingsScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.SETTINGS_NOTIFICATIONS) {
            NotificationSettingsScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.SETTINGS_APPEARANCE) {
            AppearanceSettingsScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.SETTINGS_DATA) {
            DataSettingsScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.SETTINGS_BLOCKED) {
            BlockedSettingsScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.SETTINGS_ACCESSIBILITY) {
            AccessibilitySettingsScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.SETTINGS_ABOUT) {
            AboutSettingsScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.STORY_CREATOR) {
            StoryCreatorScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.EDITOR) {
            EditorScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.APP_LOCK) {
            AppLockScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.FOLLOW_REQUESTS) {
            FollowRequestsScreen(onBack = back, onNavigate = navigate)
        }
        composable(Routes.PROFILE) { entry ->
            ProfileScreen(
                username = entry.arguments?.getString(Routes.ARG_USERNAME).orEmpty(),
                onBack = back,
                onNavigate = navigate,
            )
        }
        composable(Routes.POST_DETAIL) { entry ->
            PostDetailScreen(
                postId = entry.arguments?.getString(Routes.ARG_POST_ID).orEmpty(),
                onBack = back,
                onNavigate = navigate,
            )
        }
        composable(Routes.COMMENTS) { entry ->
            CommentsScreen(
                postId = entry.arguments?.getString(Routes.ARG_POST_ID).orEmpty(),
                onBack = back,
                onNavigate = navigate,
            )
        }
        composable(Routes.LIKED_BY) { entry ->
            LikedByScreen(
                postId = entry.arguments?.getString(Routes.ARG_POST_ID).orEmpty(),
                onBack = back,
                onNavigate = navigate,
            )
        }
        composable(Routes.STORY_VIEWER) { entry ->
            StoryViewerScreen(
                storyUserId = entry.arguments?.getString(Routes.ARG_STORY_USER_ID).orEmpty(),
                onBack = back,
                onNavigate = navigate,
            )
        }
        composable(Routes.CHAT) { entry ->
            ChatScreen(
                conversationId = entry.arguments?.getString(Routes.ARG_CONVERSATION_ID).orEmpty(),
                onBack = back,
                onNavigate = navigate,
            )
        }
        composable(Routes.CONVERSATION_INFO) { entry ->
            ConversationInfoScreen(
                conversationId = entry.arguments?.getString(Routes.ARG_CONVERSATION_ID).orEmpty(),
                onBack = back,
                onNavigate = navigate,
            )
        }
        composable(Routes.COLLECTION_DETAIL) { entry ->
            CollectionDetailScreen(
                collectionId = entry.arguments?.getString(Routes.ARG_COLLECTION_ID).orEmpty(),
                onBack = back,
                onNavigate = navigate,
            )
        }
        composable(Routes.HASHTAG_DETAIL) { entry ->
            HashtagDetailScreen(
                hashtag = entry.arguments?.getString(Routes.ARG_HASHTAG).orEmpty(),
                onBack = back,
                onNavigate = navigate,
            )
        }
        composable(Routes.FOLLOWERS) { entry ->
            FollowersScreen(
                username = entry.arguments?.getString(Routes.ARG_USERNAME).orEmpty(),
                onBack = back,
                onNavigate = navigate,
            )
        }
        composable(Routes.FOLLOWING) { entry ->
            FollowingScreen(
                username = entry.arguments?.getString(Routes.ARG_USERNAME).orEmpty(),
                onBack = back,
                onNavigate = navigate,
            )
        }
    }
}
