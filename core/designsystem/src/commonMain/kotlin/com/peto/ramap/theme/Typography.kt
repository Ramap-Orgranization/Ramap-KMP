package com.peto.ramap.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Immutable
data class AppTypography(
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val h4: TextStyle,
    val h3Brand: TextStyle,
    val h4Brand: TextStyle,
    val t1: TextStyle,
    val t2: TextStyle,
    val t3: TextStyle,
    val b1: TextStyle,
    val b2: TextStyle,
    val b3: TextStyle,
    val b4: TextStyle,
    val c1: TextStyle,
    val c2: TextStyle,
    val c3: TextStyle,
    val l1: TextStyle,
    val l2: TextStyle,
    val l3: TextStyle,
)

@Composable
internal fun provideAppTypography(): AppTypography {
    val satoshiFamily = provideSatoshiFamily()
    val laundryGothicFamily = provideLaundryGothicFontFamily()

    return AppTypography(
        h1 =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = lineHeightPercent(28f, 140f),
                letterSpacing = letterSpacingPercent(28f, -1f),
            ),
        h2 =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                lineHeight = lineHeightPercent(24f, 140f),
                letterSpacing = letterSpacingPercent(24f, -1f),
            ),
        h3 =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                lineHeight = lineHeightPercent(22f, 140f),
                letterSpacing = letterSpacingPercent(22f, -1f),
            ),
        h4 =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = lineHeightPercent(18f, 140f),
                letterSpacing = letterSpacingPercent(18f, -2f),
            ),
        h3Brand =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = lineHeightPercent(20f, 140f),
                letterSpacing = letterSpacingPercent(20f, -2f),
            ),
        h4Brand =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = lineHeightPercent(20f, 140f),
                letterSpacing = letterSpacingPercent(20f, -2f),
            ),
        t1 =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                lineHeight = lineHeightPercent(18f, 150f),
                letterSpacing = letterSpacingPercent(18f, -2f),
            ),
        t2 =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = lineHeightPercent(16f, 150f),
                letterSpacing = letterSpacingPercent(16f, -2f),
            ),
        t3 =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                lineHeight = lineHeightPercent(14f, 150f),
                letterSpacing = letterSpacingPercent(14f, -1f),
            ),
        b1 =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = lineHeightPercent(14f, 150f),
                letterSpacing = letterSpacingPercent(14f, -2.5f),
            ),
        b2 =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = lineHeightPercent(14f, 150f),
                letterSpacing = letterSpacingPercent(14f, -2.5f),
            ),
        b3 =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                lineHeight = lineHeightPercent(12f, 150f),
                letterSpacing = letterSpacingPercent(12f, -1f),
            ),
        b4 =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = lineHeightPercent(12f, 150f),
                letterSpacing = letterSpacingPercent(12f, -2.5f),
            ),
        c1 =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = lineHeightPercent(12f, 150f),
                letterSpacing = letterSpacingPercent(12f, -2.5f),
            ),
        c2 =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = lineHeightPercent(11f, 150f),
                letterSpacing = letterSpacingPercent(11f, -2.5f),
            ),
        c3 =
            TextStyle(
                fontFamily = satoshiFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp,
                lineHeight = lineHeightPercent(10f, 150f),
                letterSpacing = letterSpacingPercent(10f, -2.5f),
            ),
        l1 =
            TextStyle(
                fontFamily = laundryGothicFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = lineHeightPercent(20f, 140f),
                letterSpacing = letterSpacingPercent(20f, -2f),
            ),
        l2 =
            TextStyle(
                fontFamily = laundryGothicFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = lineHeightPercent(16f, 150f),
                letterSpacing = letterSpacingPercent(16f, -2f),
            ),
        l3 =
            TextStyle(
                fontFamily = laundryGothicFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = lineHeightPercent(12f, 150f),
                letterSpacing = letterSpacingPercent(12f, -2.5f),
            ),
    )
}

internal fun AppTextStyle.toTextStyle(typography: AppTypography): TextStyle =
    when (this) {
        AppTextStyle.H1 -> typography.h1
        AppTextStyle.H2 -> typography.h2
        AppTextStyle.H3 -> typography.h3
        AppTextStyle.H4 -> typography.h4
        AppTextStyle.H3Brand -> typography.h3Brand
        AppTextStyle.H4Brand -> typography.h4Brand
        AppTextStyle.T1 -> typography.t1
        AppTextStyle.T2 -> typography.t2
        AppTextStyle.T3 -> typography.t3
        AppTextStyle.B1 -> typography.b1
        AppTextStyle.B2 -> typography.b2
        AppTextStyle.B3 -> typography.b3
        AppTextStyle.B4 -> typography.b4
        AppTextStyle.C1 -> typography.c1
        AppTextStyle.C2 -> typography.c2
        AppTextStyle.C3 -> typography.c3
        AppTextStyle.L1 -> typography.l1
        AppTextStyle.L2 -> typography.l2
        AppTextStyle.L3 -> typography.l3
    }

private fun lineHeightPercent(
    fontSizeSp: Float,
    percent: Float,
): TextUnit = (fontSizeSp * (percent / 100f)).sp

private fun letterSpacingPercent(
    fontSizeSp: Float,
    percent: Float,
): TextUnit = (fontSizeSp * (percent / 100f)).sp
