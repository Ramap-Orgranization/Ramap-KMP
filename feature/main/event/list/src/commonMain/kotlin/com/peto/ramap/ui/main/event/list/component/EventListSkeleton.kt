package com.peto.ramap.ui.main.event.list.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
        contentPadding = PaddingValues(bottom = 5.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
    ) {
        item { EventSectionTitleSkeleton() }
        item { OngoingEventsSkeleton() }
        item { EventSectionTitleSkeleton() }
        item { OngoingEventsSkeleton() }
        item { EventSectionTitleSkeleton() }
        items(3) { EventCardSkeleton() }
    }
}

@Composable
private fun EventSectionTitleSkeleton() {
    Skeleton(
        modifier = Modifier.padding(top = 8.dp, start = 15.dp).width(80.dp).height(24.dp),
    )
}

@Composable
private fun OngoingEventsSkeleton() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(4) {
            Column(
                modifier = Modifier.width(72.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Skeleton(modifier = Modifier.size(68.dp), shape = CircleShape)
                Skeleton(modifier = Modifier.fillMaxWidth().height(14.dp))
            }
        }
    }
}

@Composable
private fun EventCardSkeleton() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
                .border(1.dp, GrayColor.C100, RoundedCornerShape(16.dp))
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Skeleton(modifier = Modifier.size(44.dp), shape = CircleShape)
            Skeleton(modifier = Modifier.weight(1f).height(18.dp))
            Skeleton(modifier = Modifier.width(48.dp).height(22.dp))
        }
        Skeleton(modifier = Modifier.fillMaxWidth(0.7f).height(18.dp))
        Skeleton(modifier = Modifier.fillMaxWidth(0.35f).height(14.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun EventListSkeletonPreview() {
    RamapTheme {
        EventListSkeleton()
    }
}
