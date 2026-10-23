package com.peto.ramap.ui.main.notice.model

import com.peto.ramap.domain.model.notice.OperatingNotice
import kotlinx.datetime.LocalDate

data class OperatingNoticeSelection(
    val notice: OperatingNotice,
    val date: LocalDate?,
    val notices: List<OperatingNotice>,
)
