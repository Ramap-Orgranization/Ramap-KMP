package com.peto.ramap.data.model

import com.peto.ramap.domain.model.community.ProfileAccess
import kotlinx.serialization.Serializable

@Serializable
internal data class ProfileAccessResponse(
    val status: String,
    val profile: PublicProfileResponse? = null,
) {
    fun toDomain(): ProfileAccess =
        when (status) {
            VISIBLE -> ProfileAccess.Visible(requireNotNull(profile).toDomain())
            BLOCKED -> ProfileAccess.Blocked(requireNotNull(profile).toDomain())
            PRIVATE -> ProfileAccess.Private
            UNAVAILABLE -> ProfileAccess.Unavailable
            else -> error("Unknown profile access status: $status")
        }

    companion object {
        const val VISIBLE = "visible"
        const val BLOCKED = "blocked"
        const val PRIVATE = "private"
        const val UNAVAILABLE = "unavailable"
    }
}
