package com.peto.ramap.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.LocalAppTypography
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_notice_search
import ramap.shared.generated.resources.operating_notice_search

@Composable
fun NoticeSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier.background(GrayColor.C050, RoundedCornerShape(12.dp)),
        textStyle = LocalAppTypography.current.b2.copy(color = GrayColor.C500),
        cursorBrush = SolidColor(GrayColor.C500),
        singleLine = true,
        decorationBox = { input ->
            Row(
                modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_notice_search),
                    contentDescription = null,
                    tint = GrayColor.C300,
                    modifier = Modifier.size(20.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        AppText(
                            text = stringResource(Res.string.operating_notice_search),
                            style = AppTextStyle.B2,
                            color = GrayColor.C300,
                        )
                    }
                    input()
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun NoticeSearchFieldPreview() {
    RamapTheme {
        NoticeSearchField(
            query = "",
            onQueryChanged = {},
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}
