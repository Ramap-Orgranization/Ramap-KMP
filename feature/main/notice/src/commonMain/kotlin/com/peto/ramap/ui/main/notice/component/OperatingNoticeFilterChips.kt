package com.peto.ramap.ui.main.notice.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppFilterChip
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.preview.AreaFilterPreviewParameterProvider
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.resource.area.AreaFilterResourceMapper
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_keyboard_arrow_down
import ramap.shared.generated.resources.ic_kid_star
import ramap.shared.generated.resources.ic_kid_star_filled

@Composable
internal fun OperatingNoticeFilterChips(
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
            onClick = onSavedToggled,
            icon = if (savedOnly) Res.drawable.ic_kid_star_filled else Res.drawable.ic_kid_star,
            selected = savedOnly,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OperatingNoticeFilterChipsPreview(
    @PreviewParameter(AreaFilterPreviewParameterProvider::class)
    areaFilter: AreaFilter,
) {
    RamapTheme {
        OperatingNoticeFilterChips(
            areaFilter = areaFilter,
            savedOnly = true,
            onAreaOpened = {},
            onSavedToggled = {},
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}
