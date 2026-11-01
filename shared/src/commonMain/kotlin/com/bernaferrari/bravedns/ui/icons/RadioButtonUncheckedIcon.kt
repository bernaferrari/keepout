package com.bernaferrari.bravedns.ui.icons

// Material Symbols "radio_button_unchecked": a 2dp ring, the same 10dp outer radius as the
// filled circle glyphs (DoNotDisturbOn, CheckCircle) so it sits in their family.

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val filledRadioButtonUnchecked: ImageVector by lazy {
    ImageVector.Builder(
        name = "radio_button_unchecked",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(12f, 2f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12f, y1 = 22f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12f, y1 = 2f)
            close()
            moveTo(12f, 4f)
            arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12f, y1 = 20f)
            arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12f, y1 = 4f)
            close()
        }
    }.build()
}
