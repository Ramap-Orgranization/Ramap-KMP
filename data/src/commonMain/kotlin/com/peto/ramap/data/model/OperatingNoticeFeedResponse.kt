package com.peto.ramap.data.model

import com.peto.ramap.domain.model.notice.OperatingNoticeFeed
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OperatingNoticeFeedResponse(
    @SerialName("reference_date") val referenceDate: String,
    val items: List<OperatingNoticeFeedItemResponse>,
) {
    fun toDomain(): OperatingNoticeFeed = OperatingNoticeFeed(LocalDate.parse(referenceDate), items.mapNotNull { it.toDomain() })
}
