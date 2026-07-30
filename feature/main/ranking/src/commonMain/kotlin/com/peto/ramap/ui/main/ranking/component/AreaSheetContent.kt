package com.peto.ramap.ui.main.ranking.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AdministrativeDistrict
import com.peto.ramap.domain.model.shop.AdministrativeDistricts
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.resource.area.AdministrativeAreaUiModel
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ranking_all_regions
import ramap.shared.generated.resources.ranking_area_all

@Composable
internal fun AreaSheetContent(
    areaFilter: AreaFilter,
    areaSelectionArea: AdministrativeArea?,
    administrativeDistricts: AdministrativeDistricts,
    isLoadingDistricts: Boolean,
    onAdministrativeAreaSelected: (AdministrativeArea) -> Unit,
    onAreaFilterSelected: (AreaFilter) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().height(AREA_LIST_HEIGHT)) {
        AdministrativeAreaOptions(
            areaSelectionArea = areaSelectionArea,
            onAdministrativeAreaSelected = onAdministrativeAreaSelected,
            onNationwideSelected = { onAreaFilterSelected(AreaFilter.Nationwide) },
            modifier = Modifier.weight(MASTER_COLUMN_WEIGHT),
        )
        DistrictOptions(
            area = areaSelectionArea,
            areaFilter = areaFilter,
            administrativeDistricts = administrativeDistricts,
            isLoading = isLoadingDistricts,
            onAreaFilterSelected = onAreaFilterSelected,
            modifier = Modifier.weight(DETAIL_COLUMN_WEIGHT),
        )
    }
}

@Composable
private fun AdministrativeAreaOptions(
    areaSelectionArea: AdministrativeArea?,
    onAdministrativeAreaSelected: (AdministrativeArea) -> Unit,
    onNationwideSelected: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
    ) {
        AreaOption(
            label = stringResource(Res.string.ranking_all_regions),
            selected = areaSelectionArea == null,
            onClick = onNationwideSelected,
        )
        AdministrativeAreaUiModel.entries.forEach { area ->
            AreaOption(
                label = stringResource(area.shortName),
                selected = areaSelectionArea == area.area,
                onClick = { onAdministrativeAreaSelected(area.area) },
            )
        }
    }
}

@Composable
private fun DistrictOptions(
    area: AdministrativeArea?,
    areaFilter: AreaFilter,
    administrativeDistricts: AdministrativeDistricts,
    isLoading: Boolean,
    onAreaFilterSelected: (AreaFilter) -> Unit,
    modifier: Modifier,
) {
    key(area) {
        Column(
            modifier =
                modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
        ) {
            if (area == null) return@Column

            val provinceFilter = AreaFilter.Province(area)
            val provinceName =
                stringResource(AdministrativeAreaUiModel.map(area).shortName)
            AreaOption(
                label = stringResource(Res.string.ranking_area_all, provinceName),
                selected = areaFilter == provinceFilter,
                onClick = { onAreaFilterSelected(provinceFilter) },
            )
            if (isLoading) {
                DistrictLoading()
                return@Column
            }
            administrativeDistricts.forEach { district ->
                val districtFilter = AreaFilter.District(area, district)
                AreaOption(
                    label = district.name,
                    selected = areaFilter == districtFilter,
                    onClick = { onAreaFilterSelected(districtFilter) },
                )
            }
        }
    }
}

@Composable
private fun DistrictLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = InstagramColor.Pink,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun AreaOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    color = if (selected) GrayColor.C100 else Color.Transparent,
                    shape = RoundedCornerShape(24.dp),
                ).noRippleClickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp),
            style = if (selected) AppTextStyle.T2 else AppTextStyle.B2,
            color = if (selected) GrayColor.C500 else GrayColor.C400,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AreaSheetContentPreview() {
    RamapTheme {
        AreaSheetContent(
            areaFilter =
                AreaFilter.District(
                    AdministrativeArea.GYEONGGI,
                    AdministrativeDistrict("수원시"),
                ),
            areaSelectionArea = AdministrativeArea.GYEONGGI,
            administrativeDistricts =
                AdministrativeDistricts(
                    listOf(
                        AdministrativeDistrict("고양시"),
                        AdministrativeDistrict("수원시"),
                        AdministrativeDistrict("용인시"),
                    ),
                ),
            isLoadingDistricts = false,
            onAdministrativeAreaSelected = {},
            onAreaFilterSelected = {},
        )
    }
}

private const val MASTER_COLUMN_WEIGHT = 0.4f
private const val DETAIL_COLUMN_WEIGHT = 0.6f
private val AREA_LIST_HEIGHT = 440.dp
