package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.menu.AppDropdownMenu
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.place.PlaceSearchResults
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_chevron_right
import ramap.shared.generated.resources.ic_keyboard_arrow_down
import ramap.shared.generated.resources.ic_map_outline

@Composable
internal fun PlaceDestinationShortcut(
    places: PlaceSearchResults,
    onPlaceSelected: (PlaceSearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstPlace = places.firstOrNull() ?: return
    val hasMultiplePlaces = places.size > 1
    var isMenuExpanded by remember(places) { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier) {
        SectionCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(10.dp)),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(role = Role.Button) {
                            if (hasMultiplePlaces) {
                                isMenuExpanded = true
                            } else {
                                onPlaceSelected(firstPlace)
                            }
                        }.semantics { contentDescription = firstPlace.name }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .border(width = 1.dp, color = GrayColor.C100, shape = RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_map_outline),
                        contentDescription = null,
                        tint = ChromaticColor.Blue400,
                        modifier = Modifier.size(22.dp),
                    )
                }
                AppText(
                    text = if (hasMultiplePlaces) firstPlace.name else firstPlace.address.ifBlank { firstPlace.name },
                    style = AppTextStyle.B2,
                    color = ChromaticColor.Blue400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(if (hasMultiplePlaces) Res.drawable.ic_keyboard_arrow_down else Res.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = ChromaticColor.Blue400,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        AppDropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            modifier =
                Modifier
                    .width(maxWidth)
                    .heightIn(max = 320.dp),
        ) {
            PlaceDestinationList(
                places = places,
                onPlaceSelected = { place ->
                    isMenuExpanded = false
                    onPlaceSelected(place)
                },
            )
        }
    }
}
