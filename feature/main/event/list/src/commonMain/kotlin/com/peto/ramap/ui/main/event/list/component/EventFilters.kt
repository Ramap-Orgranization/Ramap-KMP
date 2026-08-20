package com.peto.ramap.ui.main.event.list.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.peto.ramap.designsystem.resource.EventFilterResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.EventFilter
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EventFilters(
    selectedFilter: EventFilter,
    onFilterSelected: (EventFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    PrimaryTabRow(
        selectedTabIndex = EventFilter.entries.indexOf(selectedFilter),
        modifier = modifier.fillMaxWidth(),
    ) {
        EventFilter.entries.forEach { filter ->
            Tab(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                text = {
                    AppText(
                        text = stringResource(EventFilterResourceMapper.label(filter)),
                        style = AppTextStyle.B1,
                        color = if (filter == selectedFilter) GrayColor.C500 else GrayColor.C300,
                    )
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventFiltersPreview() {
    RamapTheme {
        EventFilters(
            selectedFilter = EventFilter.EVENT,
            onFilterSelected = {},
        )
    }
}
