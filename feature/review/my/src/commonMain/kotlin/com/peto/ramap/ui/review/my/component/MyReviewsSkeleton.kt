package com.peto.ramap.ui.review.my.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
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
internal fun MyReviewsFilterSkeleton(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (width in listOf(80.dp, 80.dp, 92.dp)) {
            Skeleton(
                modifier = Modifier.size(width = width, height = 30.dp),
                shape = CircleShape,
            )
        }
    }
}

@Composable
internal fun MyReviewCardSkeleton(showPhotos: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Skeleton(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Skeleton(modifier = Modifier.fillMaxWidth(0.48f).height(16.dp))
                Skeleton(modifier = Modifier.fillMaxWidth(0.3f).height(12.dp))
            }
            Skeleton(
                modifier = Modifier.size(20.dp),
                shape = CircleShape,
            )
        }

        if (showPhotos) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                userScrollEnabled = false,
            ) {
                items(2) {
                    Skeleton(
                        modifier = Modifier.size(220.dp),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Skeleton(modifier = Modifier.fillMaxWidth().height(14.dp))
            Skeleton(modifier = Modifier.fillMaxWidth(0.76f).height(14.dp))
            Skeleton(modifier = Modifier.fillMaxWidth(0.42f).height(14.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MyReviewsSkeletonPreview() {
    RamapTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MyReviewsFilterSkeleton()
            MyReviewCardSkeleton(showPhotos = true)
        }
    }
}
