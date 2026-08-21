package com.peto.ramap.ui.main.event.list.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.Skeleton
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun EventListSkeleton(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 15.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        userScrollEnabled = false,
    ) {
        item {
            EventFiltersSkeleton()
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EventSectionTitleSkeleton()
                OngoingEventsSkeleton()
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EventSectionTitleSkeleton()
                repeat(3) {
                    EventCardSkeleton()
                    if (it < 2) Box(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun EventFiltersSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Skeleton(
                    modifier = Modifier.width(60.dp).height(18.dp),
                )
                if (index == 0) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(GrayColor.C200),
                    )
                }
            }
        }
    }
}

@Composable
private fun EventSectionTitleSkeleton() {
    Skeleton(
        modifier = Modifier.padding(start = 15.dp).width(80.dp).height(24.dp),
    )
}

@Composable
private fun OngoingEventsSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(4) {
            OngoingEventShopItemSkeleton(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun OngoingEventShopItemSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(72.dp),
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

@Composable
private fun EventCardSkeleton() {
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
                .border(1.dp, GrayColor.C100, cardShape)
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Skeleton(modifier = Modifier.size(44.dp), shape = CircleShape)
            Skeleton(modifier = Modifier.weight(1f).height(18.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Skeleton(modifier = Modifier.weight(1f).height(20.dp))
                Skeleton(modifier = Modifier.width(40.dp).height(18.dp))
            }
            Skeleton(modifier = Modifier.width(100.dp).height(14.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventListSkeletonPreview() {
    RamapTheme {
        EventListSkeleton()
    }
}
