package com.peto.ramap.designsystem.resource

import com.peto.ramap.domain.model.event.EventFilter
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_filter_event
import ramap.shared.generated.resources.event_filter_new_menu
import ramap.shared.generated.resources.event_filter_store_renewal

object EventFilterResourceMapper {
    fun label(filter: EventFilter): StringResource =
        when (filter) {
            EventFilter.EVENT -> Res.string.event_filter_event
            EventFilter.NEW_MENU -> Res.string.event_filter_new_menu
            EventFilter.STORE_RENEWAL -> Res.string.event_filter_store_renewal
        }
}
