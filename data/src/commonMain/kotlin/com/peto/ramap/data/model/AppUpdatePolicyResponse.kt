package com.peto.ramap.data.model

import com.peto.ramap.domain.model.update.AppUpdatePolicy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AppUpdatePolicyResponse(
    @SerialName("minimum_build_number")
    val minimumBuildNumber: Long,
    @SerialName("store_url")
    val storeUrl: String,
) {
    fun toDomain(): AppUpdatePolicy =
        AppUpdatePolicy(
            minimumBuildNumber = minimumBuildNumber,
            storeUrl = storeUrl,
        )
}
