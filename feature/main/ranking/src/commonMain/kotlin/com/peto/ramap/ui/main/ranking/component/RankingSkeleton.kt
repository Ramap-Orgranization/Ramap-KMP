package com.peto.ramap.ui.main.ranking.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
internal fun RankingSkeleton(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 24.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        userScrollEnabled = false,
    ) {
        items(10) {
            RankingRowSkeleton()
        }
    }
}

@Composable
internal fun RankingRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Skeleton(
            modifier = Modifier.size(width = 16.dp, height = 24.dp),
            shape = RoundedCornerShape(4.dp),
        )

        Skeleton(
            modifier = Modifier.size(55.dp),
            shape = RoundedCornerShape(8.dp),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Skeleton(
                modifier =
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(18.dp),
                shape = RoundedCornerShape(4.dp),
            )
            Skeleton(
                modifier =
                    Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp),
                shape = RoundedCornerShape(4.dp),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Skeleton(
                modifier = Modifier.size(25.dp),
                shape = CircleShape,
            )
            Skeleton(
                modifier = Modifier.size(width = 24.dp, height = 14.dp),
                shape = RoundedCornerShape(2.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RankingSkeletonPreview() {
    RamapTheme {
        RankingSkeleton()
    }
}

@Preview(showBackground = true)
@Composable
private fun RankingRowSkeletonPreview() {
    RamapTheme {
        RankingRowSkeleton(modifier = Modifier.padding(horizontal = 24.dp))
    }
}
