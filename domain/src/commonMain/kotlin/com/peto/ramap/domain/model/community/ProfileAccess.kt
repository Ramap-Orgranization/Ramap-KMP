package com.peto.ramap.domain.model.community

sealed interface ProfileAccess {
    data class Visible(
        val profile: PublicProfile,
    ) : ProfileAccess

    data class Blocked(
        val profile: PublicProfile,
    ) : ProfileAccess

    data object Private : ProfileAccess

    data object Unavailable : ProfileAccess
}
