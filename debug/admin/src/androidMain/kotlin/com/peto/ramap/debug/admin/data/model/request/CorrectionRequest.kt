package com.peto.ramap.debug.admin.data.model.request

import com.peto.ramap.debug.admin.data.model.AdminCorrectionChanges
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CorrectionRequest(
    val action: String,
    @SerialName("registration_type") val registrationType: String? = null,
    @SerialName("target_id") val targetId: String? = null,
    val request: String? = null,
    val changes: AdminCorrectionChanges? = null,
)
