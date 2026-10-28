package com.peto.ramap.ui.main.map.archive

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import com.peto.ramap.ui.main.map.archive.model.ArchiveMarkerPainter
import kotlin.test.Test
import kotlin.test.assertEquals

class ArchiveMarkerPainterTest {
    @Test
    fun `체크 벡터의 픽셀 크기가 달라도 지도 마커의 논리 크기는 기존 마커와 같다`() {
        for (density in listOf(1f, 2f, 3f)) {
            val vector = painter(Size(32f * density, 40f * density))
            val marker = ArchiveMarkerPainter(shape = vector, photo = null)

            assertEquals(Size(32f, 40f), marker.intrinsicSize)
            assertEquals(Size(32f * density, 40f * density), marker.intrinsicSize * density)
        }
    }

    @Test
    fun `사진 마커는 프레임 벡터와 사진 해상도에 관계없이 동일한 논리 크기를 유지한다`() {
        val frame = painter(Size(216f, 246f))
        val photo = painter(Size(2000f, 1500f))

        assertEquals(Size(72f, 82f), ArchiveMarkerPainter(shape = frame, photo = photo).intrinsicSize)
    }

    private fun painter(size: Size): Painter =
        object : Painter() {
            override val intrinsicSize = size

            override fun DrawScope.onDraw() = Unit
        }
}
