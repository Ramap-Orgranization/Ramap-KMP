package com.peto.ramap.ui.main.ranking.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.rank.RankedShop
import com.peto.ramap.domain.model.rank.ShopRanking
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.preview.RamenShopPreviewParameterProvider
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmarked_shops_toggle
import ramap.shared.generated.resources.ic_kid_star_filled
import ramap.shared.generated.resources.ranking_bookmark_cancel_action

@Composable
internal fun RankingRow(
    item: RankedShop,
    isBookmarked: Boolean,
    likeCount: Long,
    onBookmarkClick: () -> Unit,
    onClick: () -> Unit,
) {
    val shop = item.ranking.shop
    val bookmarkDescription =
        stringResource(
            if (isBookmarked) {
                Res.string.ranking_bookmark_cancel_action
            } else {
                Res.string.bookmarked_shops_toggle
            },
        )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .noRippleClickable(onClick = onClick)
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppText(
            text = item.rank?.toString() ?: "-",
            style = AppTextStyle.H3,
            color = GrayColor.C500,
        )

        RemoteShopImage(
            url = shop.instagramProfileImageUrl,
            modifier = Modifier.size(55.dp),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            AppText(
                text = shop.name,
                style = AppTextStyle.T2,
                color = GrayColor.C500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AppText(
                text = shop.address,
                style = AppTextStyle.B4,
                color = GrayColor.C300,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val bookmarkColor = if (isBookmarked) InstagramColor.Pink else GrayColor.C500
            IconButton(
                onClick = onBookmarkClick,
                modifier =
                    Modifier
                        .size(48.dp)
                        .semantics { contentDescription = bookmarkDescription },
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_kid_star_filled),
                    contentDescription = null,
                    modifier = Modifier.size(25.dp),
                    colorFilter = ColorFilter.tint(bookmarkColor),
                )
            }
            AppText(
                text = "$likeCount",
                style = AppTextStyle.L3,
                color = GrayColor.C400,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RankingRowPreview(
    @PreviewParameter(RamenShopPreviewParameterProvider::class) shop: RamenShop,
) {
    RamapTheme {
        RankingRow(
            item =
                RankedShop(
                    rank = 1,
                    ranking =
                        ShopRanking(
                            shop = shop,
                            likeCount = 128,
                        ),
                ),
            isBookmarked = true,
            likeCount = 128,
            onBookmarkClick = {},
            onClick = {},
        )
    }
}
