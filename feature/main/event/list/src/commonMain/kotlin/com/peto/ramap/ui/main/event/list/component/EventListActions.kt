package com.peto.ramap.ui.main.event.list.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_registration_open
import ramap.shared.generated.resources.ic_add

@Composable
internal fun EventListActions(
    onClickAdminRegistration: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (onClickAdminRegistration == null) return

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 15.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_add),
            contentDescription = stringResource(Res.string.event_registration_open),
            modifier =
                Modifier
                    .size(25.dp)
                    .noRippleClickable(onClick = onClickAdminRegistration),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EventListActionsPreview() {
    RamapTheme {
        EventListActions(onClickAdminRegistration = {})
    }
}
