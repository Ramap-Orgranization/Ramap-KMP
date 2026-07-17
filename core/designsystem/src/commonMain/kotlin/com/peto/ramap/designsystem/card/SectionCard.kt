package com.peto.ramap.designsystem.card

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CommonColor.White,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 15.dp),
        ) {
            title?.let {
                AppText(
                    text = title,
                    style = AppTextStyle.T1,
                    color = GrayColor.C500,
                    modifier =
                        Modifier
                            .padding(top = 15.dp),
                )
            }
            content()
        }
    }
}
