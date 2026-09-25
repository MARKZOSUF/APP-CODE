package com.insangram.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val InsangramShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

/** Named shapes for components that are not part of the Material scale. */
object InsangramComponentShapes {
    val button = RoundedCornerShape(14.dp)
    val pill = RoundedCornerShape(percent = 50)
    val input = RoundedCornerShape(14.dp)
    val card = RoundedCornerShape(20.dp)
    val mediaTile = RoundedCornerShape(4.dp)
    val bottomSheet = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    val dialog = RoundedCornerShape(26.dp)
    val chip = RoundedCornerShape(12.dp)
    val bubbleOutgoing = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
    val bubbleIncoming = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
}

/**
 * 4dp-based spacing scale. Exposed through a CompositionLocal so screens never
 * invent one-off paddings.
 */
data class InsangramSpacing(
    val none: Dp = 0.dp,
    val hair: Dp = 2.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
    val screenHorizontal: Dp = 16.dp,
    val minTouchTarget: Dp = 48.dp,
    val avatarSmall: Dp = 32.dp,
    val avatarMedium: Dp = 44.dp,
    val avatarLarge: Dp = 64.dp,
    val avatarProfile: Dp = 92.dp,
    val storyRing: Dp = 68.dp,
    val bottomBarHeight: Dp = 60.dp,
)

val LocalInsangramSpacing = staticCompositionLocalOf { InsangramSpacing() }
