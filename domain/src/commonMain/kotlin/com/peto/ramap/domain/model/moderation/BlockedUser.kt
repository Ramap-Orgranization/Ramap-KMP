package com.peto.ramap.domain.model.moderation

import com.peto.ramap.domain.model.profile.PublicProfile
import kotlinx.datetime.LocalDate

data class BlockedUser(
    val profile: PublicProfile,
    val blockedOn: LocalDate,
)
