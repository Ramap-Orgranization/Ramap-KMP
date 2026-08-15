package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.main.map.model.search.SearchResultGuide
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.search_result_empty_message
import ramap.shared.generated.resources.search_result_filter_empty_message
import ramap.shared.generated.resources.search_result_hidden_only_message
import ramap.shared.generated.resources.search_result_query_filter_empty_message

@Composable
internal fun SearchResultGuide(
    guide: SearchResultGuide,
    modifier: Modifier = Modifier,
) {
    val message =
        when (guide) {
            SearchResultGuide.SearchEmpty -> stringResource(Res.string.search_result_empty_message)
            SearchResultGuide.FilterEmpty -> stringResource(Res.string.search_result_filter_empty_message)
            SearchResultGuide.QueryAndFilterEmpty ->
                stringResource(Res.string.search_result_query_filter_empty_message)
            SearchResultGuide.HiddenOnly -> stringResource(Res.string.search_result_hidden_only_message)
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = message,
            style = AppTextStyle.B1,
            color = GrayColor.C400,
        )
    }
}
