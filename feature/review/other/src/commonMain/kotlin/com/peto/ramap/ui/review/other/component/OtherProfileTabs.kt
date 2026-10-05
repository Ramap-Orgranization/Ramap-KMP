package com.peto.ramap.ui.review.other.component

import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.review.other.contract.OtherReviewsTab
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_profile_reviews_tab
import ramap.shared.generated.resources.review_profile_saved_shops_tab

@Composable
internal fun OtherProfileTabs(
    selectedTab: OtherReviewsTab,
    onSelect: (OtherReviewsTab) -> Unit,
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = CommonColor.White,
        contentColor = GrayColor.C500,
    ) {
        OtherReviewsTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onSelect(tab) },
                text = {
                    AppText(
                        text =
                            stringResource(
                                if (tab == OtherReviewsTab.Reviews) {
                                    Res.string.review_profile_reviews_tab
                                } else {
                                    Res.string.review_profile_saved_shops_tab
                                },
                            ),
                        style = AppTextStyle.T2,
                        color = if (selectedTab == tab) GrayColor.C500 else GrayColor.C300,
                    )
                },
            )
        }
    }
}
