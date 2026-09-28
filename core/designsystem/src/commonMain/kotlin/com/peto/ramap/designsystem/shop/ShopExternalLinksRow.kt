package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.resource.wating.WaitingSystemUiModel
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.MapColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.apple_maps_icon
import ramap.shared.generated.resources.instagram_icon
import ramap.shared.generated.resources.kakao_map_icon
import ramap.shared.generated.resources.naver_map_icon
import ramap.shared.generated.resources.shop_detail_label_waiting
import ramap.shared.generated.resources.shop_detail_link_apple_maps
import ramap.shared.generated.resources.shop_detail_link_instagram
import ramap.shared.generated.resources.shop_detail_link_kakao_map
import ramap.shared.generated.resources.shop_detail_link_naver_map

@Composable
internal fun ShopExternalLinksRow(
    shop: RamenShop,
    waitingSystem: WaitingSystemUiModel?,
    isAppleMapsAvailable: Boolean,
    onMapLinkClick: (String) -> Unit,
    onWaitingClick: (String) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onAppleMapsClick: (RamenShop) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = 20.dp),
    ) {
        if (shop.instagramUrl != null || waitingSystem != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                shop.instagramUrl?.let { url ->
                    ShopLinkRow(
                        icon = Res.drawable.instagram_icon,
                        label = stringResource(Res.string.shop_detail_link_instagram),
                        onClick = { onExternalLinkClick(url) },
                        modifier = Modifier.weight(1f),
                    )
                }
                waitingSystem?.let { waiting ->
                    ShopLinkRow(
                        label = stringResource(Res.string.shop_detail_label_waiting),
                        icon = waiting.icon,
                        onClick = { onWaitingClick(waiting.providerUrl) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (shop.kakaoPlaceUrl != null || shop.naverPlaceUrl != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                shop.kakaoPlaceUrl?.let { url ->
                    ShopLinkRow(
                        icon = Res.drawable.kakao_map_icon,
                        label = stringResource(Res.string.shop_detail_link_kakao_map),
                        containerColor = MapColor.Kakao,
                        contentColor = GrayColor.C500,
                        shape = RoundedCornerShape(100.dp),
                        onClick = {
                            onMapLinkClick("kakao")
                            onExternalLinkClick(url)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                shop.naverPlaceUrl?.let { url ->
                    ShopLinkRow(
                        icon = Res.drawable.naver_map_icon,
                        label = stringResource(Res.string.shop_detail_link_naver_map),
                        containerColor = MapColor.Naver,
                        contentColor = CommonColor.White,
                        shape = RoundedCornerShape(100.dp),
                        onClick = {
                            onMapLinkClick("naver")
                            onExternalLinkClick(url)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (isAppleMapsAvailable) {
            ShopLinkRow(
                icon = Res.drawable.apple_maps_icon,
                label = stringResource(Res.string.shop_detail_link_apple_maps),
                onClick = {
                    onMapLinkClick("apple")
                    onAppleMapsClick(shop)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
