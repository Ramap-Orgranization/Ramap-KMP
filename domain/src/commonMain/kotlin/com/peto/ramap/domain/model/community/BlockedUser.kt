package com.peto.ramap.domain.model.community

import kotlinx.datetime.LocalDate

data class BlockedUser(
    val profile: PublicProfile,
    val blockedOn: LocalDate,
)
