package com.peto.ramap.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/**
 * 현재 위치를 표시하는 파란 원형 마커 비트맵을 생성한다.
 */
internal class MyLocationMarkerBitmapFactory {
    private var cachedBitmap: Bitmap? = null

    fun create(): Bitmap =
        cachedBitmap ?: drawMarkerBitmap().also { bitmap ->
            cachedBitmap = bitmap
        }

    private fun drawMarkerBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(MARKER_SIZE, MARKER_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = MARKER_SIZE / 2f

        canvas.drawCircle(center, center, OUTER_RADIUS, outerPaint())
        canvas.drawCircle(center, center, INNER_RADIUS, innerPaint())

        return bitmap
    }

    private fun outerPaint(): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OUTER_COLOR
        }

    private fun innerPaint(): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INNER_COLOR
            style = Paint.Style.FILL
        }

    private companion object {
        private const val MARKER_SIZE = 34
        private const val OUTER_RADIUS = 15f
        private const val INNER_RADIUS = 10f
        private const val OUTER_COLOR = 0xFFFFFFFF.toInt()
        private const val INNER_COLOR = 0xFF2F80ED.toInt()
    }
}
