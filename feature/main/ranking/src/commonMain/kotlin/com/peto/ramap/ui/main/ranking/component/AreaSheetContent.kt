package com.peto.ramap.ui.main.ranking.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.ranking.model.AdministrativeAreaUiModel
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ranking_all_regions

@Composable
internal fun AreaSheetContent(
    areaFilter: AreaFilter,
    onAreaFilterSelected: (AreaFilter) -> Unit,
) {
    AreaOption(
        label = stringResource(Res.string.ranking_all_regions),
        selected = areaFilter is AreaFilter.Nationwide,
        onClick = { onAreaFilterSelected(AreaFilter.Nationwide) },
    )
    AdministrativeAreaUiModel.entries.forEach { area ->
        val optionFilter = AreaFilter.Selected(area.area)
        AreaOption(
            label = stringResource(area.officialNameResource),
            selected = areaFilter == optionFilter,
            onClick = { onAreaFilterSelected(optionFilter) },
        )
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun AreaOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    AppText(
        text = label,
        modifier =
            Modifier
                .fillMaxWidth()
                .noRippleClickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 13.dp),
        style = if (selected) AppTextStyle.T2 else AppTextStyle.B2,
        color = if (selected) GrayColor.C500 else GrayColor.C300,
    )
}

@Preview(showBackground = true)
@Composable
private fun AreaSheetContentPreview() {
    RamapTheme {
        AreaSheetContent(
            areaFilter = AreaFilter.Selected(AdministrativeArea.SEOUL),
            onAreaFilterSelected = {},
        )
    }
}
