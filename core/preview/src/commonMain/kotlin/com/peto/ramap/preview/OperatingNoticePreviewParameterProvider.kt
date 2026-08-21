package com.peto.ramap.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class OperatingNoticePreviewParameterProvider : PreviewParameterProvider<List<OperatingNotice>> {
    private val shopProvider = RamenShopPreviewParameterProvider()
    private val shops = shopProvider.ramenShopPreviewSamples

    override val values: Sequence<List<OperatingNotice>> =
        sequenceOf(
            listOf(
                OperatingNotice(
                    id = "1",
                    shop = shops[0],
                    type = OperatingNoticeType.OPERATING_NOTICE,
                    description = "금일 준비한 육수가 모두 소진되어 영업을 종료합니다. 내일 다시 뵙겠습니다.",
                    startDate = LocalDate(2026, 8, 21),
                    endDate = LocalDate(2026, 8, 21),
                    startTime = null,
                    endTime = LocalTime(20, 0),
                    sourceUrl = "https://instagram.com/p/123",
                ),
                OperatingNotice(
                    id = "2",
                    shop = shops[1],
                    type = OperatingNoticeType.TEMPORARY_CLOSURE,
                    description = "내부 수리 관계로 이번 주말 휴무합니다.",
                    startDate = LocalDate(2026, 8, 21),
                    endDate = LocalDate(2026, 8, 23),
                    startTime = null,
                    endTime = null,
                    sourceUrl = null,
                ),
                OperatingNotice(
                    id = "3",
                    shop = shops[2],
                    type = OperatingNoticeType.EARLY_CLOSING,
                    description = "갑작스러운 개인 사정으로 오늘만 일찍 닫습니다.",
                    startDate = LocalDate(2026, 8, 21),
                    endDate = LocalDate(2026, 8, 21),
                    startTime = null,
                    endTime = LocalTime(18, 0),
                    sourceUrl = null,
                ),
                OperatingNotice(
                    id = "4",
                    shop = shops[3],
                    type = OperatingNoticeType.LATE_OPENING,
                    description = "준비 시간 부족으로 평소보다 1시간 늦게 오픈합니다.",
                    startDate = LocalDate(2026, 8, 21),
                    endDate = LocalDate(2026, 8, 21),
                    startTime = LocalTime(12, 0),
                    endTime = null,
                    sourceUrl = null,
                ),
                OperatingNotice(
                    id = "5",
                    shop = shops[0],
                    type = OperatingNoticeType.TEMPORARY_CLOSURE,
                    description = "개인 사정으로 다음 주 월요일 하루 쉽니다.",
                    startDate = LocalDate(2026, 8, 24),
                    endDate = LocalDate(2026, 8, 24),
                    startTime = null,
                    endTime = null,
                    sourceUrl = null,
                ),
                OperatingNotice(
                    id = "6",
                    shop = shops[1],
                    type = OperatingNoticeType.OPERATING_NOTICE,
                    description = "다음 달부터 영업 시간이 1시간 단축됩니다.",
                    startDate = LocalDate(2026, 9, 1),
                    endDate = LocalDate(2026, 9, 30),
                    startTime = null,
                    endTime = null,
                    sourceUrl = null,
                ),
                OperatingNotice(
                    id = "7",
                    shop = shops[2],
                    type = OperatingNoticeType.EARLY_CLOSING,
                    description = "다음 주 금요일은 매장 재정비로 인해 휴무합니다.",
                    startDate = LocalDate(2026, 8, 28),
                    endDate = LocalDate(2026, 8, 28),
                    startTime = null,
                    endTime = null,
                    sourceUrl = null,
                ),
                OperatingNotice(
                    id = "8",
                    shop = shops[3],
                    type = OperatingNoticeType.LATE_OPENING,
                    description = "신메뉴 준비를 위해 오전 영업을 하지 않습니다.",
                    startDate = LocalDate(2026, 9, 5),
                    endDate = LocalDate(2026, 9, 5),
                    startTime = LocalTime(15, 0),
                    endTime = null,
                    sourceUrl = null,
                ),
                OperatingNotice(
                    id = "9",
                    shop = shops[3],
                    type = OperatingNoticeType.LATE_OPENING,
                    description = "신메뉴 준비를 위해 오전 영업을 하지 않습니다.",
                    startDate = LocalDate(2026, 9, 5),
                    endDate = LocalDate(2026, 9, 5),
                    startTime = LocalTime(15, 0),
                    endTime = null,
                    sourceUrl = null,
                ),
            ),
        )
}
