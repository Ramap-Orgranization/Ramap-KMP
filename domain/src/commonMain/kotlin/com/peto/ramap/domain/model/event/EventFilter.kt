package com.peto.ramap.domain.model.event

enum class EventFilter {
    EVENT,
    NEW_MENU,
    STORE_RENEWAL,
    ;

    fun matches(event: ShopEvent): Boolean =
        when (this) {
            EVENT -> event.type != ShopEventType.NEW_MENU && event.type != ShopEventType.STORE_RENEWAL
            NEW_MENU -> event.type == ShopEventType.NEW_MENU
            STORE_RENEWAL -> event.type == ShopEventType.STORE_RENEWAL
        }
}
