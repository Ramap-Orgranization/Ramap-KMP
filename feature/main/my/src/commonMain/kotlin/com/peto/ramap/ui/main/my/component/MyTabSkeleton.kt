package com.peto.ramap.ui.main.my.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.Skeleton
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun MyTabSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(108.dp),
            contentAlignment = Alignment.Center,
        ) {
            Skeleton(
                modifier = Modifier.size(86.dp),
                shape = CircleShape,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Skeleton(
            modifier =
                Modifier
                    .width(110.dp)
                    .height(22.dp),
            shape = RoundedCornerShape(4.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Skeleton(
            modifier =
                Modifier
                    .width(150.dp)
                    .height(14.dp),
            shape = RoundedCornerShape(4.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MyTabSkeletonPreview() {
    RamapTheme {
        MyTabSkeleton()
    }
}
