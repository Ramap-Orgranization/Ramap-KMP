package com.peto.ramap.designsystem.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.peto.ramap.core.extension.noRippleClickable
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor

@Composable
fun CommonPopup(
    visible: Boolean,
    anchorOffset: IntOffset,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!visible) return

    Popup(
        alignment = Alignment.TopStart,
        offset = anchorOffset,
        onDismissRequest = onDismiss,
        properties =
            PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
    ) {
        content()
    }
}

@Composable
fun CommonPopupItem(
    text: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (isSelected) GrayColor.C050 else GrayColor.C100.copy(alpha = 0f))
                .noRippleClickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        AppText(
            text = text,
            style = if (isSelected) AppTextStyle.B1 else AppTextStyle.B2,
            color = if (isSelected) GrayColor.C500 else GrayColor.C400,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
fun CommonPopupDivider() {
    HorizontalDivider(thickness = 1.dp, color = GrayColor.C100)
}
