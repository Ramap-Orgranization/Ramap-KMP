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
import ramap.shared.generated.resources.review_profile_reviews_tab_count
import ramap.shared.generated.resources.review_profile_saved_shops_tab
import ramap.shared.generated.resources.review_profile_saved_shops_tab_count

@Composable
internal fun OtherProfileTabs(
    selectedTab: OtherReviewsTab,
    onSelect: (OtherReviewsTab) -> Unit,
    reviewCount: Long?,
    savedShopCount: Long?,
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
                            when {
                                tab == OtherReviewsTab.Reviews && reviewCount != null -> stringResource(Res.string.review_profile_reviews_tab_count, reviewCount)
                                tab == OtherReviewsTab.SavedShops && savedShopCount != null -> stringResource(Res.string.review_profile_saved_shops_tab_count, savedShopCount)
                                tab == OtherReviewsTab.Reviews -> stringResource(Res.string.review_profile_reviews_tab)
                                else -> stringResource(Res.string.review_profile_saved_shops_tab)
                            },
                        style = AppTextStyle.T2,
                        color = if (selectedTab == tab) GrayColor.C500 else GrayColor.C300,
                    )
                },
            )
        }
    }
}
