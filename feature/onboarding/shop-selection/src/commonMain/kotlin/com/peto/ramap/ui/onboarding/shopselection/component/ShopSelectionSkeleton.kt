package com.peto.ramap.ui.onboarding.shopselection.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.RamenShopSummarySkeleton
import com.peto.ramap.designsystem.component.Skeleton
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.loading_content_description

@Composable
internal fun ShopSelectionSkeleton(rowCount: Int = 8) {
    val loadingDescription = stringResource(Res.string.loading_content_description)
    Column(
        modifier =
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                contentDescription = loadingDescription
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(rowCount) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RamenShopSummarySkeleton(
                    contentPadding = PaddingValues(start = 8.dp),
                    modifier = Modifier.weight(1f),
                )
                Skeleton(modifier = Modifier.size(24.dp))
            }
        }
    }
}
