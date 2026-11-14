package com.peto.ramap.domain.model.shop

import kotlinx.datetime.LocalDate

data class ShopOpening(
    val date: LocalDate,
    val kind: Kind,
) {
    enum class Kind {
        OPENING,
        OFFICIAL,
        SOFT,
    }
}
