package com.peto.ramap.domain.model.notice

import com.peto.ramap.domain.model.shop.RamenShop
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class OperatingNotice(
    val id: String,
    val shop: RamenShop,
    val type: OperatingNoticeType,
    val description: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val sourceUrl: String?,
)
