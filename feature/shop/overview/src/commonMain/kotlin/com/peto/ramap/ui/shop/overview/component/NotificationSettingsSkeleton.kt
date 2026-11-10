package com.peto.ramap.ui.shop.overview.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.Skeleton
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun NotificationSettingsSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Skeleton(
                modifier = Modifier.width(80.dp).height(34.dp),
                shape = RoundedCornerShape(10.dp),
            )
        }
        repeat(6) { index ->
            NotificationTypeChoiceSkeleton(
                titleWidthFraction =
                    when (index % 3) {
                        0 -> 0.45f
                        1 -> 0.35f
                        else -> 0.5f
                    },
                descriptionWidthFraction =
                    when (index % 3) {
                        0 -> 0.75f
                        1 -> 0.6f
                        else -> 0.8f
                    },
            )
        }
    }
}

@Composable
private fun NotificationTypeChoiceSkeleton(
    titleWidthFraction: Float,
    descriptionWidthFraction: Float,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .clip(shape)
                .background(CommonColor.White)
                .border(1.dp, GrayColor.C100, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Skeleton(
                modifier = Modifier.fillMaxWidth(titleWidthFraction).height(16.dp),
                shape = RoundedCornerShape(4.dp),
            )
            Skeleton(
                modifier = Modifier.fillMaxWidth(descriptionWidthFraction).height(12.dp),
                shape = RoundedCornerShape(4.dp),
            )
        }
        Skeleton(
            modifier = Modifier.width(44.dp).height(24.dp),
            shape = RoundedCornerShape(12.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationSettingsSkeletonPreview() {
    RamapTheme {
        NotificationSettingsSkeleton(
            modifier = Modifier.padding(20.dp),
        )
    }
}
