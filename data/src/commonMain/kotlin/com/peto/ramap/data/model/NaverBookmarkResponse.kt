package com.peto.ramap.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class NaverBookmarkResponse(
    val type: String? = null,
    val sid: JsonElement? = null,
    val name: String? = null,
    val address: String? = null,
    val px: JsonElement? = null,
    val py: JsonElement? = null,
    val available: Boolean? = null,
    val matched: Boolean? = null,
)
