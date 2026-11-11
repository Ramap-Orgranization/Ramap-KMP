package com.peto.ramap.designsystem.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.profile.ProfileVisitedShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.profile_visit_count

@Composable
fun ProfileVisitRankingShopSummary(
    profileVisitedShop: ProfileVisitedShop,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    imageShape: Shape = RoundedCornerShape(8.dp),
    imageUrl: String? = null,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onClick != null)
                    Modifier.noRippleClickable(onClick = onClick) else Modifier)
                .background(CommonColor.White),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(end = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteShopImage(
                url = imageUrl,
                shape = imageShape,
                modifier = Modifier.size(60.dp),
            )
            AppText(
                text = profileVisitedShop.name,
                style = AppTextStyle.B1,
                color = GrayColor.C500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        AppText(
            text = stringResource(Res.string.profile_visit_count, profileVisitedShop.visitCount),
            style = AppTextStyle.C1,
            color = GrayColor.C500,
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
        )
    }
}
