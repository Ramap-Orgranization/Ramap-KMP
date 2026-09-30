package com.peto.ramap.ui.review.other.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.menu.AppDropdownMenu
import com.peto.ramap.designsystem.menu.AppDropdownMenuItem
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_more_vert
import ramap.shared.generated.resources.review_block
import ramap.shared.generated.resources.review_more
import ramap.shared.generated.resources.review_unblock

@Composable
internal fun ProfileBlockOverflowMenu(
    userId: String?,
    isBlocked: Boolean,
    enabled: Boolean,
    onToggleBlock: () -> Unit,
) {
    var expanded by remember(userId) { mutableStateOf(false) }

    Box {
        Image(
            painter = painterResource(Res.drawable.ic_more_vert),
            contentDescription = stringResource(Res.string.review_more),
            modifier =
                Modifier
                    .size(48.dp)
                    .noRippleClickable(enabled = enabled) { expanded = true }
                    .padding(12.dp),
            colorFilter = ColorFilter.tint(GrayColor.C500),
        )
        AppDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 150.dp),
        ) {
            AppDropdownMenuItem(
                text = stringResource(if (isBlocked) Res.string.review_unblock else Res.string.review_block),
                enabled = enabled,
                onClick = {
                    expanded = false
                    onToggleBlock()
                },
            )
        }
    }
}
