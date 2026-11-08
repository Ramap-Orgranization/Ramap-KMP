package com.peto.ramap.ui.shop.overview.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.domain.model.notification.NotificationCatalog
import com.peto.ramap.domain.model.notification.NotificationSelection
import com.peto.ramap.domain.model.notification.NotificationTypeId
import com.peto.ramap.preview.NotificationCatalogPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_notification_settings_clear
import ramap.shared.generated.resources.shop_notification_settings_select_all

@Composable
internal fun NotificationSettingsChoices(
    catalog: NotificationCatalog,
    selection: NotificationSelection,
    enabled: Boolean,
    onTypeToggled: (NotificationTypeId) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAllSelected = catalog.types.isNotEmpty() && selection.ids.size == catalog.types.size
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        AppButton(
            text =
                stringResource(
                    if (isAllSelected) {
                        Res.string.shop_notification_settings_clear
                    } else {
                        Res.string.shop_notification_settings_select_all
                    },
                ),
            onClick = if (isAllSelected) onClear else onSelectAll,
            modifier = Modifier.width(76.dp),
            enabled = enabled,
            backgroundColor = if (isAllSelected) GrayColor.C500 else GrayColor.C050,
            textColor = if (isAllSelected) CommonColor.White else GrayColor.C500,
            disabledBackgroundColor = GrayColor.C050,
            disabledTextColor = GrayColor.C300,
            textStyle = AppTextStyle.B3,
            cornerRadius = 10.dp,
            height = 34.dp,
        )
    }

    for (type in catalog.types) {
        NotificationTypeChoice(
            type = type,
            selected = selection.contains(type.id),
            enabled = enabled,
            onToggle = { onTypeToggled(type.id) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationSettingsChoicesPreview(
    @PreviewParameter(NotificationCatalogPreviewParameterProvider::class) catalog: NotificationCatalog,
) {
    val selection =
        catalog.selection(
            catalog.types
                .take(2)
                .map { it.id }
                .toSet(),
        )
    RamapTheme {
        NotificationSettingsChoices(
            catalog = catalog,
            selection = selection,
            enabled = true,
            onTypeToggled = {},
            onSelectAll = {},
            onClear = {},
        )
    }
}
