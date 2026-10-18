package com.peto.ramap.domain.model.notice

import com.peto.ramap.domain.model.shop.AdministrativeArea

data class OperatingNoticeFeedItem(
    val notice: OperatingNotice,
    val area: AdministrativeArea? = null,
    val sigungu: String? = null,
)
