package com.peto.ramap.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

class BrandColorContrastTest {
    @Test
    fun `강조 글자는 밝은 화면과 카드와 배지 배경에서 읽을 수 있다`() {
        listOf(SurfaceColor.Background, CommonColor.White, BrandColor.SubtleFill).forEach { background ->
            assertContrast(foreground = BrandColor.Foreground, background = background, minimum = 4.5f)
        }
    }

    @Test
    fun `골드 배경 위 글자와 저장 별의 윤곽을 구별할 수 있다`() {
        assertContrast(foreground = BrandColor.OnFill, background = BrandColor.Fill, minimum = 4.5f)
        assertContrast(foreground = BrandColor.Foreground, background = BrandColor.Fill, minimum = 3f)
    }

    @Test
    fun `미저장 별은 화면과 카드 배경에서 구별할 수 있다`() {
        listOf(SurfaceColor.Background, CommonColor.White).forEach { background ->
            assertContrast(foreground = GrayColor.C300, background = background, minimum = 3f)
        }
    }

    private fun assertContrast(
        foreground: Color,
        background: Color,
        minimum: Float,
    ) {
        val foregroundLuminance = foreground.luminance()
        val backgroundLuminance = background.luminance()
        val contrast =
            (maxOf(foregroundLuminance, backgroundLuminance) + 0.05f) /
                (minOf(foregroundLuminance, backgroundLuminance) + 0.05f)

        assertTrue(contrast >= minimum, "색상 대비는 $minimum 이상이어야 합니다. 실제 대비 $contrast")
    }
}
