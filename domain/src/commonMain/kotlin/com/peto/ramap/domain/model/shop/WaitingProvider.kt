package com.peto.ramap.domain.model.shop

enum class WaitingProvider(
    val id: String,
) {
    CATCHTABLE("catchtable"),
    TABLING("tabling"),
    SYRUP_FRIENDS("syrup_friends"),
    UNKNOWN("unknown"),
    ;

    companion object {
        private val providersById: Map<String, WaitingProvider> = entries.associateBy(WaitingProvider::id)

        fun fromId(id: String): WaitingProvider = providersById[id] ?: UNKNOWN
    }
}
