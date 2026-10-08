package com.peto.ramap.domain.model.community

sealed interface ProfileAccess {
    data class Visible(
        val profile: PublicProfile,
        val reviewCount: Int? = null,
        val savedShopCount: Int? = null,
    ) : ProfileAccess

    data class Blocked(
        val profile: PublicProfile,
    ) : ProfileAccess

    data object Private : ProfileAccess

    data object Unavailable : ProfileAccess
}
