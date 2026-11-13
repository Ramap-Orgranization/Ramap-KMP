package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.peto.ramap.theme.LocalAppTypography
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.map.contract.HoursFilterDialogUiState
import com.peto.ramap.ui.main.map.contract.HoursFilterDialogUiState.Option
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_clock
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.ic_refresh
import ramap.shared.generated.resources.map_hours_filter_all_shops
import ramap.shared.generated.resources.map_hours_filter_apply_with_count
import ramap.shared.generated.resources.map_hours_filter_close
import ramap.shared.generated.resources.map_hours_filter_dialog_title
import ramap.shared.generated.resources.map_hours_filter_open_now
import ramap.shared.generated.resources.map_hours_filter_reset
import ramap.shared.generated.resources.map_hours_filter_visit

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
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
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
                        .fillMaxWidth()
                        .heightIn(max = (maxHeight - 32.dp).coerceIn(0.dp, 648.dp)),
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
    val typography = LocalAppTypography.current
    val dialogTypography =
        remember(typography) {
            typography.copy(
                t2 = typography.t2.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, fontFeatureSettings = "tnum"),
                b2 = typography.b2.copy(letterSpacing = 0.sp),
                b4 = typography.b4.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp, fontFeatureSettings = "tnum"),
                c1 = typography.c1.copy(letterSpacing = 0.sp, fontFeatureSettings = "tnum"),
            )
        }
    CompositionLocalProvider(LocalAppTypography provides dialogTypography) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(20.dp),
            color = CommonColor.White,
            shadowElevation = 4.dp,
        ) {
            Column(modifier = if (state.option == Option.VisitTime) Modifier.fillMaxHeight() else Modifier) {
                HoursFilterDialogHeader(onDismiss = onDismiss)

                Column(
                    modifier =
                        Modifier
                            .weight(1f, fill = state.option == Option.VisitTime)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        modifier = Modifier.selectableGroup(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        HoursFilterOptionRow(
                            text = stringResource(Res.string.map_hours_filter_all_shops),
                            selected = state.option == Option.All,
                            onClick = { onStateChange(state.select(Option.All)) },
                        )
                        HoursFilterOptionRow(
                            text = stringResource(Res.string.map_hours_filter_open_now),
                            selected = state.option == Option.OpenNow,
                            onClick = { onStateChange(state.select(Option.OpenNow)) },
                        )
                        HoursFilterOptionRow(
                            text = stringResource(Res.string.map_hours_filter_visit),
                            selected = state.option == Option.VisitTime,
                            onClick = { onStateChange(state.select(Option.VisitTime)) },
                        )
                    }

                    if (state.option == Option.VisitTime) {
                        val visit = state.visitDateTime(window)
                        HoursFilterVisitPicker(
                            visit = visit,
                            window = window,
                            onDateSelected = { onStateChange(state.selectDate(it, window)) },
                            onTimeSelected = { onStateChange(state.selectTime(it, window)) },
                        )
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
}

@Composable
private fun HoursFilterDialogHeader(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_clock),
                contentDescription = null,
                tint = GrayColor.C500,
                modifier = Modifier.size(20.dp),
            )
            AppText(
                text = stringResource(Res.string.map_hours_filter_dialog_title),
                style = AppTextStyle.T2,
                color = GrayColor.C500,
            )
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            AppIconButton(
                icon = Res.drawable.ic_close,
                contentDescription = stringResource(Res.string.map_hours_filter_close),
                onClick = onDismiss,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
    }
}

@Composable
private fun HoursFilterOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(shape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = text,
            style = AppTextStyle.B2,
            color = GrayColor.C500,
            modifier = Modifier.weight(1f),
        )
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = BrandColor.Primary, unselectedColor = GrayColor.C300),
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
            height = 48.dp,
            cornerRadius = 8.dp,
            modifier = Modifier.width(76.dp),
        )
        Surface(
            onClick = onApply,
            modifier = Modifier.weight(1f).height(48.dp),
            color = BrandColor.Primary,
            shape = RoundedCornerShape(8.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = stringResource(Res.string.map_hours_filter_apply_with_count, selectedCount),
                    style = AppTextStyle.B1,
                    color = BrandColor.OnFill,
                )
            }
        }
    }
}

@Preview(showBackground = true)
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
            modifier = Modifier.padding(12.dp).fillMaxWidth().heightIn(max = 620.dp),
        )
    }
}
