package com.peto.ramap.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.laundry_gothic_bold
import ramap.shared.generated.resources.laundry_gothic_regular
import ramap.shared.generated.resources.satoshi_variable

@Composable
internal fun provideLaundryGothicFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.laundry_gothic_regular, FontWeight.Normal),
        Font(Res.font.laundry_gothic_bold, FontWeight.Bold),
    )

@Composable
internal fun provideSatoshiFamily(): FontFamily =
    FontFamily(
        Font(Res.font.satoshi_variable, FontWeight.Light),
        Font(Res.font.satoshi_variable, FontWeight.Normal),
        Font(Res.font.satoshi_variable, FontWeight.Medium),
        Font(Res.font.satoshi_variable, FontWeight.SemiBold),
        Font(Res.font.satoshi_variable, FontWeight.Bold),
        Font(Res.font.satoshi_variable, FontWeight.ExtraBold),
        Font(Res.font.satoshi_variable, FontWeight.Black),
    )
