package com.peto.ramap.ui.main.notice.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeUiState
import com.peto.ramap.ui.main.notice.preview.OperatingNoticeUiStatePreviewParameterProvider

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
private fun OperatingNoticeFiltersPreview(
    @PreviewParameter(OperatingNoticeUiStatePreviewParameterProvider::class)
    uiState: OperatingNoticeUiState,
) {
    RamapTheme {
        Column {
            OperatingNoticeFilters(
                uiState = uiState,
                onTabSelected = {},
                onQueryChanged = {},
                onAreaOpened = {},
                onSavedToggled = {},
            )
        }
    }
}
