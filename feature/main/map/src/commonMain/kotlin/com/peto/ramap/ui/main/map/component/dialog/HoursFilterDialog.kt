package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.button.AppIconButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.ShopHoursFilter
import com.peto.ramap.domain.model.shop.ShopVisitTimeWindow
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.BrandColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.map.component.dialog.Option
import com.peto.ramap.ui.main.map.contract.HoursFilterDialogUiState
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.ic_hours_filter_calendar
import ramap.shared.generated.resources.ic_hours_filter_clock
import ramap.shared.generated.resources.ic_hours_filter_storefront
import ramap.shared.generated.resources.ic_refresh
import ramap.shared.generated.resources.map_hours_filter_all_shops
import ramap.shared.generated.resources.map_hours_filter_apply_with_count
import ramap.shared.generated.resources.map_hours_filter_close
import ramap.shared.generated.resources.map_hours_filter_dialog_description
import ramap.shared.generated.resources.map_hours_filter_dialog_title
import ramap.shared.generated.resources.map_hours_filter_open_now
import ramap.shared.generated.resources.map_hours_filter_reset
import ramap.shared.generated.resources.map_hours_filter_visit

private val MinHeightForFixedOptions = 320.dp

@Composable
internal fun HoursFilterDialog(
    selected: ShopHoursFilter,
    window: ShopVisitTimeWindow,
    resultCount: (ShopHoursFilter) -> Int,
    onDismiss: () -> Unit,
    onApply: (ShopHoursFilter) -> Unit,
) {
    var state by remember(selected) { mutableStateOf(HoursFilterDialogUiState(selected)) }
    val filter = state.filter(window)
    val openNowCount = remember(resultCount) { resultCount(ShopHoursFilter.OpenNow) }
    val selectedCount = if (filter == ShopHoursFilter.OpenNow) openNowCount else resultCount(filter)

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.matchParentSize().noRippleClickable(onClick = onDismiss))
            HoursFilterDialogContent(
                state = state,
                window = window,
                selectedCount = selectedCount,
                onStateChange = { state = it },
                onDismiss = onDismiss,
                onApply = { onApply(filter) },
                modifier =
                    Modifier
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                        .widthIn(max = 400.dp)
                        .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HoursFilterDialogContent(
    state: HoursFilterDialogUiState,
    window: ShopVisitTimeWindow,
    selectedCount: Int,
    onStateChange: (HoursFilterDialogUiState) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = CommonColor.White,
        shadowElevation = 4.dp,
    ) {
        Column {
            HoursFilterDialogHeader(onDismiss = onDismiss)

            BoxWithConstraints(
                modifier = Modifier.weight(1f, fill = false),
            ) {
                val scrollOptions = maxHeight < MinHeightForFixedOptions
                val optionsScroll = rememberScrollState()
                val pickerScroll = rememberScrollState()

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(if (scrollOptions) Modifier.verticalScroll(optionsScroll) else Modifier)
                            .padding(horizontal = 16.dp),
                ) {
                    HoursFilterOptions(
                        selectedOption = state.option,
                        onSelect = { onStateChange(state.select(it)) },
                    )

                    if (state.option == Option.VisitTime) {
                        HoursFilterVisitPicker(
                            visit = state.visitDateTime(window),
                            window = window,
                            onDateSelected = { onStateChange(state.selectDate(it, window)) },
                            onTimeSelected = { onStateChange(state.selectTime(it, window)) },
                            modifier =
                                if (scrollOptions) {
                                    Modifier
                                } else {
                                    Modifier
                                        .weight(1f, fill = false)
                                        .verticalScroll(pickerScroll)
                                },
                        )
                    } else {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            HorizontalDivider(color = GrayColor.C100)
            HoursFilterDialogActions(
                selectedCount = selectedCount,
                onReset = { onStateChange(HoursFilterDialogUiState()) },
                onApply = onApply,
            )
        }
    }
}

@Composable
private fun HoursFilterDialogHeader(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = stringResource(Res.string.map_hours_filter_dialog_title),
                style = AppTextStyle.H4,
                color = GrayColor.C500,
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { heading() },
            )
            AppIconButton(
                icon = Res.drawable.ic_close,
                contentDescription = stringResource(Res.string.map_hours_filter_close),
                onClick = onDismiss,
            )
        }
        AppText(
            text = stringResource(Res.string.map_hours_filter_dialog_description),
            style = AppTextStyle.C1,
            color = GrayColor.C300,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}

@Composable
private fun HoursFilterOptions(
    selectedOption: Option,
    onSelect: (Option) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(1.dp, GrayColor.C100, shape)
                .selectableGroup(),
    ) {
        HoursFilterOptionRow(
            icon = Res.drawable.ic_hours_filter_calendar,
            title = stringResource(Res.string.map_hours_filter_all_shops),
            selected = selectedOption == Option.All,
            onClick = { onSelect(Option.All) },
        )
        HorizontalDivider(color = GrayColor.C100)
        HoursFilterOptionRow(
            icon = Res.drawable.ic_hours_filter_storefront,
            title = stringResource(Res.string.map_hours_filter_open_now),
            selected = selectedOption == Option.OpenNow,
            onClick = { onSelect(Option.OpenNow) },
        )
        HorizontalDivider(color = GrayColor.C100)
        HoursFilterOptionRow(
            icon = Res.drawable.ic_hours_filter_clock,
            title = stringResource(Res.string.map_hours_filter_visit),
            selected = selectedOption == Option.VisitTime,
            onClick = { onSelect(Option.VisitTime) },
        )
    }
}

@Composable
private fun HoursFilterOptionRow(
    icon: DrawableResource,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .background(if (selected) BrandColor.SubtleFill else CommonColor.White)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .background(
                        color = if (selected) CommonColor.White else GrayColor.C050,
                        shape = RoundedCornerShape(8.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
        AppText(
            text = title,
            style = AppTextStyle.B1,
            color = GrayColor.C500,
            modifier = Modifier.weight(1f),
        )
        RadioButton(
            selected = selected,
            onClick = null,
            colors =
                RadioButtonDefaults.colors(
                    selectedColor = BrandColor.Primary,
                    unselectedColor = GrayColor.C200,
                ),
        )
    }
}

@Composable
private fun HoursFilterDialogActions(
    selectedCount: Int,
    onReset: () -> Unit,
    onApply: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppButton(
            text = stringResource(Res.string.map_hours_filter_reset),
            onClick = onReset,
            textStyle = AppTextStyle.B1,
            textColor = GrayColor.C400,
            backgroundColor = CommonColor.White,
            border = BorderStroke(1.dp, GrayColor.C100),
            icon = Res.drawable.ic_refresh,
            iconSize = 16.dp,
            iconTint = GrayColor.C400,
            modifier = Modifier.width(92.dp),
        )
        AppButton(
            text = stringResource(Res.string.map_hours_filter_apply_with_count, selectedCount),
            onClick = onApply,
            modifier = Modifier.weight(1f),
            textStyle = AppTextStyle.B1,
            textColor = BrandColor.OnFill,
            backgroundColor = BrandColor.Primary,
        )
    }
}

@Preview(name = "영업시간 필터 설정", showBackground = true, widthDp = 360, heightDp = 760)
@Preview(name = "작은 화면의 영업시간 필터 설정", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
private fun HoursFilterDialogPreview() {
    RamapTheme {
        HoursFilterDialogContent(
            state = HoursFilterDialogUiState(ShopHoursFilter.VisitTime(LocalDateTime(2026, 11, 14, 19, 0))),
            window = ShopVisitTimeWindow(LocalDateTime(2026, 11, 14, 18, 45)),
            selectedCount = 12,
            onStateChange = {},
            onDismiss = {},
            onApply = {},
            modifier =
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
        )
    }
}
