package com.peto.ramap.ui.review.my.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.review.my.contract.MyReviewsUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.my_reviews_all_filter
import ramap.shared.generated.resources.my_reviews_filter_number
import ramap.shared.generated.resources.my_reviews_private_filter
import ramap.shared.generated.resources.my_reviews_public_filter

@Composable
internal fun MyReviewFilterHeader(
    state: MyReviewsUiState,
    onSelectFilter: (MyReviewVisibility) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            label = stringResource(Res.string.my_reviews_all_filter),
            count = state.totalCount,
            selected = state.filter == MyReviewVisibility.ALL,
            onClick = { onSelectFilter(MyReviewVisibility.ALL) },
        )
        FilterChip(
            label = stringResource(Res.string.my_reviews_public_filter),
            count = state.publicCount,
            selected = state.filter == MyReviewVisibility.PUBLIC,
            onClick = { onSelectFilter(MyReviewVisibility.PUBLIC) },
        )
        FilterChip(
            label = stringResource(Res.string.my_reviews_private_filter),
            count = state.privateCount,
            selected = state.filter == MyReviewVisibility.PRIVATE,
            onClick = { onSelectFilter(MyReviewVisibility.PRIVATE) },
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (selected) GrayColor.C500 else GrayColor.C100
    val labelColor = if (selected) CommonColor.White else CommonColor.Black

    Row(
        modifier =
            modifier
                .height(30.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .semantics { this.selected = selected }
                .clickable(role = Role.Tab, onClick = onClick)
                .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = label,
            style = AppTextStyle.B2,
            color = labelColor,
        )
        AppText(
            text = stringResource(Res.string.my_reviews_filter_number, count),
            modifier = Modifier.padding(start = 6.dp),
            style = AppTextStyle.B2,
            color = labelColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MyReviewFilterHeaderPreview() {
    RamapTheme {
        MyReviewFilterHeader(
            state =
                MyReviewsUiState(
                    filter = MyReviewVisibility.ALL,
                    totalCount = 112,
                    publicCount = 15,
                    privateCount = 97,
                    profileIsPublic = false,
                ),
            onSelectFilter = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
