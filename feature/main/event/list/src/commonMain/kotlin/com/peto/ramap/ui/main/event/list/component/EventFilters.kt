package com.peto.ramap.ui.main.event.list.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.CategoryFilterChip
import com.peto.ramap.designsystem.resource.EventFilterResourceMapper
import com.peto.ramap.domain.model.event.EventFilter
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EventFilters(
    selectedFilter: EventFilter,
    onFilterSelected: (EventFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier =
            modifier
                .height(40.dp)
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(
            items = EventFilter.entries,
            key = { filter -> filter.name },
        ) { filter ->
            CategoryFilterChip(
                label = stringResource(EventFilterResourceMapper.label(filter)),
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                style = AppTextStyle.L3,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventFiltersPreview() {
    RamapTheme {
        EventFilters(
            selectedFilter = EventFilter.ALL,
            onFilterSelected = {},
        )
    }
}
