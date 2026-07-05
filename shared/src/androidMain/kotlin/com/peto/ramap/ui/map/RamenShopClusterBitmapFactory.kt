package com.peto.ramap.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.peto.ramap.core.config.MarkerClusterConfig
import com.peto.ramap.core.config.MarkerConfig

/**
 * 클러스터 마커 리소스 이미지 위에 count 텍스트를 그린 비트맵을 생성하고 재사용한다.
 */
internal class RamenShopClusterBitmapFactory {
    private val cache = mutableMapOf<String, Bitmap>()

    fun create(
        count: Int,
        markerBitmap: Bitmap,
    ): Bitmap {
        val text = MarkerClusterConfig.countText(count)
        return cache.getOrPut(text) {
            drawClusterBitmap(
                text = text,
                markerBitmap = markerBitmap,
            )
        }
    }

    private fun drawClusterBitmap(
        text: String,
        markerBitmap: Bitmap,
    ): Bitmap {
        val bitmap = markerBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)

        drawText(canvas, text)

        return bitmap
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
    ) {
        val paint = createTextPaint()
        val x = canvas.width * CLUSTER_TEXT_CENTER_RATIO
        val y = canvas.height * CLUSTER_TEXT_CENTER_RATIO - (paint.descent() + paint.ascent()) / 2

        canvas.drawText(text, x, y, paint)
    }

    private fun createTextPaint(): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MarkerConfig.Cluster.TEXT_COLOR
            textAlign = Paint.Align.CENTER
            textSize = MarkerConfig.Cluster.TEXT_SIZE.toFloat()
            typeface = Typeface.DEFAULT_BOLD
        }

    private companion object {
        private const val CLUSTER_TEXT_CENTER_RATIO = 35f / 76f
    }
}
