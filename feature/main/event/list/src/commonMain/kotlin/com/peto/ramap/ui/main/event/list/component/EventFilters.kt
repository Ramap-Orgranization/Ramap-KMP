package com.peto.ramap.ui.main.event.list.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.resource.EventFilterResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.EventFilter
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_report
import ramap.shared.generated.resources.news_report_open

@Composable
internal fun EventFilters(
    selectedFilter: EventFilter,
    onFilterSelected: (EventFilter) -> Unit,
    onClickNewsReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EventFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter

            AppText(
                text = stringResource(EventFilterResourceMapper.label(filter)),
                style = if (isSelected) AppTextStyle.H3 else AppTextStyle.H4,
                color = if (isSelected) GrayColor.C500 else GrayColor.C300,
                modifier = Modifier.noRippleClickable { onFilterSelected(filter) },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Image(
            painter = painterResource(Res.drawable.ic_report),
            contentDescription = stringResource(Res.string.news_report_open),
            modifier =
                Modifier
                    .size(25.dp)
                    .noRippleClickable { onClickNewsReport() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EventFiltersPreview() {
    RamapTheme {
        EventFilters(
            selectedFilter = EventFilter.EVENT,
            onFilterSelected = {},
            onClickNewsReport = {},
        )
    }
}
