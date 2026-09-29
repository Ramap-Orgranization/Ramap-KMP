package com.peto.ramap.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class DeleteAccountResponse(
    val deleted: Boolean = false,
)
