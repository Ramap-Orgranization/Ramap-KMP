package com.peto.ramap.ui.main.event.list.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheetConfig
import com.peto.ramap.designsystem.card.EventShopGroupCard
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEvents

@Composable
internal fun EventListBottomSheet(
    events: ShopEvents,
    onDismiss: () -> Unit,
    onEventClick: (ShopEvent) -> Unit,
) {
    CommonBottomSheet(
        visible = true,
        onDismissRequest = onDismiss,
        config = CommonBottomSheetConfig(isDraggable = true),
    ) { modifier ->
        Column(
            modifier = modifier.padding(horizontal = 20.dp).padding(bottom = 5.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EventShopGroupCard(
                eventGroup = events,
                onEventClick = onEventClick,
            )
        }
    }
}
