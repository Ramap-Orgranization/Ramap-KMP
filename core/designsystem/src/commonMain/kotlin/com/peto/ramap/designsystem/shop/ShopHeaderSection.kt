package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.MenuCategoryLabels
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.designsystem.resource.format
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_kid_star_filled
import ramap.shared.generated.resources.shop_detail_copy_address
import ramap.shared.generated.resources.shop_detail_label_address

@Composable
internal fun ShopHeaderSection(
    shop: RamenShop,
    likeCount: Long,
    isBookmarked: Boolean,
    isNotificationEnabled: Boolean,
    showNotificationActions: Boolean,
    isHidden: Boolean,
    event: ShopEvent?,
    modifier: Modifier = Modifier,
    dragAreaModifier: Modifier = Modifier,
    onBookmarkClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onHiddenClick: () -> Unit,
    onReportClick: () -> Unit,
    onShareClick: () -> Unit,
    onEventClick: (ShopEvent) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = dragAreaModifier,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            event?.let { shopEvent ->
                ShopEventResourceMapper.notice(shopEvent)?.let { notice ->
                    AppText(
                        text = notice.format(),
                        modifier =
                            Modifier
                                .padding(top = 5.dp)
                                .padding(horizontal = 24.dp)
                                .noRippleClickable { onEventClick(shopEvent) },
                        style = AppTextStyle.B1,
                        color = SystemColor.Warning,
                    )
                }
            }
            Column {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RemoteShopImage(
                        url = shop.instagramProfileImageUrl,
                        modifier =
                            Modifier
                                .align(Alignment.CenterVertically)
                                .border(
                                    width = 1.dp,
                                    color = GrayColor.C100,
                                    shape = RoundedCornerShape(999.dp),
                                ).size(45.dp)
                                .clip(CircleShape),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        AppText(
                            text = shop.name,
                            style = AppTextStyle.H4,
                            color = GrayColor.C500,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.ic_kid_star_filled),
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                colorFilter = ColorFilter.tint(GrayColor.C400),
                            )
                            AppText(
                                text = "$likeCount",
                                style = AppTextStyle.B1,
                                color = GrayColor.C400,
                            )
                        }
                    }

                    ShopOverflowMenu(
                        shopId = shop.id,
                        isBookmarked = isBookmarked,
                        isNotificationEnabled = isNotificationEnabled,
                        showNotificationActions = showNotificationActions,
                        isHidden = isHidden,
                        onBookmarkClick = onBookmarkClick,
                        onNotificationClick = onNotificationClick,
                        onHiddenClick = onHiddenClick,
                        onReportClick = onReportClick,
                        onShareClick = onShareClick,
                    )
                }

                MenuCategoryLabels(
                    menuCategories = shop.menuCategories,
                    categoryLabel = { category ->
                        stringResource(
                            CategoryResourceMapper.label(
                                category,
                            ),
                        )
                    },
                    style = AppTextStyle.B1,
                    modifier =
                        Modifier
                            .padding(top = 10.dp)
                            .padding(horizontal = 20.dp),
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                ShopInfoRow(
                    label = stringResource(Res.string.shop_detail_label_address),
                    value = shop.address,
                    onClick = { clipboardManager.setText(AnnotatedString(shop.address)) },
                    onClickLabel = stringResource(Res.string.shop_detail_copy_address),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopHeaderSectionPreview(
    @PreviewParameter(RamenShopPreviewParameterProvider::class) shop: RamenShop,
) {
    RamapTheme {
        ShopHeaderSection(
            shop = shop,
            likeCount = 42L,
            isBookmarked = false,
            isNotificationEnabled = false,
            showNotificationActions = true,
            isHidden = false,
            event = null,
            onBookmarkClick = {},
            onNotificationClick = {},
            onHiddenClick = {},
            onReportClick = {},
            onShareClick = {},
            onEventClick = {},
        )
    }
}
