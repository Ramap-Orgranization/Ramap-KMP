package com.peto.ramap.ui.shop.review.component

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
fun ShopReviewSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        repeat(2) { index ->
            ShopReviewCardSkeleton(showPhotos = index == 0)
        }
    }
}

@Composable
private fun ShopReviewCardSkeleton(
    showPhotos: Boolean = false,
) {
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
                        modifier = Modifier.size(180.dp),
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
private fun ShopReviewSkeletonPreview() {
    RamapTheme {
        ShopReviewSkeleton(
            modifier = Modifier.padding(16.dp),
        )
    }
}
