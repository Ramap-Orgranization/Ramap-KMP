package com.peto.ramap.ui.main.notice.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
internal fun OperatingNoticeSkeleton(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false,
    ) {
        item {
            Skeleton(
                modifier = Modifier.padding(horizontal = 16.dp).width(80.dp).height(24.dp),
            )
        }
        item {
            FlowRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(8) {
                    OperatingNoticeShopItemSkeleton()
                }
                repeat(10) {
                    Spacer(Modifier.width(72.dp).height(0.dp))
                }
            }
        }
    }
}

@Composable
private fun OperatingNoticeShopItemSkeleton() {
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Skeleton(modifier = Modifier.size(68.dp), shape = CircleShape)
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
