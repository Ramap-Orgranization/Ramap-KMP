package com.peto.ramap.domain.model.event

enum class ShopEventType {
    COLLAB,
    POPUP,
    LIMITED_MENU,
    SUMMER_LIMITED,
    NEW_MENU,
    STORE_RENEWAL,
    ;

    companion object {
        fun from(id: String): ShopEventType =
            runCatching { valueOf(id.uppercase()) }
                .getOrElse { error("Invalid event type: $id") }
    }
}
