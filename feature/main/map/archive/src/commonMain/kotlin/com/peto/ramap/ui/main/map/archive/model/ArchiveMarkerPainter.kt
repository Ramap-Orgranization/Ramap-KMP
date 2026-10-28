package com.peto.ramap.ui.main.map.archive.model

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter

internal class ArchiveMarkerPainter(
    private val shape: Painter,
    private val photo: Painter?,
) : Painter() {
    override val intrinsicSize =
        if (photo == null) {
            Size(CHECK_WIDTH_DP, CHECK_HEIGHT_DP)
        } else {
            Size(PHOTO_WIDTH_DP, PHOTO_HEIGHT_DP)
        }

    override fun DrawScope.onDraw() {
        if (photo == null) {
            with(shape) { draw(size = this@onDraw.size) }
            return
        }
        val markerSize = intrinsicSize
        scale(
            scaleX = size.width / intrinsicSize.width,
            scaleY = size.height / intrinsicSize.height,
            pivot = Offset.Zero,
        ) {
            drawPhoto(photo)
            with(shape) { draw(size = markerSize) }
        }
    }

    private fun DrawScope.drawPhoto(image: Painter) {
        val clip =
            Path().apply {
                addRoundRect(RoundRect(Rect(7f, 7f, 65f, 65f), CornerRadius(14f)))
            }
        clipPath(clip) {
            val imageSize = image.intrinsicSize
            val cropScale = maxOf(58f / imageSize.width, 58f / imageSize.height)
            val width = imageSize.width * cropScale
            val height = imageSize.height * cropScale
            translate(left = 7f + (58f - width) / 2, top = 7f + (58f - height) / 2) {
                with(image) { draw(size = Size(width, height)) }
            }
        }
    }

    private companion object {
        const val CHECK_WIDTH_DP = 32f
        const val CHECK_HEIGHT_DP = 40f
        const val PHOTO_WIDTH_DP = 72f
        const val PHOTO_HEIGHT_DP = 82f
    }
}
