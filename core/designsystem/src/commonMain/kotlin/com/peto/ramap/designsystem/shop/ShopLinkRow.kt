package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.SocialColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.kakao_map_icon
import ramap.shared.generated.resources.naver_map_icon

@Composable
internal fun ShopLinkRow(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = GrayColor.C050,
    contentColor: Color = GrayColor.C500,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Row(
        modifier =
            modifier
                .clip(shape)
                .background(containerColor)
                .noRippleClickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        AppText(
            text = label,
            style = AppTextStyle.B1,
            color = contentColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopLinkRowPreview() {
    RamapTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShopLinkRow(
                icon = Res.drawable.ic_close,
                label = "닫기",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShopLinkRow(
                    icon = Res.drawable.kakao_map_icon,
                    label = "카카오 지도",
                    containerColor = SocialColor.Kakao,
                    contentColor = GrayColor.C500,
                    shape = RoundedCornerShape(100.dp),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                ShopLinkRow(
                    icon = Res.drawable.naver_map_icon,
                    label = "네이버 지도",
                    containerColor = SocialColor.Naver,
                    contentColor = CommonColor.White,
                    shape = RoundedCornerShape(100.dp),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
