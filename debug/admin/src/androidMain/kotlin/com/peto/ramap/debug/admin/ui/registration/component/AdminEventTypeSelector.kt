package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AdminEventTypeSelector(
    selectedEventType: ShopEventType,
    onEventTypeSelected: (ShopEventType) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShopEventType.entries.forEach { eventType ->
            FilterChip(
                selected = selectedEventType == eventType,
                onClick = { onEventTypeSelected(eventType) },
                label = {
                    AppText(
                        text = stringResource(ShopEventResourceMapper.typeLabel(eventType)),
                        style = AppTextStyle.B4,
                        color = if (selectedEventType == eventType) GrayColor.C500 else GrayColor.C300,
                    )
                },
                shape = RoundedCornerShape(100.dp),
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GrayColor.C050,
                        selectedLabelColor = GrayColor.C500,
                        containerColor = GrayColor.C050.copy(alpha = 0f),
                        labelColor = GrayColor.C300,
                    ),
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedEventType == eventType,
                        borderColor = GrayColor.C200,
                        selectedBorderColor = GrayColor.C200,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.dp,
                    ),
            )
        }
    }
}
