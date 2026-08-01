package com.peto.ramap.ui.main.ranking.model

import com.peto.ramap.domain.model.shop.RamenShop

data class PendingRankingAction(
    val shop: RamenShop,
    val enabled: Boolean,
)
