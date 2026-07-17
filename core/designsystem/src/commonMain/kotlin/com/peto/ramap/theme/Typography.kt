package com.peto.ramap.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.satoshi_variable

@Immutable
data class AppTypography(
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
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
)

@Composable
fun provideAppTypography(): AppTypography {
    val satoshiFamily =
        FontFamily(
            Font(Res.font.satoshi_variable, FontWeight.Light),
            Font(Res.font.satoshi_variable, FontWeight.Normal),
            Font(Res.font.satoshi_variable, FontWeight.Medium),
            Font(Res.font.satoshi_variable, FontWeight.SemiBold),
            Font(Res.font.satoshi_variable, FontWeight.Bold),
            Font(Res.font.satoshi_variable, FontWeight.ExtraBold),
            Font(Res.font.satoshi_variable, FontWeight.Black),
        )

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
    )
}

fun AppTextStyle.toTextStyle(typography: AppTypography): TextStyle =
    when (this) {
        AppTextStyle.H1 -> typography.h1
        AppTextStyle.H2 -> typography.h2
        AppTextStyle.H3 -> typography.h3
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
    }

private fun lineHeightPercent(
    fontSizeSp: Float,
    percent: Float,
): TextUnit = (fontSizeSp * (percent / 100f)).sp

private fun letterSpacingPercent(
    fontSizeSp: Float,
    percent: Float,
): TextUnit = (fontSizeSp * (percent / 100f)).sp
