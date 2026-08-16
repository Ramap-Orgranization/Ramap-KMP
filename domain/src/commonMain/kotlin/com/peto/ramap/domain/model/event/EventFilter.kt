package com.peto.ramap.domain.model.event

enum class EventFilter {
    ALL,
    SUMMER_LIMITED,
    EVENT,
    STORE_RENEWAL,
    ;

    fun matches(event: ShopEvent): Boolean =
        when (this) {
            ALL -> true
            SUMMER_LIMITED -> event.type == ShopEventType.SUMMER_LIMITED
            EVENT ->
                event.type != ShopEventType.SUMMER_LIMITED &&
                    event.type != ShopEventType.STORE_RENEWAL
            STORE_RENEWAL -> event.type == ShopEventType.STORE_RENEWAL
        }
}
