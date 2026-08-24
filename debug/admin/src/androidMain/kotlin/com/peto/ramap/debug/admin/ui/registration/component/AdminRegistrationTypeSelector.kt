package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun AdminRegistrationTypeSelector(
    isOperatingNotice: Boolean,
    onRegistrationTypeSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(GrayColor.C050)
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SelectorItem(
            text = stringResource(R.string.admin_registration_event),
            selected = !isOperatingNotice,
            onClick = { onRegistrationTypeSelected(false) },
            modifier = Modifier.weight(1f),
        )
        SelectorItem(
            text = stringResource(R.string.admin_registration_notice),
            selected = isOperatingNotice,
            onClick = { onRegistrationTypeSelected(true) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SelectorItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (selected) CommonColor.White else GrayColor.C050)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = text,
            style = if (selected) AppTextStyle.B1 else AppTextStyle.B2,
            color = if (selected) GrayColor.C500 else GrayColor.C300,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminRegistrationTypeSelectorPreview() {
    RamapTheme {
        AdminRegistrationTypeSelector(
            isOperatingNotice = true,
            onRegistrationTypeSelected = {},
        )
    }
}
