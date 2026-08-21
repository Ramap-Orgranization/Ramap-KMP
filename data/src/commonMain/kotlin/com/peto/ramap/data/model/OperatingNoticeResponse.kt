package com.peto.ramap.data.model

import com.peto.ramap.data.extension.toLocalDate
import com.peto.ramap.data.extension.toLocalTime
import com.peto.ramap.domain.model.operatingnotice.OperatingNotice
import com.peto.ramap.domain.model.operatingnotice.OperatingNoticeType
import com.peto.ramap.domain.model.shop.RamenShop
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OperatingNoticeResponse(
    val id: String,
    @SerialName("shop_id")
    val shopId: String,
    @SerialName("notice_type")
    val noticeType: String,
    val title: String,
    val description: String,
    @SerialName("notice_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String?,
    @SerialName("start_time")
    val startTime: String?,
    @SerialName("end_time")
    val endTime: String?,
    @SerialName("source_url")
    val sourceUrl: String?,
) {
    fun toDomain(shop: RamenShop): OperatingNotice =
        OperatingNotice(
            id = id,
            shop = shop,
            type = OperatingNoticeType.from(noticeType),
            title = title,
            description = description,
            startDate = startDate.toLocalDate(),
            endDate = endDate?.toLocalDate(),
            startTime = startTime?.toLocalTime(),
            endTime = endTime?.toLocalTime(),
            sourceUrl = sourceUrl,
        )
}
