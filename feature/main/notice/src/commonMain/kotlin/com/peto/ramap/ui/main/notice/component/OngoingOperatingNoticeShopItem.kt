package com.peto.ramap.ui.main.notice.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.badge.NewsBadge
import com.peto.ramap.designsystem.component.ShopThumbnail
import com.peto.ramap.designsystem.resource.operatingnotice.ShopOperatingNoticeResourceMapper
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.preview.OperatingNoticePreviewParameterProvider
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OngoingOperatingNoticeShop(
    notice: OperatingNotice,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ShopThumbnail(
        imageUrl = notice.shop.instagramProfileImageUrl,
        name = notice.shop.name,
        modifier = modifier,
        onClick = onClick,
        badge = {
            NewsBadge(
                text = stringResource(ShopOperatingNoticeResourceMapper.typeLabel(notice.type)),
                modifier = Modifier.align(Alignment.BottomStart),
                containerColor = SystemColor.Warning,
                contentColor = CommonColor.White,
            )
        },
    )
}

@Preview
@Composable
private fun OngoingOperatingNoticeShopPreview() {
    val notices = OperatingNoticePreviewParameterProvider().values.first()
    RamapTheme {
        Row(
            modifier = Modifier.background(CommonColor.White).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            notices.forEach { notice ->
                OngoingOperatingNoticeShop(
                    notice = notice,
                ) {}
            }
        }
    }
}
