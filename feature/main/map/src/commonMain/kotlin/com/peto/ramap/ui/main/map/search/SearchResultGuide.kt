package com.peto.ramap.ui.main.map.search

sealed interface SearchResultGuide {
    data object SearchEmpty : SearchResultGuide

    data object FilterEmpty : SearchResultGuide

    data object QueryAndFilterEmpty : SearchResultGuide

    data object HiddenOnly : SearchResultGuide
}
