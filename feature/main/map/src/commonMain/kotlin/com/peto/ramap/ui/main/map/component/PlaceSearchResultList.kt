package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.place.PlaceSearchResults
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor

@Composable
internal fun SearchResultList(
    places: PlaceSearchResults,
    onPlaceClick: (PlaceSearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
    ) {
        places.forEach { place ->
            PlaceSearchResultItem(
                place = place,
                onClick = { onPlaceClick(place) },
            )
        }
    }
}

@Composable
private fun PlaceSearchResultItem(
    place: PlaceSearchResult,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppText(
            text = place.name,
            style = AppTextStyle.H3,
            color = GrayColor.C500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AppText(
            text = place.address,
            style = AppTextStyle.B2,
            color = GrayColor.C300,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
