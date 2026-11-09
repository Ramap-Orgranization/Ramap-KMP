package com.peto.ramap.domain.model.announcement

data class AnnouncementReadState(
    val accountId: String? = null,
    val readIds: Set<String> = emptySet(),
)
