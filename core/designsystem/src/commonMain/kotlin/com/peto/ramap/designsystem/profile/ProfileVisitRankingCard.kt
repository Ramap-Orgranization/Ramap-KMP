package com.peto.ramap.designsystem.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.profile.ProfileVisitRanking
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.profile_visit_ranking_title

@Composable
fun ProfileVisitRankingCard(
    ranking: ProfileVisitRanking?,
    onShopClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileVisitRankingCard(
        ranking = ranking,
        onShopClick = onShopClick,
        modifier = modifier,
        initiallyShowMore = false,
    )
}

@Composable
internal fun ProfileVisitRankingCard(
    ranking: ProfileVisitRanking?,
    onShopClick: (String) -> Unit,
    modifier: Modifier,
    initiallyShowMore: Boolean,
) {
    if (ranking?.hasPodium != true) return
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(CommonColor.White)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        AppText(
            text = stringResource(Res.string.profile_visit_ranking_title),
            style = AppTextStyle.T2,
            color = GrayColor.C500,
        )
        ProfileVisitRankingContent(
            ranking = ranking,
            onShopClick = onShopClick,
            initiallyExpanded = initiallyShowMore,
        )
    }
}
