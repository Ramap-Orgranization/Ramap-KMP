package com.peto.ramap.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.peto.ramap.core.config.MarkerClusterConfig

/**
 * 클러스터 마커에 사용할 라운드 사각형 count 비트맵을 생성하고 재사용한다.
 */
internal class RamenShopClusterBitmapFactory {
    private val cache = mutableMapOf<String, Bitmap>()

    /**
     * 포함 매장 수에 대응하는 클러스터 마커 비트맵을 반환한다.
     */
    fun create(count: Int): Bitmap {
        val text = countText(count)
        return cache.getOrPut(text) {
            drawClusterBitmap(text)
        }
    }

    /**
     * 너무 큰 숫자는 고정 문구로 줄여 마커 안에서 읽히게 한다.
     */
    private fun countText(count: Int): String = MarkerClusterConfig.countText(count)

    /**
     * 라운드 사각형 배경과 count 텍스트가 들어간 클러스터 비트맵을 그린다.
     */
    private fun drawClusterBitmap(text: String): Bitmap {
        val bitmap = createBitmap()
        val canvas = Canvas(bitmap)
        val rect = createMarkerRect()

        drawShadow(canvas, rect)
        drawRoundedBackground(canvas, rect)
        drawText(canvas, text, rect)

        return bitmap
    }

    /**
     * 그림자를 포함할 수 있는 전체 비트맵 영역을 만든다.
     */
    private fun createBitmap(): Bitmap {
        val bitmapSize = MarkerConfig.Cluster.SIZE + SHADOW_PADDING * 2
        return Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
    }

    /**
     * 실제 빨간 마커가 그려질 라운드 사각형 영역을 만든다.
     */
    private fun createMarkerRect(): RectF {
        val markerSize = MarkerConfig.Cluster.SIZE.toFloat()
        return RectF(
            SHADOW_PADDING_FLOAT,
            SHADOW_PADDING_FLOAT,
            markerSize + SHADOW_PADDING,
            markerSize + SHADOW_PADDING,
        )
    }

    /**
     * 지도 위에서 마커가 살짝 떠 보이도록 부드러운 그림자를 그린다.
     */
    private fun drawShadow(
        canvas: Canvas,
        rect: RectF,
    ) {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = CLUSTER_SHADOW_COLOR
            }

        canvas.drawRoundRect(
            createShadowRect(rect),
            CLUSTER_CORNER_RADIUS,
            CLUSTER_CORNER_RADIUS,
            paint,
        )
    }

    /**
     * 사진과 같은 코랄 레드 라운드 사각형 배경을 그린다.
     */
    private fun drawRoundedBackground(
        canvas: Canvas,
        rect: RectF,
    ) {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = CLUSTER_BACKGROUND_COLOR
            }

        canvas.drawRoundRect(rect, CLUSTER_CORNER_RADIUS, CLUSTER_CORNER_RADIUS, paint)
    }

    /**
     * 클러스터에 포함된 매장 수를 마커 중앙에 굵은 흰색 텍스트로 그린다.
     */
    private fun drawText(
        canvas: Canvas,
        text: String,
        rect: RectF,
    ) {
        val paint = createTextPaint()
        val x = rect.centerX()
        val y = rect.centerY() - (paint.descent() + paint.ascent()) / 2

        canvas.drawText(text, x, y, paint)
    }

    /**
     * 클러스터 숫자 전용 텍스트 paint를 만든다.
     */
    private fun createTextPaint(): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MarkerConfig.Cluster.TEXT_COLOR
            textAlign = Paint.Align.CENTER
            textSize = MarkerConfig.Cluster.TEXT_SIZE.toFloat()
            typeface = Typeface.DEFAULT_BOLD
        }

    /**
     * 원본 rect를 변경하지 않고 그림자 위치만큼 이동한 rect를 반환한다.
     */
    private fun createShadowRect(rect: RectF): RectF =
        RectF(
            rect.left + SHADOW_OFFSET,
            rect.top + SHADOW_OFFSET,
            rect.right + SHADOW_OFFSET,
            rect.bottom + SHADOW_OFFSET,
        )

    companion object {
        private const val SHADOW_PADDING = 3
        private const val SHADOW_PADDING_FLOAT = SHADOW_PADDING.toFloat()
        private const val SHADOW_OFFSET = 2f
        private const val CLUSTER_CORNER_RADIUS = 15f
        private const val CLUSTER_BACKGROUND_COLOR = 0xFFFF564D.toInt()
        private const val CLUSTER_SHADOW_COLOR = 0x33000000
    }
}
