package com.peto.ramap.ui.main.notice.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.notice.OperatingNoticeFeed
import com.peto.ramap.domain.model.notice.OperatingNoticeFeedItem
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AdministrativeDistrict
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.preview.OperatingNoticePreviewParameterProvider
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeUiState
import kotlinx.datetime.LocalDate

class OperatingNoticeUiStatePreviewParameterProvider : PreviewParameterProvider<OperatingNoticeUiState> {
    override val values: Sequence<OperatingNoticeUiState> =
        sequenceOf(
            defaultState,
            selectedState,
        )

    companion object {
        val defaultState: OperatingNoticeUiState
            get() {
                val notices = OperatingNoticePreviewParameterProvider().values.first()
                return OperatingNoticeUiState(
                    feed =
                        OperatingNoticeFeed(
                            referenceDate = LocalDate(2026, 10, 14),
                            items = notices.map { OperatingNoticeFeedItem(it, AdministrativeArea.SEOUL, "마포구") },
                        ),
                )
            }

        val selectedState: OperatingNoticeUiState
            get() {
                val base = defaultState
                val bookmarkedShopIds = base.feed?.items.orEmpty().map { it.notice.shop.id }.toSet()
                return base.copy(
                    scheduledSelected = true,
                    query = "라멘",
                    areaFilter = AreaFilter.District(AdministrativeArea.SEOUL, AdministrativeDistrict("마포구")),
                    savedOnly = true,
                    bookmarkedShopIds = bookmarkedShopIds,
                    personalizationState =
                        PersonalizationBootstrapState.Success(
                            Personalization(bookmarkedShopIds = bookmarkedShopIds),
                        ),
                )
            }
    }
}
