package com.peto.ramap.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_close

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RamenShopSummary(
    shop: RamenShop,
    onClick: () -> Unit,
    categoryLabel: @Composable (Category) -> String,
    modifier: Modifier = Modifier,
    leadingContent: @Composable () -> Unit = {},
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .noRippleClickable(onClick = onClick)
                .background(CommonColor.White)
                .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingContent()
            RemoteShopImage(
                url = shop.instagramProfileImageUrl,
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(8.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AppText(
                    text = shop.name,
                    style = AppTextStyle.B1,
                    color = GrayColor.C500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AppText(
                    text = shop.address,
                    style = AppTextStyle.C1,
                    color = GrayColor.C300,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MenuCategoryLabels(
                    menuCategories = shop.menuCategories,
                    style = AppTextStyle.C1,
                    categoryLabel = categoryLabel,
                )
            }
        }

        RamenShopSummaryAction(
            actionLabel = actionLabel,
            onAction = onAction,
        )
    }
}

@Composable
private fun BoxScope.RamenShopSummaryAction(
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    if (actionLabel != null && onAction != null) {
        IconButton(
            onClick = onAction,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_close),
                contentDescription = actionLabel,
                modifier = Modifier.size(24.dp),
                tint = GrayColor.C400,
            )
        }
    }
}
