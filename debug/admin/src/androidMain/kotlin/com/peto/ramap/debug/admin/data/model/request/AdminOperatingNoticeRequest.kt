package com.peto.ramap.debug.admin.data.model.request

import kotlinx.serialization.Serializable

@Serializable
internal data class AdminOperatingNoticeRequest(
    val action: String,
    val id: String? = null,
)
