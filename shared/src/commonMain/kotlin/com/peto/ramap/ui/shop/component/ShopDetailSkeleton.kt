package com.peto.ramap.ui.shop.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.Skeleton
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.shop.menu.component.ShopMenuSkeleton
import com.peto.ramap.ui.shop.model.ShopDetailTab
import com.peto.ramap.ui.shop.review.component.ShopReviewSkeleton

@Composable
internal fun ShopDetailSkeleton(
    selectedTab: ShopDetailTab,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag("shop_detail_initial_skeleton"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ShopHeaderSkeleton()
            ShopBusinessHoursSkeleton()
            ShopLinksSkeleton()
        }
        ShopTabsSkeleton(selectedTab = selectedTab)
        when (selectedTab) {
            ShopDetailTab.MENU -> {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(27.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Skeleton(modifier = Modifier.width(48.dp).height(18.dp))
                        Skeleton(modifier = Modifier.width(110.dp).height(12.dp))
                    }
                    ShopMenuSkeleton()
                }
            }
            ShopDetailTab.REVIEW -> ShopReviewSkeleton()
        }
    }
}

@Composable
private fun ShopHeaderSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Skeleton(
                modifier = Modifier.size(45.dp),
                shape = CircleShape,
            )
            Column(modifier = Modifier.weight(1f).padding(start = 5.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(25.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Skeleton(modifier = Modifier.fillMaxWidth(0.65f).height(18.dp))
                }
                Row(
                    modifier = Modifier.height(21.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Skeleton(modifier = Modifier.size(15.dp), shape = CircleShape)
                    Skeleton(modifier = Modifier.width(28.dp).height(14.dp))
                }
            }
            repeat(2) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Skeleton(modifier = Modifier.size(20.dp), shape = CircleShape)
                }
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 20.dp).height(21.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { Skeleton(modifier = Modifier.width(38.dp).height(14.dp)) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(21.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Skeleton(modifier = Modifier.width(26.dp).height(14.dp))
            Skeleton(modifier = Modifier.fillMaxWidth(0.85f).height(14.dp))
        }
    }
}

@Composable
private fun ShopBusinessHoursSkeleton() {
    Column(
        modifier =
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .border(1.dp, GrayColor.C100, RoundedCornerShape(12.dp))
                .background(GrayColor.C050, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.height(18.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Skeleton(modifier = Modifier.size(14.dp), shape = CircleShape)
            Skeleton(modifier = Modifier.width(150.dp).height(12.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(31.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Skeleton(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
            Skeleton(modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ShopLinksSkeleton() {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(2) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(if (row == 0) 1 else 2) {
                    Skeleton(
                        modifier = Modifier.weight(1f).height(45.dp),
                        shape = if (row == 0) RoundedCornerShape(8.dp) else CircleShape,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopTabsSkeleton(selectedTab: ShopDetailTab) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ShopDetailTab.entries.forEach { tab ->
            Column(
                modifier = Modifier.weight(1f).height(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Skeleton(modifier = Modifier.width(40.dp).height(14.dp))
                    Skeleton(
                        modifier = Modifier.width(24.dp).height(20.dp),
                        shape = CircleShape,
                    )
                }
                if (tab == selectedTab) {
                    Skeleton(modifier = Modifier.fillMaxWidth().height(2.dp))
                } else {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ShopDetailMenuSkeletonPreview() {
    RamapTheme { ShopDetailSkeleton(selectedTab = ShopDetailTab.MENU) }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ShopDetailReviewSkeletonPreview() {
    RamapTheme { ShopDetailSkeleton(selectedTab = ShopDetailTab.REVIEW) }
}
