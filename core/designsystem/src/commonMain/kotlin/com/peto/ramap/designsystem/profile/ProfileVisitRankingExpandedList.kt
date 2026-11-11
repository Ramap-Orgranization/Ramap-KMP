package com.peto.ramap.designsystem.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.peto.ramap.domain.model.profile.ProfileVisitRanking
import com.peto.ramap.theme.GrayColor

@Composable
internal fun ProfileVisitRankingExpandedList(
    ranking: ProfileVisitRanking,
    onShopClick: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = GrayColor.C100,
                        start = Offset.Zero,
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (index in ProfileVisitRanking.PODIUM_SIZE until ranking.values.size) {
            ProfileVisitRankingRow(
                rank = ranking.rankAt(index),
                profileVisitedShop = ranking.values[index],
                onShopClick = onShopClick,
            )
        }
    }
}
