package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun AdminSourceField(
    sourceUrl: String,
    onSourceUrlChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = sourceUrl,
        onValueChange = onSourceUrlChanged,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            AppText(
                text = stringResource(R.string.admin_registration_source_placeholder),
                style = AppTextStyle.B2,
                color = GrayColor.C200,
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrayColor.C200,
                unfocusedBorderColor = GrayColor.C200,
                cursorColor = GrayColor.C500,
            ),
        singleLine = true,
    )
}

@Preview(showBackground = true)
@Composable
private fun AdminSourceFieldPreview() {
    RamapTheme {
        AdminSourceField(
            sourceUrl = "https://instagram.com/...",
            onSourceUrlChanged = {},
        )
    }
}
