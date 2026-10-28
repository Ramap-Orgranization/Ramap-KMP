package com.peto.ramap.designsystem.topbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.extension.noRippleClickable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.navigation_back

@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    CommonTopBar(
        title = title,
        modifier = modifier,
        left = {
            Image(
                painter = painterResource(Res.drawable.ic_arrow3_left),
                contentDescription = stringResource(Res.string.navigation_back),
                modifier =
                    Modifier
                        .padding(18.dp)
                        .size(24.dp)
                        .noRippleClickable(onClick = onBack),
            )
        },
        right = action,
    )
}
