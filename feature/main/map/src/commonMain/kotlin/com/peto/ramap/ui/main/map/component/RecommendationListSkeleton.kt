package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.RamenShopSummarySkeleton
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.loading_content_description

@Composable
internal fun RecommendationListSkeleton(modifier: Modifier) {
    val loadingDescription = stringResource(Res.string.loading_content_description)

    LazyColumn(
        modifier =
            modifier.semantics {
                contentDescription = loadingDescription
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                liveRegion = LiveRegionMode.Polite
            },
        userScrollEnabled = false,
    ) {
        items(10) { index ->
            if (index > 0) HorizontalDivider(color = GrayColor.C100)
            RamenShopSummarySkeleton(
                contentPadding = PaddingValues(start = 8.dp, top = 16.dp, bottom = 16.dp),
            )
        }
    }
}
