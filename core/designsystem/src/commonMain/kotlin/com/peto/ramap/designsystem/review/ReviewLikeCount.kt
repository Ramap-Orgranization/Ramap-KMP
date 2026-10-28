package com.peto.ramap.designsystem.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_heart_filled
import ramap.shared.generated.resources.ic_heart_outline
import ramap.shared.generated.resources.review_like

@Composable
fun ReviewLikeCount(
    count: Int,
    modifier: Modifier = Modifier,
    isLiked: Boolean = false,
) {
    val description = stringResource(Res.string.review_like, count)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(if (isLiked) Res.drawable.ic_heart_filled else Res.drawable.ic_heart_outline),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(if (isLiked) SystemColor.Warning else GrayColor.C300),
        )
        AppText(
            text = count.toString(),
            style = AppTextStyle.B2,
            color = GrayColor.C500,
        )
    }
}
