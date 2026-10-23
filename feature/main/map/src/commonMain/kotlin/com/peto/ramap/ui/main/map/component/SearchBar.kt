package com.peto.ramap.ui.main.map.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.peto.ramap.designsystem.component.AppSearchBar
import com.peto.ramap.domain.model.shop.SearchQuery

@Composable
internal fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isSearchMode: Boolean = false,
) {
    var internalValue by rememberSaveable(query) { mutableStateOf(query) }

    AppSearchBar(
        query = internalValue,
        onQueryChange = { value ->
            val limitedValue = value.take(SearchQuery.MAX_LENGTH)
            internalValue = limitedValue

            if (limitedValue.trim().length < SearchQuery.MIN_LENGTH) {
                onQueryChange(limitedValue)
            }
        },
        onFocusChanged = onFocusChanged,
        isSearchMode = isSearchMode,
        modifier = modifier,
        onSearch = {
            val trimmed = internalValue.trim()
            if (trimmed.length in SearchQuery.MIN_LENGTH..SearchQuery.MAX_LENGTH) {
                onQueryChange(trimmed)
            }
        },
    )
}
