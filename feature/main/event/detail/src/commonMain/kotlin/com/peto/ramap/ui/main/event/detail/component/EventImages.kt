package com.peto.ramap.ui.main.event.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.GrayColor

@Composable
internal fun EventImages(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
) {
    if (imageUrls.isEmpty()) return

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (imageUrls.size == 1) {
            EventImage(
                url = imageUrls.single(),
                modifier = Modifier.fillMaxWidth().aspectRatio(EVENT_IMAGE_ASPECT_RATIO),
            )
        } else {
            val pagerState = rememberPagerState(pageCount = { imageUrls.size })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().aspectRatio(EVENT_IMAGE_ASPECT_RATIO),
            ) { page ->
                EventImage(url = imageUrls[page], modifier = Modifier.fillMaxSize())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                imageUrls.indices.forEach { page ->
                    Box(
                        modifier =
                            Modifier
                                .size(if (page == pagerState.currentPage) 8.dp else 6.dp)
                                .background(
                                    color = if (page == pagerState.currentPage) GrayColor.C500 else GrayColor.C200,
                                    shape = CircleShape,
                                ),
                    )
                }
            }
        }
    }
}

private const val EVENT_IMAGE_ASPECT_RATIO = 0.9f
