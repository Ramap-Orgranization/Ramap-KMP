package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peto.ramap.debug.admin.data.model.AdminScheduleOverride
import com.peto.ramap.debug.admin.ui.registration.continuousSegments
import com.peto.ramap.designsystem.button.AppButton

@Composable
internal fun AdminRegularScheduleSelector(
    day: AdminScheduleOverride?,
    onSegmentSelected: (String, String) -> Unit,
) {
    val segments = day?.continuousSegments().orEmpty()
    if (segments.isEmpty()) return
    Column {
        segments.forEach { segment ->
            val open = segment.open ?: return@forEach
            val close = segment.close ?: return@forEach
            AppButton("$open ~ $close", { onSegmentSelected(open, close) }, modifier = Modifier)
        }
    }
}
