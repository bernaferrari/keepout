package com.bernaferrari.bravedns.ui.icons

// The app's "none" glyph: a dashed ring, an empty slot rather than a crossed-out or switched-off
// one, so "no rule" / "no app" never reads as blocked or disabled. Same 10dp footprint as the
// filled circle glyphs (DoNotDisturbOn, CheckCircle) so it sits in their family.

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val filledDashedCircle: ImageVector by lazy {
    ImageVector.Builder(
        name = "dashed_circle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Eight 30° dashes with 15° gaps on a 9dp radius; a 2dp round-capped stroke.
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
            moveTo(13.17f, 3.08f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 17.48f, y1 = 4.86f)
            moveTo(19.14f, 6.52f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 20.92f, y1 = 10.83f)
            moveTo(20.92f, 13.17f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 19.14f, y1 = 17.48f)
            moveTo(17.48f, 19.14f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 13.17f, y1 = 20.92f)
            moveTo(10.83f, 20.92f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 6.52f, y1 = 19.14f)
            moveTo(4.86f, 17.48f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 3.08f, y1 = 13.17f)
            moveTo(3.08f, 10.83f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 4.86f, y1 = 6.52f)
            moveTo(6.52f, 4.86f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 10.83f, y1 = 3.08f)
        }
    }.build()
}
