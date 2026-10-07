package com.peto.ramap.ui.main.map.shop.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notice.OperatingNotice

@Immutable
data class ShopOverviewHeaderActions(
    val onDismissRequest: () -> Unit,
    val onBookmarkClick: () -> Unit,
    val onNotificationClick: () -> Unit,
    val onHiddenClick: () -> Unit,
    val onReportClick: () -> Unit,
    val onShareClick: () -> Unit,
    val onEventClick: (ShopEvent) -> Unit,
    val onOperatingNoticeClick: (OperatingNotice) -> Unit = {},
)
