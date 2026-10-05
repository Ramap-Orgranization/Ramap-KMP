package com.peto.ramap.designsystem.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_photo_description

@Composable
fun ReviewCardImages(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    photoViewerEnabled: Boolean = true,
) {
    if (imageUrls.isEmpty()) return
    var selectedIndex by remember(imageUrls, photoViewerEnabled) { mutableStateOf<Int?>(null) }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement =
            if (imageUrls.size == 1) Arrangement.Center else Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        itemsIndexed(imageUrls) { index, url ->
            ReviewPhoto(
                url = url,
                contentDescription =
                    stringResource(
                        Res.string.review_photo_description,
                        index + 1,
                    ),
                modifier =
                    Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (photoViewerEnabled) {
                                Modifier.clickable(role = Role.Button) { selectedIndex = index }
                            } else {
                                Modifier
                            },
                        ),
            )
        }
    }
    selectedIndex?.let { index ->
        imageUrls.getOrNull(index)?.let { url ->
            ReviewPhotoViewer(
                url = url,
                contentDescription = stringResource(Res.string.review_photo_description, index + 1),
                onDismiss = { selectedIndex = null },
            )
        }
    }
}
