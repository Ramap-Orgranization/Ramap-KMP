package com.peto.ramap.debug.admin.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PreviewRequest(
    @SerialName("registration_type") val registrationType: String,
    @SerialName("shop_name") val shopName: String?,
    val feedback: String?,
    @SerialName("source_url") val sourceUrl: String?,
    @SerialName("evidence_path") val evidencePath: String?,
)
