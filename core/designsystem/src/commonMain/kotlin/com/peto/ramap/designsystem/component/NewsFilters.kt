package com.peto.ramap.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.BookmarkedFilterButton
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.operating_notice_saved

@Composable
fun NewsFilters(
    query: String,
    savedOnly: Boolean,
    onQueryChanged: (String) -> Unit,
    onSavedToggled: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppSearchBar(
            query = query,
            onQueryChange = onQueryChanged,
            modifier = Modifier.weight(1f),
        )
        BookmarkedFilterButton(
            isActive = savedOnly,
            onClick = onSavedToggled,
            contentDescription = stringResource(Res.string.operating_notice_saved),
        )
    }
}
