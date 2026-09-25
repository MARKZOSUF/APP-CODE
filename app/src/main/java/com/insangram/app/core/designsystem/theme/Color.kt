package com.insangram.app.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Brand palette sampled directly from the supplied Insangram logo: a
 * purple -> magenta -> pink -> orange -> yellow ramp on near-black.
 */
object InsangramPalette {
    val Purple = Color(0xFF8A2BE2)
    val Violet = Color(0xFFA644F0)
    val Indigo = Color(0xFF5B2BD6)
    val Magenta = Color(0xFFE1306C)
    val Pink = Color(0xFFFF3D77)
    val Rose = Color(0xFFFF5C8A)
    val Orange = Color(0xFFFF7A3D)
    val Amber = Color(0xFFFF9F1C)
    val Yellow = Color(0xFFFFCF33)

    val Black = Color(0xFF000000)
    val NearBlack = Color(0xFF0A080E)
    val SurfaceDark = Color(0xFF121016)
    val SurfaceDarkElevated = Color(0xFF1B1822)
    val OutlineDark = Color(0xFF2E2A36)

    val White = Color(0xFFFFFFFF)
    val BackgroundLight = Color(0xFFFBF9FC)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceLightElevated = Color(0xFFF3EFF7)
    val OutlineLight = Color(0xFFDCD6E4)

    // Text colours chosen for >= 4.5:1 contrast against their own surface.
    val TextPrimaryDark = Color(0xFFF7F5FA)
    val TextSecondaryDark = Color(0xFFB6AFC4)
    val TextTertiaryDark = Color(0xFF8A8394)
    val TextPrimaryLight = Color(0xFF14111A)
    val TextSecondaryLight = Color(0xFF554D63)
    val TextTertiaryLight = Color(0xFF6F6880)

    val Success = Color(0xFF2ED47A)
    val Warning = Color(0xFFFFB020)
    val Danger = Color(0xFFFF4D4F)
    val Info = Color(0xFF3D9BFF)

    val LikeRed = Color(0xFFFF2D55)
    val OnlineGreen = Color(0xFF34C759)
}

/** Reusable brand gradients. Never used as the only carrier of meaning. */
object InsangramGradients {
    val brand = Brush.linearGradient(
        listOf(
            InsangramPalette.Purple,
            InsangramPalette.Magenta,
            InsangramPalette.Orange,
            InsangramPalette.Yellow,
        ),
    )

    val storyUnseen = Brush.sweepGradient(
        listOf(
            InsangramPalette.Yellow,
            InsangramPalette.Orange,
            InsangramPalette.Pink,
            InsangramPalette.Magenta,
            InsangramPalette.Purple,
            InsangramPalette.Yellow,
        ),
    )

    val closeFriends = Brush.sweepGradient(
        listOf(InsangramPalette.Success, Color(0xFF7BE495), InsangramPalette.Success),
    )

    val createButton = Brush.linearGradient(
        listOf(InsangramPalette.Violet, InsangramPalette.Magenta, InsangramPalette.Amber),
    )

    val darkScrim = Brush.verticalGradient(
        listOf(Color(0x00000000), Color(0xCC000000)),
    )

    val skeletonDark = Brush.horizontalGradient(
        listOf(Color(0xFF1B1822), Color(0xFF272231), Color(0xFF1B1822)),
    )

    val skeletonLight = Brush.horizontalGradient(
        listOf(Color(0xFFEDE8F2), Color(0xFFF7F4FA), Color(0xFFEDE8F2)),
    )
}
