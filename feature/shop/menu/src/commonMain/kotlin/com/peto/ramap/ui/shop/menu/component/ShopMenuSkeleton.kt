package com.peto.ramap.ui.shop.menu.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.Skeleton
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun ShopMenuSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Skeleton(
                modifier = Modifier.width(80.dp).height(24.dp),
                shape = RoundedCornerShape(12.dp),
            )
        }

        repeat(3) { index ->
            ShopMenuItemSkeleton()
            if (index < 2) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 4.dp),
                    thickness = 1.dp,
                    color = GrayColor.C100,
                )
            }
        }
    }
}

@Composable
private fun ShopMenuItemSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Skeleton(
            modifier = Modifier.size(96.dp),
            shape = RoundedCornerShape(16.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Skeleton(
                modifier = Modifier.width(48.dp).height(16.dp),
                shape = RoundedCornerShape(999.dp),
            )
            Skeleton(
                modifier = Modifier.fillMaxWidth(0.6f).height(20.dp),
            )
            Skeleton(
                modifier = Modifier.fillMaxWidth(0.85f).height(14.dp),
            )
            Skeleton(
                modifier = Modifier.width(70.dp).height(18.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopMenuSkeletonPreview() {
    RamapTheme {
        ShopMenuSkeleton(
            modifier = Modifier.padding(16.dp),
        )
    }
}
