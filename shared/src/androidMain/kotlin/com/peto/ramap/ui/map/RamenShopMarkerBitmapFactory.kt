package com.peto.ramap.ui.map

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.peto.ramap.extension.toPx

/**
 * Compose resource로 제공되는 매장 마커 Painter를 KakaoMap label style에 사용할 Bitmap으로 변환한다.
 */
internal class RamenShopMarkerBitmapFactory {
    fun create(
        painter: Painter,
        density: Density,
        width: Int,
        height: Int,
    ): Bitmap {
        val bitmap = createBitmap(width, height, density)
        val imageBitmap = bitmap.asImageBitmap()

        drawPainter(
            painter = painter,
            density = density,
            imageBitmap = imageBitmap,
        )

        return bitmap
    }

    private fun createBitmap(
        width: Int,
        height: Int,
        density: Density,
    ): Bitmap =
        Bitmap.createBitmap(
            width.toPx(density),
            height.toPx(density),
            Bitmap.Config.ARGB_8888,
        )

    private fun drawPainter(
        painter: Painter,
        density: Density,
        imageBitmap: ImageBitmap,
    ) {
        CanvasDrawScope().draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(imageBitmap),
            size = imageBitmap.size,
        ) {
            with(painter) {
                draw(size = size)
            }
        }
    }

    private val ImageBitmap.size: Size
        get() =
            Size(
                width = width.toFloat(),
                height = height.toFloat(),
            )
}
