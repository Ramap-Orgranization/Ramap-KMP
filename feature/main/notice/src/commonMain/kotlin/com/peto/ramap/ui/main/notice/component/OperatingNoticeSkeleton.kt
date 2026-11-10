package com.peto.ramap.ui.main.notice.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        userScrollEnabled = false,
    ) {
        // Today section title
        item {
            Skeleton(
                modifier = Modifier.padding(horizontal = 16.dp).width(80.dp).height(24.dp),
            )
        }
        // Today items rows (2 rows of 4 items)
        items(2) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(4) {
                    OperatingNoticeShopItemSkeleton(modifier = Modifier.weight(1f))
                }
            }
        }
        // Scheduled section title
        item {
            Spacer(modifier = Modifier.height(3.dp))
            Skeleton(
                modifier = Modifier.padding(horizontal = 16.dp).width(80.dp).height(24.dp),
            )
        }
        // Scheduled items row (1 row of 4 items)
        items(1) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(4) {
                    OperatingNoticeShopItemSkeleton(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OperatingNoticeShopItemSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            Skeleton(modifier = Modifier.fillMaxSize(0.94f), shape = CircleShape)
        }
        Skeleton(modifier = Modifier.fillMaxWidth().height(36.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun OperatingNoticeSkeletonPreview() {
    RamapTheme {
        OperatingNoticeSkeleton()
    }
}
