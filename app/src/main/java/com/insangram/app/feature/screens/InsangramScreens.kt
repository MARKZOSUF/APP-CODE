package com.insangram.app.feature.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.insangram.app.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Grid3x3
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.insangram.app.core.designsystem.theme.InsangramGradients
import com.insangram.app.core.designsystem.theme.InsangramPalette
import androidx.hilt.navigation.compose.hiltViewModel
import com.insangram.app.core.navigation.Routes
import com.insangram.app.domain.model.SessionState
import com.insangram.app.feature.auth.AuthViewModel
import com.insangram.app.feature.auth.SessionViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.insangram.app.feature.ui.DemoContent
import com.insangram.app.feature.ui.FilterChipPill
import com.insangram.app.feature.ui.GradientButton
import com.insangram.app.feature.ui.GridTile
import com.insangram.app.feature.ui.ListRow
import com.insangram.app.feature.ui.NetworkImage
import com.insangram.app.feature.ui.OutlinedPill
import com.insangram.app.feature.ui.PostCard
import com.insangram.app.feature.ui.ProfileStat
import com.insangram.app.feature.ui.ScreenHeader
import com.insangram.app.feature.ui.StoryItem
import com.insangram.app.feature.ui.StoryRing
import kotlinx.coroutines.delay

/**
 * Every destination in the application. Primary surfaces (feed, reels,
 * explore, profile, messaging, composer) are laid out in full; the remaining
 * destinations share [DetailScreen] so navigation, spacing and the dark theme
 * stay identical everywhere.
 */

// ---------------------------------------------------------------------------
// Shared chrome
// ---------------------------------------------------------------------------

@Composable
private fun Surface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        content()
    }
}

/** Fallback layout for secondary destinations. */
@Composable
private fun DetailScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    actions: List<Pair<String, String>> = emptyList(),
    onNavigate: (String) -> Unit = {},
) {
    Surface {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = title, onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                actions.forEach { (label, route) ->
                    OutlinedPill(
                        label = label,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigate(route) },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SearchField(
    placeholder: String,
    value: String = "",
    onValueChange: (String) -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

// ---------------------------------------------------------------------------
// Launch and authentication
// ---------------------------------------------------------------------------

/**
 * The rounded, icon-led input used on every auth screen so Login, Sign up and
 * Edit profile all look identical to the design.
 */
@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
    var revealed by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { revealed = !revealed }) {
                    Icon(
                        imageVector = if (revealed) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (revealed) "Hide password" else "Show password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            null
        },
        visualTransformation = if (isPassword && !revealed) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            imeAction = imeAction,
        ),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
    )
}

/** The app mark rendered from the launcher artwork, so brand stays consistent. */
@Composable
private fun BrandMark(size: Int) {
    Image(
        painter = painterResource(R.drawable.insangram_logo),
        contentDescription = "Insangram",
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(size.dp),
    )
}

@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val session by viewModel.sessionState.collectAsState()

    // Keep the brand moment on screen for a beat, then send people to the feed
    // when a session already exists and to the login form when it does not.
    LaunchedEffect(session) {
        delay(1200)
        when (session) {
            is SessionState.SignedIn -> onNavigate(Routes.HOME)
            SessionState.SignedOut -> onNavigate(Routes.LOGIN)
            SessionState.Unknown -> Unit
        }
    }
    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandMark(size = 132)
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Insangram",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Share your moments\nwith the world",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun OnboardingScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Welcome",
        subtitle = "A quick tour of feeds, reels and messaging before you sign in.",
        onBack = onBack,
        actions = listOf("Log in" to Routes.LOGIN, "Create account" to Routes.REGISTER),
        onNavigate = onNavigate,
    )
}

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var remembered by remember { mutableStateOf(true) }
    var googleNotice by remember { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.signedIn) {
        if (state.signedIn) {
            viewModel.consumeNavigation()
            onNavigate(Routes.HOME)
        }
    }

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(70.dp))
            BrandMark(size = 92)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Insangram",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = "Welcome back!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Log in to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email or Phone Number",
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
            )
            Spacer(Modifier.height(12.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                imeAction = ImeAction.Done,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(checked = remembered, onCheckedChange = { remembered = it })
                Text(
                    text = "Remember me",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Forgot password?",
                    style = MaterialTheme.typography.labelMedium,
                    color = InsangramPalette.Info,
                    modifier = Modifier.clickable { onNavigate(Routes.FORGOT_PASSWORD) },
                )
            }
            Spacer(Modifier.height(12.dp))
            state.error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    color = InsangramPalette.Danger,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            if (state.submitting) {
                CircularProgressIndicator(
                    color = InsangramPalette.Pink,
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.height(14.dp))
            } else {
                GradientButton(text = "Log In") { viewModel.signIn(email, password) }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "or",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedPill(
                label = "Continue with Google",
                modifier = Modifier.fillMaxWidth(),
                // Google sign-in is not wired to a real credential flow yet, so
                // it must never open the app. Showing a message keeps the
                // account boundary honest instead of faking a session.
                onClick = { googleNotice = true },
            )
            if (googleNotice) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Google sign-in isn't enabled yet. Please use your email and password.",
                    style = MaterialTheme.typography.labelMedium,
                    color = InsangramPalette.Info,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(26.dp))
            Row {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.bodySmall,
                    color = InsangramPalette.Pink,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigate(Routes.REGISTER) },
                )
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var googleNotice by remember { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.signedIn) {
        if (state.signedIn) {
            viewModel.consumeNavigation()
            onNavigate(Routes.HOME)
        }
    }

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader(title = "", onBack = onBack)
            Column(modifier = Modifier.padding(horizontal = 26.dp)) {
                Text(
                    text = "Create your account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Join Insangram and be part of an amazing community",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(22.dp))
                AuthTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Full Name",
                    leadingIcon = Icons.Outlined.Person,
                )
                Spacer(Modifier.height(12.dp))
                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Email or Phone Number",
                    leadingIcon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email,
                )
                Spacer(Modifier.height(12.dp))
                AuthTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "Username",
                    leadingIcon = Icons.Outlined.AlternateEmail,
                )
                Spacer(Modifier.height(12.dp))
                AuthTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password",
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                )
                Spacer(Modifier.height(12.dp))
                Spacer(Modifier.height(8.dp))
                state.error?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelMedium,
                        color = InsangramPalette.Danger,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (state.submitting) {
                    CircularProgressIndicator(
                        color = InsangramPalette.Pink,
                        modifier = Modifier.size(30.dp),
                    )
                } else {
                    GradientButton(text = "Sign Up") {
                        viewModel.register(
                            fullName = name,
                            username = username,
                            email = email,
                            password = password,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "By signing up, you agree to our Terms of Service and Privacy Policy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                OutlinedPill(
                    label = "Continue with Google",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { googleNotice = true },
                )
                if (googleNotice) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Google sign-up isn't enabled yet. Please create an account with your email.",
                        style = MaterialTheme.typography.labelMedium,
                        color = InsangramPalette.Info,
                    )
                }
                Spacer(Modifier.height(26.dp))
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Forgot password",
        subtitle = "Enter the email linked to your account and we will send a reset link.",
        onBack = onBack,
        actions = listOf("Send reset link" to Routes.LOGIN),
        onNavigate = onNavigate,
    )
}

@Composable
fun EmailVerificationScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Verify email",
        subtitle = "We sent you a six digit code. Enter it to finish setting up your account.",
        onBack = onBack,
        actions = listOf("Continue" to Routes.HOME),
        onNavigate = onNavigate,
    )
}

@Composable
fun AccountRecoveryScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Account recovery",
        subtitle = "Recover access using a trusted device or a backup code.",
        onBack = onBack,
        actions = listOf("Back to login" to Routes.LOGIN),
        onNavigate = onNavigate,
    )
}

@Composable
fun AccountSelectionScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Switch account",
        subtitle = "Choose which saved account to continue with.",
        onBack = onBack,
        actions = listOf("Add account" to Routes.LOGIN),
        onNavigate = onNavigate,
    )
}

@Composable
fun ReauthenticateScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Confirm it's you",
        subtitle = "Re-enter your password to continue with this sensitive action.",
        onBack = onBack,
        actions = listOf("Confirm" to Routes.SETTINGS),
        onNavigate = onNavigate,
    )
}

@Composable
fun DeleteAccountScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Delete account",
        subtitle = "Deleting removes your posts, reels and messages after a 30 day grace period.",
        onBack = onBack,
        actions = listOf("Keep my account" to Routes.SETTINGS),
        onNavigate = onNavigate,
    )
}

// ---------------------------------------------------------------------------
// Home feed
// ---------------------------------------------------------------------------

@Composable
fun HomeScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    // Auth guard: the feed is never reachable without a real session, so a
    // signed-out viewer is pushed straight back to the login form.
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val session by sessionViewModel.sessionState.collectAsState()
    LaunchedEffect(session) {
        if (session is SessionState.SignedOut) {
            onNavigate(Routes.LOGIN)
        }
    }

    val feedViewModel: HomeFeedViewModel = hiltViewModel()
    val feed = feedViewModel.feed.collectAsLazyPagingItems()
    val stories by feedViewModel.stories.collectAsState()
    val viewerUser by feedViewModel.viewer.collectAsState()

    Surface {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 6.dp, top = 10.dp, bottom = 6.dp),
                ) {
                    Text(
                        text = "Insangram",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onNavigate(Routes.NOTIFICATIONS) }) {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Notifications")
                    }
                    IconButton(onClick = { onNavigate(Routes.MESSAGES) }) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Messages")
                    }
                }
            }

            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 10.dp),
                ) {
                    item {
                        StoryItem(
                            label = "Your story",
                            avatarUrl = viewerUser?.photoUrl ?: "",
                            isViewer = true,
                            onClick = { onNavigate(Routes.STORY_CREATOR) },
                        )
                    }
                    items(stories) { tray ->
                        StoryItem(
                            label = tray.author.username,
                            avatarUrl = tray.author.photoUrl ?: "",
                            seen = tray.allSeen,
                            onClick = { onNavigate(Routes.storyViewer(tray.author.id)) },
                        )
                    }
                }
            }

            items(feed.itemCount) { index ->
                val post = feed[index]
                if (post != null) {
                    PostCard(
                        post = post.toPresentation(),
                        onOpenProfile = { onNavigate(Routes.profile(post.author.username)) },
                        onOpenComments = { onNavigate(Routes.comments(post.id)) },
                        onLike = { feedViewModel.toggleLike(post.id) },
                        onSave = { feedViewModel.toggleSave(post.id) },
                        onShare = { feedViewModel.share(post.id) },
                        initiallySaved = post.savedByViewer,
                    )
                }
            }

            if (feed.itemCount == 0) {
                item {
                    EmptyStateBlock(
                        title = "Your feed is empty",
                        message = "Follow a few accounts or share your first post to get started.",
                        actionLabel = "Create your first post",
                        onAction = { onNavigate(Routes.CREATE) },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Explore and search
// ---------------------------------------------------------------------------

@Composable
fun ExploreScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    var selected by remember { mutableIntStateOf(0) }

    Surface {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Explore",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 18.dp, top = 12.dp, bottom = 10.dp),
            )
            Box(modifier = Modifier.clickable { onNavigate(Routes.SEARCH) }) {
                SearchField(placeholder = "Search users, hashtags, or places")
            }
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                DemoContent.exploreFilters.forEachIndexed { index, filter ->
                    FilterChipPill(
                        label = filter,
                        selected = index == selected,
                        onClick = { selected = index },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                items(DemoContent.exploreTiles) { tile ->
                    GridTile(
                        url = tile.image,
                        tag = tile.tag,
                        onClick = { onNavigate(Routes.hashtag(tile.tag)) },
                    )
                }
            }
        }
    }
}

@Composable
fun SearchScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    var query by remember { mutableStateOf("") }

    Surface {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    SearchField(
                        placeholder = "Search...",
                        value = query,
                        onValueChange = { query = it },
                    )
                }
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(horizontal = 14.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                listOf("Top", "Accounts", "Tags", "Places").forEachIndexed { index, label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (index == 0) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(DemoContent.searchSuggestions) { suggestion ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(Routes.hashtag(suggestion)) }
                            .padding(horizontal = 18.dp, vertical = 11.dp),
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(text = suggestion, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    Text(
                        text = "Suggested accounts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 6.dp),
                    )
                }
                items(DemoContent.suggestedAccounts) { account ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(Routes.profile(account.username)) }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                    ) {
                        StoryRing(avatarUrl = account.avatar, size = 44)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.username,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Outlined.PersonAdd,
                            contentDescription = null,
                            tint = InsangramPalette.Violet,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Create and Reels
// ---------------------------------------------------------------------------

@Composable
fun CreateScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    var selectedFilter by remember { mutableIntStateOf(0) }

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "New Post",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Next",
                    style = MaterialTheme.typography.titleSmall,
                    color = InsangramPalette.Info,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onNavigate(Routes.EDITOR) }
                        .padding(horizontal = 14.dp),
                )
            }

            NetworkImage(
                url = DemoContent.composerPreview,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )

            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                DemoContent.filters.forEachIndexed { index, filter ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (index == selectedFilter) {
                                        Modifier.border(2.dp, InsangramPalette.Violet, RoundedCornerShape(10.dp))
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { selectedFilter = index },
                        ) {
                            NetworkImage(
                                url = DemoContent.composerPreview,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (index == selectedFilter) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            ListRow(label = "Add caption...", icon = Icons.Outlined.Edit)
            ListRow(label = "Tag people", icon = Icons.Outlined.Group)
            ListRow(label = "Add location", icon = Icons.Outlined.Star)
            ListRow(label = "Advanced settings", icon = Icons.Outlined.Settings)
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                OutlinedPill(
                    label = "New story",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.STORY_CREATOR) },
                )
                OutlinedPill(
                    label = "Open editor",
                    modifier = Modifier.weight(1f),
                    filled = true,
                    onClick = { onNavigate(Routes.EDITOR) },
                )
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
fun ReelsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val reels = DemoContent.reels
    val pagerState = rememberPagerState(pageCount = { reels.size })

    Surface {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val reel = reels[page]
            Box(modifier = Modifier.fillMaxSize()) {
                NetworkImage(url = reel.image, modifier = Modifier.fillMaxSize())

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                        .align(Alignment.BottomCenter)
                        .background(InsangramGradients.darkScrim),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = "Reels",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onNavigate(Routes.CREATE) }) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "Record reel",
                            tint = Color.White,
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp, bottom = 90.dp),
                ) {
                    ReelAction(Icons.Outlined.FavoriteBorder, reel.likes)
                    ReelAction(Icons.Outlined.ChatBubbleOutline, reel.comments)
                    ReelAction(Icons.AutoMirrored.Outlined.Send, reel.shares)
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = Color.White,
                    )
                    NetworkImage(
                        url = reel.image,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White, RoundedCornerShape(8.dp)),
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 80.dp, bottom = 26.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StoryRing(
                            avatarUrl = reel.author.avatar,
                            size = 34,
                            ringed = false,
                            onClick = { onNavigate(Routes.profile(reel.author.username)) },
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            text = reel.author.username,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 3.dp),
                        ) {
                            Text(text = "Follow", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = reel.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "\u266A ${reel.audio}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReelAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Text(text = label, color = Color.White, fontSize = 11.sp)
    }
}

// ---------------------------------------------------------------------------
// Profile
// ---------------------------------------------------------------------------

@Composable
private fun ProfileBody(
    username: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    showBack: Boolean,
) {
    var tab by remember { mutableIntStateOf(0) }

    val profileViewModel: UserProfileViewModel = hiltViewModel()
    LaunchedEffect(username) { profileViewModel.open(username) }
    val profileUser by profileViewModel.user.collectAsState()
    val profilePosts = profileViewModel.posts.collectAsLazyPagingItems()

    Surface {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = if (showBack) 4.dp else 18.dp, end = 6.dp, top = 8.dp),
                ) {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Text(
                        text = username,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onNavigate(Routes.CREATE) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create")
                    }
                    IconButton(onClick = { onNavigate(Routes.SETTINGS) }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    StoryRing(avatarUrl = profileUser?.photoUrl ?: "", size = 86)
                    Spacer(Modifier.width(18.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.weight(1f),
                    ) {
                        ProfileStat(formatSocialCount(profileUser?.postCount ?: 0L), "Posts")
                        ProfileStat(formatSocialCount(profileUser?.followerCount ?: 0L), "Followers")
                        ProfileStat(formatSocialCount(profileUser?.followingCount ?: 0L), "Following")
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    Text(
                        text = profileUser?.fullName ?: username,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!profileUser?.bio.isNullOrBlank()) {
                        Text(
                            text = profileUser?.bio ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!profileUser?.pronouns.isNullOrBlank()) {
                        Text(
                            text = profileUser?.pronouns ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!profileUser?.website.isNullOrBlank()) {
                        Text(
                            text = profileUser?.website ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = InsangramPalette.Info,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedPill(
                        label = "Edit Profile",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigate(Routes.EDIT_PROFILE) },
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    DemoContent.highlights.forEach { highlight ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            NetworkImage(
                                url = highlight.image,
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, InsangramPalette.OutlineDark, CircleShape),
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = highlight.label,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .border(1.dp, InsangramPalette.OutlineDark, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "New highlight")
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(text = "New", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    listOf(
                        Icons.Outlined.Grid3x3,
                        Icons.Outlined.PlayCircle,
                        Icons.Outlined.AccountCircle,
                    ).forEachIndexed { index, icon ->
                        IconButton(onClick = { tab = index }) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (tab == index) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }

            val gridItems = (0 until profilePosts.itemCount).mapNotNull { index ->
                val post = profilePosts[index]
                if (post == null) null else post.id to (post.media.firstOrNull()?.url ?: "")
            }

            items(gridItems.chunked(3)) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 1.5.dp),
                ) {
                    row.forEach { entry ->
                        Box(modifier = Modifier.weight(1f)) {
                            GridTile(
                                url = entry.second,
                                onClick = { onNavigate(Routes.postDetail(entry.first)) },
                            )
                        }
                    }
                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            if (gridItems.isEmpty()) {
                item {
                    EmptyStateBlock(
                        title = "No posts yet",
                        message = "Posts you share will show up here.",
                        actionLabel = if (showBack) null else "Share a post",
                        onAction = { onNavigate(Routes.CREATE) },
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileTabScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val viewerViewModel: ProfileTabViewModel = hiltViewModel()
    val viewer by viewerViewModel.viewer.collectAsState()
    ProfileBody(
        username = viewer?.username ?: "",
        onBack = onBack,
        onNavigate = onNavigate,
        showBack = false,
    )
}

@Composable
fun ProfileScreen(username: String, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    ProfileBody(
        username = username,
        onBack = onBack,
        onNavigate = onNavigate,
        showBack = true,
    )
}

// ---------------------------------------------------------------------------
// Messaging
// ---------------------------------------------------------------------------

@Composable
fun MessagesScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }

    val inboxViewModel: InboxViewModel = hiltViewModel()
    val conversations by inboxViewModel.conversations.collectAsState()
    val chatRows = conversations.map { it.id to it.toPresentation() }

    Surface {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 6.dp, top = 10.dp),
            ) {
                Text(
                    text = "Insangram",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onNavigate(Routes.NEW_CONVERSATION) }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "New message")
                }
            }
            Spacer(Modifier.height(6.dp))
            SearchField(placeholder = "Search chats")
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.padding(horizontal = 18.dp),
            ) {
                listOf("Primary", "General", "Requests").forEachIndexed { index, label ->
                    Text(
                        text = if (index == 2) "$label  2" else label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (tab == index) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (tab == index) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .clickable { tab = index }
                            .padding(vertical = 8.dp),
                    )
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (chatRows.isEmpty()) {
                    item {
                        EmptyStateBlock(
                            title = "No messages yet",
                            message = "Start a conversation with someone you follow.",
                            actionLabel = null,
                        )
                    }
                }
                items(chatRows) { row ->
                    val conversationId = row.first
                    val chat = row.second
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(Routes.chat(conversationId)) }
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                    ) {
                        StoryRing(avatarUrl = chat.user.avatar, size = 50, ringed = false)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chat.user.username,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${chat.preview}  \u00B7 ${chat.time}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        if (chat.unread) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(InsangramPalette.Info),
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(conversationId: String, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val chatViewModel: ChatViewModel = hiltViewModel()
    LaunchedEffect(conversationId) { chatViewModel.open(conversationId) }
    val liveMessages by chatViewModel.messages.collectAsState()
    val chatTitle by chatViewModel.title.collectAsState()
    val messageRows = liveMessages.map { it.toPresentation(chatViewModel.viewerId) }
    val partnerAvatar = liveMessages
        .firstOrNull { it.sender.id != chatViewModel.viewerId }
        ?.sender
        ?.photoUrl
        .orEmpty()
    var draft by remember { mutableStateOf("") }

    Surface {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                StoryRing(avatarUrl = partnerAvatar, size = 38, ringed = false)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chatTitle.ifBlank { "Chat" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Active now",
                        style = MaterialTheme.typography.labelSmall,
                        color = InsangramPalette.OnlineGreen,
                    )
                }
                IconButton(onClick = { onNavigate(Routes.conversationInfo(conversationId)) }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Conversation info")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 14.dp,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (messageRows.isEmpty()) {
                    item {
                        EmptyStateBlock(
                            title = "No messages yet",
                            message = "Say hello to start this conversation.",
                        )
                    }
                }
                items(messageRows) { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start,
                    ) {
                        if (message.image != null) {
                            NetworkImage(
                                url = message.image,
                                modifier = Modifier
                                    .width(190.dp)
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (message.mine) {
                                            InsangramPalette.Violet
                                        } else {
                                            InsangramPalette.SurfaceDarkElevated
                                        },
                                    )
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                            ) {
                                Column {
                                    Text(
                                        text = message.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                    )
                                    Text(
                                        text = message.time,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xCCFFFFFF),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(InsangramGradients.brand),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", tint = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Message...") },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Mic, contentDescription = "Voice message")
                }
                IconButton(
                    onClick = {
                        chatViewModel.send(draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank(),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
fun NewConversationScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "New message",
        subtitle = "Pick a person to start a new conversation with.",
        onBack = onBack,
        actions = listOf("Create group instead" to Routes.NEW_GROUP),
        onNavigate = onNavigate,
    )
}

@Composable
fun NewGroupScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "New group",
        subtitle = "Add members and name your group chat.",
        onBack = onBack,
        actions = listOf("Back to messages" to Routes.MESSAGES),
        onNavigate = onNavigate,
    )
}

@Composable
fun ConversationInfoScreen(
    conversationId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    DetailScreen(
        title = "Conversation info",
        subtitle = "Members, media and privacy controls for $conversationId.",
        onBack = onBack,
        actions = listOf("Back to messages" to Routes.MESSAGES),
        onNavigate = onNavigate,
    )
}

// ---------------------------------------------------------------------------
// Activity, saved and settings
// ---------------------------------------------------------------------------

@Composable
fun NotificationsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }

    val notificationsViewModel: NotificationsFeedViewModel = hiltViewModel()
    val liveNotifications by notificationsViewModel.notifications.collectAsState()
    val notificationRows = liveNotifications.map { it.toPresentation() }
    LaunchedEffect(Unit) { notificationsViewModel.markAllRead() }

    Surface {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Notifications", onBack = onBack)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            ) {
                listOf("All", "Likes", "Comments", "Follows").forEachIndexed { index, label ->
                    FilterChipPill(
                        label = label,
                        selected = tab == index,
                        onClick = { tab = index },
                    )
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (notificationRows.isEmpty()) {
                    item {
                        EmptyStateBlock(
                            title = "No activity yet",
                            message = "Likes, comments and new followers will appear here.",
                            actionLabel = null,
                        )
                    }
                }
                items(notificationRows) { notification ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(Routes.profile(notification.user.username)) }
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                    ) {
                        StoryRing(avatarUrl = notification.user.avatar, size = 44, ringed = false)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${notification.user.username} ${notification.text}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = notification.time,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        if (notification.follow) {
                            OutlinedPill(label = "Follow", filled = true)
                        } else if (notification.thumbnail != null) {
                            NetworkImage(
                                url = notification.thumbnail,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val profileViewModel: ProfileTabViewModel = hiltViewModel()
    val savedPosts by profileViewModel.saved.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Surface {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Saved", onBack = onBack)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            ) {
                listOf("All", "Posts", "Reels").forEachIndexed { index, label ->
                    FilterChipPill(
                        label = label,
                        selected = tab == index,
                        onClick = { tab = index },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            ) {
                items(savedPosts) { post ->
                    GridTile(
                        url = post.media.firstOrNull()?.url.orEmpty(),
                        onClick = { onNavigate(Routes.postDetail(post.id)) },
                    )
                }
            }
            if (savedPosts.isEmpty()) {
                EmptyStateBlock(
                    title = "Nothing saved yet",
                    message = "Posts and reels you save will show up here.",
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val profileViewModel: ProfileTabViewModel = hiltViewModel()
    val viewer by profileViewModel.viewer.collectAsState()
    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader(title = "Settings", onBack = onBack)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(Routes.PROFILE_TAB) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                StoryRing(avatarUrl = viewer?.photoUrl.orEmpty(), size = 52, ringed = false)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = viewer?.username.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "View profile",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ListRow("Settings and privacy", Icons.Outlined.Settings) { onNavigate(Routes.SETTINGS_PRIVACY) }
            ListRow("Your activity", Icons.Outlined.History) { onNavigate(Routes.ANALYTICS) }
            ListRow("Archive", Icons.Outlined.Archive) { onNavigate(Routes.ARCHIVE) }
            ListRow("QR code", Icons.Outlined.QrCode)
            ListRow("Saved", Icons.Outlined.Bookmark) { onNavigate(Routes.SAVED) }
            ListRow("Supervision", Icons.Outlined.SupervisorAccount)
            ListRow("Orders and payments", Icons.Outlined.Payment)
            ListRow("Meta Verified", Icons.Outlined.Verified, trailingLabel = "NEW")
            ListRow("Close Friends", Icons.Outlined.Group)
            ListRow("Favorites", Icons.Outlined.Star)
            ListRow("Discover people", Icons.Outlined.PersonAdd) { onNavigate(Routes.SEARCH) }
            ListRow("Notifications", Icons.Outlined.Notifications) { onNavigate(Routes.SETTINGS_NOTIFICATIONS) }
            ListRow("Security", Icons.Outlined.Lock) { onNavigate(Routes.SETTINGS_SECURITY) }
            ListRow("Appearance", Icons.Outlined.Analytics) { onNavigate(Routes.SETTINGS_APPEARANCE) }
            ListRow("About", Icons.Outlined.AccountCircle) { onNavigate(Routes.SETTINGS_ABOUT) }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Log Out",
                style = MaterialTheme.typography.titleSmall,
                color = InsangramPalette.Danger,
                modifier = Modifier
                    .clickable { onNavigate(Routes.LOGIN) }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun EditProfileScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val profileViewModel: ProfileTabViewModel = hiltViewModel()
    val viewer by profileViewModel.viewer.collectAsState()
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    LaunchedEffect(viewer?.id) {
        val loaded = viewer ?: return@LaunchedEffect
        name = loaded.fullName
        username = loaded.username
        bio = loaded.bio
        website = loaded.website
        gender = loaded.pronouns
    }

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader(
                title = "Edit Profile",
                onBack = onBack,
                trailing = {
                    Text(
                        text = "Done",
                        color = InsangramPalette.Info,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                profileViewModel.saveProfile(
                                    fullName = name,
                                    username = username,
                                    bio = bio,
                                    website = website,
                                    pronouns = gender,
                                )
                                onBack()
                            }
                            .padding(horizontal = 14.dp),
                    )
                },
            )
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                StoryRing(avatarUrl = viewer?.photoUrl.orEmpty(), size = 96, ringed = false)
            }
            Spacer(Modifier.height(18.dp))
            listOf(
                "Name" to name,
                "Username" to username,
                "Bio" to bio,
                "Website" to website,
                "Pronouns" to gender,
            ).forEachIndexed { index, (label, value) ->
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = value,
                        onValueChange = {
                            when (index) {
                                0 -> name = it
                                1 -> username = it
                                2 -> bio = it
                                3 -> website = it
                                else -> gender = it
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun StoryViewerScreen(storyUserId: String, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    var reply by remember { mutableStateOf("") }

    Surface {
        Box(modifier = Modifier.fillMaxSize()) {
            NetworkImage(
                url = DemoContent.reels.first().image,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
            ) {
                StoryRing(avatarUrl = DemoContent.storyUsers.first().avatar, size = 34, ringed = false)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = storyUserId,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(text = "2h", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 18.dp),
            ) {
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    placeholder = { Text("Send message...") },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Like", tint = Color.White)
                }
                IconButton(onClick = { onNavigate(Routes.MESSAGES) }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Share",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
fun PostDetailScreen(postId: String, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val post = DemoContent.posts.firstOrNull { it.id == postId } ?: DemoContent.posts.first()

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader(title = "Post", onBack = onBack)
            PostCard(
                post = post,
                onOpenProfile = { onNavigate(Routes.profile(post.author.username)) },
                onOpenComments = { onNavigate(Routes.comments(post.id)) },
            )
        }
    }
}

@Composable
fun CommentsScreen(postId: String, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    var draft by remember { mutableStateOf("") }

    Surface {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Comments", onBack = onBack)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(DemoContent.notifications) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(Routes.profile(item.user.username)) }
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                    ) {
                        StoryRing(avatarUrl = item.user.avatar, size = 38, ringed = false)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.user.username,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Nice one! \uD83D\uDD25",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = item.time,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like comment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Add a comment for $postId...") },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { draft = "" }) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Post comment")
                }
            }
        }
    }
}

@Composable
fun LikedByScreen(postId: String, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Likes",
        subtitle = "Everyone who liked $postId.",
        onBack = onBack,
        actions = listOf("Back to post" to Routes.postDetail(postId)),
        onNavigate = onNavigate,
    )
}

@Composable
fun HashtagDetailScreen(hashtag: String, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    Surface {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "#${hashtag.removePrefix("#")}", onBack = onBack)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            ) {
                items(DemoContent.profileGrid) { url ->
                    GridTile(url = url, onClick = { onNavigate(Routes.postDetail("post-1")) })
                }
            }
        }
    }
}

@Composable
fun CollectionDetailScreen(
    collectionId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    DetailScreen(
        title = collectionId,
        subtitle = "Everything you saved into this collection.",
        onBack = onBack,
        actions = listOf("Back to saved" to Routes.SAVED),
        onNavigate = onNavigate,
    )
}

@Composable
fun FollowersScreen(username: String, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Followers",
        subtitle = "People following $username.",
        onBack = onBack,
        actions = listOf("Back to profile" to Routes.profile(username)),
        onNavigate = onNavigate,
    )
}

@Composable
fun FollowingScreen(username: String, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Following",
        subtitle = "Accounts $username follows.",
        onBack = onBack,
        actions = listOf("Back to profile" to Routes.profile(username)),
        onNavigate = onNavigate,
    )
}

@Composable
fun FollowRequestsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Follow requests",
        subtitle = "Approve or decline people who asked to follow you.",
        onBack = onBack,
        actions = listOf("Back to profile" to Routes.PROFILE_TAB),
        onNavigate = onNavigate,
    )
}

@Composable
fun AnalyticsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Your activity",
        subtitle = "Reach, interactions and time spent across the last 30 days.",
        onBack = onBack,
        actions = listOf("Back to settings" to Routes.SETTINGS),
        onNavigate = onNavigate,
    )
}

@Composable
fun ArchiveScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    Surface {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Archive", onBack = onBack)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            ) {
                items(DemoContent.profileGrid) { url ->
                    GridTile(url = url, onClick = { onNavigate(Routes.postDetail("post-1")) })
                }
            }
        }
    }
}

@Composable
fun RecentlyDeletedScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Recently deleted",
        subtitle = "Items stay here for 30 days before being removed permanently.",
        onBack = onBack,
        actions = listOf("Back to settings" to Routes.SETTINGS),
        onNavigate = onNavigate,
    )
}

@Composable
fun PrivacySettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Privacy",
        subtitle = "Account visibility, story sharing, tags and mentions.",
        onBack = onBack,
        actions = listOf("Blocked accounts" to Routes.SETTINGS_BLOCKED),
        onNavigate = onNavigate,
    )
}

@Composable
fun SecuritySettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Security",
        subtitle = "Password, two factor authentication and login activity.",
        onBack = onBack,
        actions = listOf("App lock" to Routes.APP_LOCK),
        onNavigate = onNavigate,
    )
}

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Notifications",
        subtitle = "Choose what you are alerted about and when.",
        onBack = onBack,
        actions = listOf("Back to settings" to Routes.SETTINGS),
        onNavigate = onNavigate,
    )
}

@Composable
fun AppearanceSettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Appearance",
        subtitle = "Theme, dynamic colour and text size preferences.",
        onBack = onBack,
        actions = listOf("Accessibility" to Routes.SETTINGS_ACCESSIBILITY),
        onNavigate = onNavigate,
    )
}

@Composable
fun DataSettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Data usage",
        subtitle = "Media quality, downloads and offline cache limits.",
        onBack = onBack,
        actions = listOf("Back to settings" to Routes.SETTINGS),
        onNavigate = onNavigate,
    )
}

@Composable
fun BlockedSettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Blocked accounts",
        subtitle = "People you blocked cannot find your profile or content.",
        onBack = onBack,
        actions = listOf("Back to privacy" to Routes.SETTINGS_PRIVACY),
        onNavigate = onNavigate,
    )
}

@Composable
fun AccessibilitySettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Accessibility",
        subtitle = "Reduced motion, high contrast and caption preferences.",
        onBack = onBack,
        actions = listOf("Back to appearance" to Routes.SETTINGS_APPEARANCE),
        onNavigate = onNavigate,
    )
}

@Composable
fun AboutSettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "About",
        subtitle = "Version, licences and community guidelines.",
        onBack = onBack,
        actions = listOf("Back to settings" to Routes.SETTINGS),
        onNavigate = onNavigate,
    )
}

@Composable
fun StoryCreatorScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    Surface {
        Box(modifier = Modifier.fillMaxSize()) {
            NetworkImage(
                url = DemoContent.exploreTiles.first().image,
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 18.dp),
            ) {
                listOf(
                    "Audio" to Icons.Outlined.MusicNote,
                    "Align" to Icons.Outlined.Tune,
                    "Speed" to Icons.Outlined.Speed,
                    "Effects" to Icons.Outlined.FilterVintage,
                    "Timer" to Icons.Outlined.Timer,
                ).forEach { (label, icon) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(text = label, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 26.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(InsangramPalette.Pink)
                        .border(4.dp, Color.White, CircleShape)
                        .clickable { onNavigate(Routes.EDITOR) },
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    listOf("Story", "Post", "Reel", "Live").forEach { mode ->
                        Text(text = mode, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
fun EditorScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "Editor",
        subtitle = "Crop, filter and adjust before sharing your post.",
        onBack = onBack,
        actions = listOf("Share now" to Routes.HOME),
        onNavigate = onNavigate,
    )
}

@Composable
fun AppLockScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    DetailScreen(
        title = "App lock",
        subtitle = "Require biometrics or a PIN each time Insangram opens.",
        onBack = onBack,
        actions = listOf("Back to security" to Routes.SETTINGS_SECURITY),
        onNavigate = onNavigate,
    )
}
