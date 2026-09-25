package com.insangram.app.feature.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.insangram.app.core.designsystem.theme.InsangramGradients
import com.insangram.app.core.designsystem.theme.InsangramPalette

/**
 * Shared visual building blocks for the Insangram surface: media tiles, story
 * rings, feed cards and the small chrome pieces every screen repeats.
 */

/** Image with a themed placeholder behind it so layout never jumps. */
@Composable
fun NetworkImage(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(modifier = modifier.background(InsangramPalette.SurfaceDarkElevated)) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Circular avatar wrapped in the unseen-story gradient ring. */
@Composable
fun StoryRing(
    avatarUrl: String,
    size: Int = 64,
    ringed: Boolean = true,
    seen: Boolean = false,
    showAdd: Boolean = false,
    onClick: () -> Unit = {},
) {
    Box(contentAlignment = Alignment.BottomEnd) {
        val ring = Modifier
            .size(size.dp)
            .then(
                when {
                    !ringed -> Modifier
                    seen -> Modifier.border(2.dp, InsangramPalette.OutlineDark, CircleShape)
                    else -> Modifier.border(2.dp, InsangramGradients.storyUnseen, CircleShape)
                },
            )
            .padding(3.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)

        NetworkImage(url = avatarUrl, modifier = ring.clip(CircleShape))

        if (showAdd) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(InsangramPalette.Violet)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

/** Story tray entry: ring plus the username caption. */
@Composable
fun StoryItem(
    label: String,
    avatarUrl: String,
    isViewer: Boolean = false,
    seen: Boolean = false,
    onClick: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp),
    ) {
        StoryRing(
            avatarUrl = avatarUrl,
            size = 66,
            ringed = !isViewer,
            seen = seen,
            showAdd = isViewer,
            onClick = onClick,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/** Full-bleed brand gradient button used for the primary auth actions. */
@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(InsangramGradients.brand)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

/** Small pill used for Explore filters and profile tabs. */
@Composable
fun FilterChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) InsangramPalette.Violet else InsangramPalette.SurfaceDarkElevated,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Outlined secondary action such as Follow or Edit Profile. */
@Composable
fun OutlinedPill(
    label: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (filled) InsangramPalette.Violet else InsangramPalette.SurfaceDarkElevated,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

/** A single feed post: header, media, action row, likes and caption. */
@Composable
fun PostCard(
    post: DemoPost,
    onOpenProfile: () -> Unit = {},
    onOpenComments: () -> Unit = {},
    onLike: (Boolean) -> Unit = {},
    onSave: (Boolean) -> Unit = {},
    onShare: () -> Unit = {},
    initiallySaved: Boolean = false,
) {
    var liked by remember(post.id, post.liked) { mutableStateOf(post.liked) }
    var saved by remember(post.id, initiallySaved) { mutableStateOf(initiallySaved) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            StoryRing(avatarUrl = post.author.avatar, size = 38, onClick = onOpenProfile)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.author.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (post.location.isNotBlank()) {
                    Text(
                        text = post.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = {}) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
            }
        }

        Box {
            NetworkImage(
                url = post.image,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp)),
            )
            if (post.pageCount > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "1/${post.pageCount}",
                        color = Color.White,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            IconButton(onClick = {
                liked = !liked
                onLike(liked)
            }) {
                Icon(
                    imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (liked) InsangramPalette.LikeRed else MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onOpenComments) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comments")
            }
            IconButton(onClick = onShare) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Share")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                saved = !saved
                onSave(saved)
            }) {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save",
                    tint = if (saved) InsangramPalette.Violet else MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            Text(
                text = "${post.likes} likes",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${post.author.username}  ${post.caption}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${post.hashtags} More",
                style = MaterialTheme.typography.bodySmall,
                color = InsangramPalette.Info,
            )
        }
        Spacer(Modifier.height(14.dp))
    }
}

/** Header used by every non-tab screen: back arrow, title, optional action. */
@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) trailing()
    }
}

/** Settings-style row with a leading icon and a chevron affordance. */
@Composable
fun ListRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingLabel: String? = null,
    onClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (trailingLabel != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(InsangramPalette.Info)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(text = trailingLabel, color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

/** Bottom-anchored gradient scrim used over full-bleed media. */
fun scrimBrush(): Brush = InsangramGradients.darkScrim

/** Compact metric column used by the profile header. */
@Composable
fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Simple square media tile used by Explore, Saved and profile grids. */
@Composable
fun GridTile(url: String, tag: String? = null, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
    ) {
        NetworkImage(url = url, modifier = Modifier.fillMaxSize())
        if (tag != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(InsangramGradients.darkScrim)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(text = tag, color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

/** Shared vertical rhythm for stacked sections. */
val SectionSpacing: Arrangement.Vertical = Arrangement.spacedBy(14.dp)
