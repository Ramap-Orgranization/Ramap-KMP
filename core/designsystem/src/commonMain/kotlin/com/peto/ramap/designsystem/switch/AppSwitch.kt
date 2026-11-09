package com.peto.ramap.designsystem.switch

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors =
            SwitchDefaults.colors(
                checkedThumbColor = CommonColor.White,
                checkedTrackColor = GrayColor.C500,
                uncheckedThumbColor = CommonColor.White,
                uncheckedTrackColor = GrayColor.C200,
                disabledCheckedThumbColor = CommonColor.White,
                disabledCheckedTrackColor = GrayColor.C300,
                disabledUncheckedThumbColor = CommonColor.White,
                disabledUncheckedTrackColor = GrayColor.C100,
            ),
    )
}
