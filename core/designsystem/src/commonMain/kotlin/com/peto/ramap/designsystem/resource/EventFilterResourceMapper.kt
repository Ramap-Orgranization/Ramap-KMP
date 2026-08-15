package com.peto.ramap.designsystem.resource

import com.peto.ramap.domain.model.event.EventFilter
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_filter_all
import ramap.shared.generated.resources.event_filter_event
import ramap.shared.generated.resources.event_filter_store_renewal
import ramap.shared.generated.resources.event_filter_summer_limited

object EventFilterResourceMapper {
    fun label(filter: EventFilter): StringResource =
        when (filter) {
            EventFilter.ALL -> Res.string.event_filter_all
            EventFilter.SUMMER_LIMITED -> Res.string.event_filter_summer_limited
            EventFilter.EVENT -> Res.string.event_filter_event
            EventFilter.STORE_RENEWAL -> Res.string.event_filter_store_renewal
        }
}
