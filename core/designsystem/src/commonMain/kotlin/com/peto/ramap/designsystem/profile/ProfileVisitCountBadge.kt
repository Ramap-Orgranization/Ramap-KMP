package com.peto.ramap.designsystem.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_visit_ramen_pixel
import ramap.shared.generated.resources.profile_visit_count

@Composable
internal fun ProfileVisitCountBadge(visitCount: Long) {
    Row(
        modifier =
            Modifier
                .background(GrayColor.C050, CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            bitmap = imageResource(Res.drawable.ic_visit_ramen_pixel),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            filterQuality = FilterQuality.None,
        )
        AppText(
            text = stringResource(Res.string.profile_visit_count, visitCount),
            style = AppTextStyle.B1,
            color = GrayColor.C500,
            maxLines = 1,
        )
    }
}
