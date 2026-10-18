package com.peto.ramap.ui.main.notice.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppFilterChip
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.notice.OperatingNoticeFeed
import com.peto.ramap.domain.model.notice.OperatingNoticeFeedItem
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AdministrativeDistrict
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.preview.OperatingNoticePreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.LocalAppTypography
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeUiState
import com.peto.ramap.ui.resource.area.AreaFilterResourceMapper
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_keyboard_arrow_down
import ramap.shared.generated.resources.ic_kid_star
import ramap.shared.generated.resources.ic_kid_star_filled
import ramap.shared.generated.resources.ic_notice_search
import ramap.shared.generated.resources.operating_notice_saved
import ramap.shared.generated.resources.operating_notice_search

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

@Preview(name = "기본 필터", showBackground = true, widthDp = 360)
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

@Preview(name = "검색·지역·저장 선택", showBackground = true, widthDp = 360)
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

@Composable
private fun NoticeSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier.background(GrayColor.C050, RoundedCornerShape(12.dp)),
        textStyle = LocalAppTypography.current.b2.copy(color = GrayColor.C500),
        cursorBrush = SolidColor(GrayColor.C500),
        singleLine = true,
        decorationBox = { input ->
            Row(
                modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_notice_search),
                    contentDescription = null,
                    tint = GrayColor.C300,
                    modifier = Modifier.size(20.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        AppText(
                            text = stringResource(Res.string.operating_notice_search),
                            style = AppTextStyle.B2,
                            color = GrayColor.C300,
                        )
                    }
                    input()
                }
            }
        },
    )
}

@Composable
private fun OperatingNoticeFilterChips(
    areaFilter: AreaFilter,
    savedOnly: Boolean,
    onAreaOpened: () -> Unit,
    onSavedToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val areaLabel = AreaFilterResourceMapper.label(areaFilter)
        AppFilterChip(
            label = stringResource(areaLabel.resource, *areaLabel.arguments.toTypedArray()),
            onClick = onAreaOpened,
            trailingIcon = Res.drawable.ic_keyboard_arrow_down,
        )
        AppFilterChip(
            label = stringResource(Res.string.operating_notice_saved),
            onClick = onSavedToggled,
            icon = if (savedOnly) Res.drawable.ic_kid_star_filled else Res.drawable.ic_kid_star,
            selected = savedOnly,
        )
    }
}
