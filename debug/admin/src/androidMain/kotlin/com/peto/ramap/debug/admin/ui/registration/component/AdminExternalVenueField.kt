package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.data.model.AdminExternalVenue
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminExternalVenueField(
    venueName: String,
    venues: List<AdminExternalVenue>,
    onNewVenueNameChanged: (String) -> Unit,
    onVenueSelected: (AdminExternalVenue) -> Unit,
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var showSuggestions by remember { mutableStateOf(initiallyExpanded) }
    val query = venueName.trim()
    val suggestions = venues.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }

    ExposedDropdownMenuBox(
        expanded = showSuggestions && suggestions.isNotEmpty(),
        onExpandedChange = { showSuggestions = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = venueName,
            onValueChange = {
                onNewVenueNameChanged(it)
                showSuggestions = true
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            placeholder = { AppText(stringResource(R.string.admin_registration_venue_name_placeholder), style = AppTextStyle.B2, color = GrayColor.C200) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrayColor.C200, unfocusedBorderColor = GrayColor.C200, cursorColor = GrayColor.C500),
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = showSuggestions && suggestions.isNotEmpty(), onDismissRequest = { showSuggestions = false }, modifier = Modifier.fillMaxWidth()) {
            suggestions.forEach { venue ->
                DropdownMenuItem(
                    text = { AppText(listOfNotNull(venue.name, venue.address).joinToString(" · "), style = AppTextStyle.B2, color = GrayColor.C500) },
                    onClick = {
                        onVenueSelected(venue)
                        showSuggestions = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Preview(name = "저장된 외부 장소 선택", showBackground = true)
@Composable
private fun AdminExternalVenueFieldStoredPreview() {
    RamapTheme {
        AdminExternalVenueField(
            venueName = "라멘 페스티벌",
            venues = previewExternalVenues(),
            onNewVenueNameChanged = {},
            onVenueSelected = {},
            initiallyExpanded = true,
        )
    }
}

@Preview(name = "새 외부 장소 입력", showBackground = true)
@Composable
private fun AdminExternalVenueFieldNewPreview() {
    RamapTheme {
        AdminExternalVenueField(
            venueName = "새 행사 장소",
            venues = previewExternalVenues(),
            onNewVenueNameChanged = {},
            onVenueSelected = {},
        )
    }
}

internal fun previewExternalVenues() =
    listOf(
        AdminExternalVenue(
            id = "venue-ramen-festival",
            name = "라멘 페스티벌",
            address = "서울시 마포구 월드컵로 1",
            instagramUrl = "https://www.instagram.com/ramen_festival/",
            naverMapUrl = "https://map.naver.com/p/entry/place/1",
            kakaoMapUrl = "https://place.map.kakao.com/1",
        ),
    )
