package com.peto.ramap.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class OperatingNoticePreviewParameterProvider : PreviewParameterProvider<List<OperatingNotice>> {
    override val values: Sequence<List<OperatingNotice>> =
        sequenceOf(
            listOf(
                OperatingNotice(
                    id = "closure",
                    shop = RamenShopPreviewParameterProvider().values.first(),
                    type = OperatingNoticeType.TEMPORARY_CLOSURE,
                    description = "내부 사정으로 하루 쉽니다.",
                    startDate = LocalDate(2026, 10, 14),
                    endDate = LocalDate(2026, 10, 14),
                    startTime = null,
                    endTime = null,
                    sourceUrl = null,
                ),
                OperatingNotice(
                    id = "early",
                    shop = RamenShopPreviewParameterProvider().values.first(),
                    type = OperatingNoticeType.EARLY_CLOSING,
                    description = "점심 영업만 합니다.",
                    startDate = LocalDate(2026, 10, 15),
                    endDate = LocalDate(2026, 10, 15),
                    startTime = null,
                    endTime = LocalTime(15, 0),
                    sourceUrl = null,
                ),
            ),
        )
}
