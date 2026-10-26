package com.peto.ramap.ui.main.feed.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.Skeleton
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun FeedSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().background(CommonColor.White)) {
        repeat(2) {
            FeedItemSkeleton()
        }
    }
}

@Composable
private fun FeedItemSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        FeedAuthorSkeleton()
        FeedShopSkeleton()
        Spacer(modifier = Modifier.height(5.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            userScrollEnabled = false,
        ) {
            items(2) {
                Skeleton(
                    modifier = Modifier.size(220.dp),
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(21.dp)) {
            Skeleton(
                modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.85f).height(14.dp),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        FeedFooterSkeleton()
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = GrayColor.C100)
    }
}

@Composable
private fun FeedAuthorSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Skeleton(modifier = Modifier.size(44.dp), shape = CircleShape)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Skeleton(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
                Skeleton(modifier = Modifier.width(48.dp).height(12.dp))
            }
        }
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            Skeleton(modifier = Modifier.width(4.dp).height(16.dp))
        }
    }
}

@Composable
private fun FeedShopSkeleton() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 7.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ChromaticColor.Orange050)
                .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Skeleton(modifier = Modifier.size(44.dp), shape = CircleShape)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Skeleton(modifier = Modifier.fillMaxWidth(0.55f).height(14.dp))
            Skeleton(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp))
        }
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            Skeleton(modifier = Modifier.width(6.dp).height(12.dp))
        }
    }
}

@Composable
private fun FeedFooterSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Skeleton(modifier = Modifier.size(20.dp), shape = CircleShape)
            Skeleton(modifier = Modifier.width(12.dp).height(14.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(top = 10.dp).height(21.dp), contentAlignment = Alignment.Center) {
            Skeleton(modifier = Modifier.width(148.dp).height(14.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedSkeletonPreview() {
    RamapTheme {
        FeedSkeleton()
    }
}
