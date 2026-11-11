package com.peto.ramap.designsystem.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.peto.ramap.domain.model.profile.ProfileVisitRanking

internal val PODIUM_ORDER = listOf(2, 1, 3)
internal val PODIUM_HEIGHTS = listOf(78, 54, 44)

@Composable
internal fun ProfileVisitRankingContent(
    ranking: ProfileVisitRanking,
    onShopClick: (String) -> Unit,
    initiallyExpanded: Boolean,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            for (rank in PODIUM_ORDER) {
                val shop = ranking.values[rank - 1]
                ProfileVisitRankingPodiumStep(
                    rank = rank,
                    profileVisitedShop = shop,
                    onClick = { onShopClick(shop.id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (ranking.hasMoreShops) {
            ProfileVisitRankingDisclosureButton(
                expanded = expanded,
                onClick = { expanded = !expanded },
            )
            if (expanded) ProfileVisitRankingExpandedList(ranking, onShopClick)
        }
    }
}
