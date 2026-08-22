package com.peto.ramap.data.model

import com.peto.ramap.domain.model.appnotice.AppNotice
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AppNoticeResponse(
    @SerialName("notice_id")
    val id: String,
    val title: String,
    val message: String,
) {
    fun toDomain(): AppNotice =
        AppNotice(
            id = id,
            title = title,
            message = message,
        )
}
