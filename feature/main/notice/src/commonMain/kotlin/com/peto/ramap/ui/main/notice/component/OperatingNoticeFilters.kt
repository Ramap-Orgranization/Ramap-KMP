package com.peto.ramap.ui.main.notice.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.domain.model.notice.OperatingNoticeFeed
import com.peto.ramap.domain.model.notice.OperatingNoticeFeedItem
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AdministrativeDistrict
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.preview.OperatingNoticePreviewParameterProvider
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeUiState
import kotlinx.datetime.LocalDate

@Composable
internal fun OperatingNoticeFilters(
    uiState: OperatingNoticeUiState,
    onTabSelected: (Boolean) -> Unit,
    onQueryChanged: (String) -> Unit,
    onAreaOpened: () -> Unit,
    onSavedToggled: () -> Unit,
) {
    OperatingNoticeTabs(
        scheduledSelected = uiState.scheduledSelected,
        todayCount = uiState.todayCount,
        scheduledCount = uiState.scheduledCount,
        onTabSelected = onTabSelected,
    )
    NoticeSearchField(
        query = uiState.query,
        onQueryChanged = onQueryChanged,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 12.dp),
    )
    OperatingNoticeFilterChips(
        areaFilter = uiState.areaFilter,
        savedOnly = uiState.savedOnly,
        onAreaOpened = onAreaOpened,
        onSavedToggled = onSavedToggled,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
    )
    HorizontalDivider(color = GrayColor.C100)
}

@Preview(showBackground = true)
@Composable
private fun OperatingNoticeFiltersPreview() {
    RamapTheme {
        Column {
            OperatingNoticeFilters(
                uiState = operatingNoticeFiltersPreviewState(),
                onTabSelected = {},
                onQueryChanged = {},
                onAreaOpened = {},
                onSavedToggled = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun OperatingNoticeSelectedFiltersPreview() {
    val state = operatingNoticeFiltersPreviewState()
    val bookmarkedShopIds =
        state.feed
            ?.items
            .orEmpty()
            .map { it.notice.shop.id }
            .toSet()
    RamapTheme {
        Column {
            OperatingNoticeFilters(
                uiState =
                    state.copy(
                        scheduledSelected = true,
                        query = "라멘",
                        areaFilter = AreaFilter.District(AdministrativeArea.SEOUL, AdministrativeDistrict("마포구")),
                        savedOnly = true,
                        bookmarkedShopIds = bookmarkedShopIds,
                        personalizationState = PersonalizationBootstrapState.Success(Personalization(bookmarkedShopIds = bookmarkedShopIds)),
                    ),
                onTabSelected = {},
                onQueryChanged = {},
                onAreaOpened = {},
                onSavedToggled = {},
            )
        }
    }
}

private fun operatingNoticeFiltersPreviewState(): OperatingNoticeUiState =
    OperatingNoticeUiState(
        feed =
            OperatingNoticeFeed(
                referenceDate = LocalDate(2026, 10, 14),
                items = OperatingNoticePreviewParameterProvider().values.first().map { OperatingNoticeFeedItem(it, AdministrativeArea.SEOUL, "마포구") },
            ),
    )
