package com.peto.ramap.ui.main.notice.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.Skeleton
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun OperatingNoticeSkeleton(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier, userScrollEnabled = false) {
        items(6) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Skeleton(modifier = Modifier.size(56.dp), shape = CircleShape)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Skeleton(modifier = Modifier.width(120.dp).height(20.dp))
                    Skeleton(modifier = Modifier.width(90.dp).height(22.dp))
                }
            }
        }
    }
}

@Preview(name = "영업 변동 로딩", showBackground = true, widthDp = 360, heightDp = 560)
@Composable
private fun OperatingNoticeSkeletonPreview() {
    RamapTheme {
        OperatingNoticeSkeleton()
    }
}
