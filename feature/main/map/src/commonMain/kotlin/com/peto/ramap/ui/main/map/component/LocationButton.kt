package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.current_location
import ramap.shared.generated.resources.ic_current_location

@Composable
internal fun LocationButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MapCircleIconButton(
        isActive = false,
        contentDescription = stringResource(Res.string.current_location),
        onClick = {
            if (!isLoading) onClick()
        },
        modifier = modifier,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = GrayColor.C500,
                strokeWidth = 2.dp,
            )
        } else {
            Image(
                painter = painterResource(Res.drawable.ic_current_location),
                contentDescription = null,
            )
        }
    }
}
