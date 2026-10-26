package com.peto.ramap.ui.main.feed.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.peto.ramap.designsystem.button.AppFilterChip
import com.peto.ramap.designsystem.menu.AppDropdownMenu
import com.peto.ramap.designsystem.menu.AppDropdownMenuItem
import com.peto.ramap.domain.model.feed.ReviewFeedSort
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.feed_sort_latest
import ramap.shared.generated.resources.feed_sort_popular
import ramap.shared.generated.resources.ic_keyboard_arrow_down

@Composable
internal fun FeedSortSelector(
    selected: ReviewFeedSort,
    onSelect: (ReviewFeedSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val options =
        listOf(
            ReviewFeedSort.LATEST to Res.string.feed_sort_latest,
            ReviewFeedSort.POPULAR to Res.string.feed_sort_popular,
        )
    Box(modifier = modifier) {
        AppFilterChip(
            label = stringResource(options.first { it.first == selected }.second),
            trailingIcon = Res.drawable.ic_keyboard_arrow_down,
            onClick = { expanded = true },
        )
        AppDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for ((sort, label) in options) {
                AppDropdownMenuItem(
                    text = stringResource(label),
                    onClick = {
                        expanded = false
                        onSelect(sort)
                    },
                )
            }
        }
    }
}
