package com.peto.ramap.domain.model.personalization

data class Personalization(
    val bookmarkedShopIds: Set<String> = emptySet(),
    val hiddenShopIds: Set<String> = emptySet(),
)
