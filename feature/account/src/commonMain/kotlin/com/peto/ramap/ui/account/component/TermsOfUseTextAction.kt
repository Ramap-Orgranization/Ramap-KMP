package com.peto.ramap.ui.account.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun TermsOfUseTextAction(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = text,
            style = AppTextStyle.B2,
            color = GrayColor.C300,
            textAlign = TextAlign.Center,
            textDecoration = TextDecoration.Underline,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TermsOfUseTextActionPreview() {
    RamapTheme {
        TermsOfUseTextAction(
            text = "돌아가기",
        ) {}
    }
}
