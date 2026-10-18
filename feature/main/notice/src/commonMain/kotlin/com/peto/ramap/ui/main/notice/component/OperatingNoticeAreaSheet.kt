package com.peto.ramap.ui.main.notice.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.peto.ramap.domain.model.notice.OperatingNoticeFeed
import com.peto.ramap.domain.model.notice.OperatingNoticeFeedItem
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AdministrativeDistrict
import com.peto.ramap.domain.model.shop.AdministrativeDistricts
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.preview.OperatingNoticePreviewParameterProvider
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.component.area.AreaSheetContent
import kotlinx.datetime.LocalDate

@Composable
internal fun OperatingNoticeAreaSheet(
    feed: OperatingNoticeFeed?,
    areaFilter: AreaFilter,
    onAreaSelected: (AreaFilter) -> Unit,
) {
    var selectedArea by remember {
        mutableStateOf(
            when (areaFilter) {
                AreaFilter.Nationwide -> null
                is AreaFilter.Province -> areaFilter.area
                is AreaFilter.District -> areaFilter.area
            },
        )
    }
    val districts = selectedArea?.let { area -> feed?.districts(area).orEmpty().map(::AdministrativeDistrict) }.orEmpty()
    AreaSheetContent(
        areaFilter = areaFilter,
        areaSelectionArea = selectedArea,
        administrativeDistricts = AdministrativeDistricts(districts),
        isLoadingDistricts = false,
        onAdministrativeAreaSelected = { area ->
            if (area == AdministrativeArea.SEJONG) {
                onAreaSelected(AreaFilter.Province(area))
            } else {
                selectedArea = area
            }
        },
        onAreaFilterSelected = onAreaSelected,
    )
}

@Preview(name = "전국 지역 선택", showBackground = true, widthDp = 360)
@Composable
private fun OperatingNoticeNationwideAreaSheetPreview() {
    RamapTheme {
        OperatingNoticeAreaSheet(
            feed = null,
            areaFilter = AreaFilter.Nationwide,
            onAreaSelected = {},
        )
    }
}

@Preview(name = "시군구 선택", showBackground = true, widthDp = 360)
@Composable
private fun OperatingNoticeDistrictAreaSheetPreview() {
    val notices = OperatingNoticePreviewParameterProvider().values.first()
    RamapTheme {
        OperatingNoticeAreaSheet(
            feed =
                OperatingNoticeFeed(
                    referenceDate = LocalDate(2026, 10, 14),
                    items =
                        listOf(
                            OperatingNoticeFeedItem(notices[0], AdministrativeArea.SEOUL, "마포구"),
                            OperatingNoticeFeedItem(notices[1], AdministrativeArea.SEOUL, "서초구"),
                            OperatingNoticeFeedItem(notices[2], AdministrativeArea.SEOUL, "종로구"),
                        ),
                ),
            areaFilter = AreaFilter.District(AdministrativeArea.SEOUL, AdministrativeDistrict("마포구")),
            onAreaSelected = {},
        )
    }
}
