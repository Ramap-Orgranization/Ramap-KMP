package com.peto.ramap.designsystem.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.profile.ProfileVisitedShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
fun ProfileVisitRankingShopSummary(
    profileVisitedShop: ProfileVisitedShop,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(CommonColor.White),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteShopImage(
            url = profileVisitedShop.instagramProfileImageUrl,
            shape = CircleShape,
            modifier = Modifier.size(50.dp),
        )
        AppText(
            text = profileVisitedShop.name,
            style = AppTextStyle.B1,
            color = GrayColor.C500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        ProfileVisitCountBadge(visitCount = profileVisitedShop.visitCount)
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileVisitRankingShopSummaryPreview() {
    RamapTheme {
        ProfileVisitRankingShopSummary(
            profileVisitedShop = ProfileVisitedShop(id = "1", name = "부타이", visitCount = 5),
            modifier = Modifier.padding(16.dp),
        )
    }
}
