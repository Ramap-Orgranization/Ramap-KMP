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
                    id = "planned",
                    shop = RamenShopPreviewParameterProvider().values.first().copy(id = "planned-shop"),
                    type = OperatingNoticeType.PLANNED_CLOSING,
                    description = "개인 사정으로 점심 영업만 합니다.",
                    startDate = LocalDate(2026, 10, 14),
                    endDate = LocalDate(2026, 10, 14),
                    startTime = null,
                    endTime = LocalTime(15, 0),
                    sourceUrl = null,
                ),
                OperatingNotice(
                    id = "early",
                    shop = RamenShopPreviewParameterProvider().values.first().copy(id = "early-shop"),
                    type = OperatingNoticeType.EARLY_CLOSING,
                    description = "준비한 수량이 소진되어 일찍 마감합니다.",
                    startDate = LocalDate(2026, 10, 15),
                    endDate = LocalDate(2026, 10, 15),
                    startTime = null,
                    endTime = LocalTime(15, 0),
                    sourceUrl = null,
                ),
            ),
            emptyList(),
        ).let { examples ->
            examples + sequenceOf(examples.first().map { it.copy(shop = it.shop.copy(name = "아주 긴 매장 이름을 가진 라멘 전문점")) })
        }
}
