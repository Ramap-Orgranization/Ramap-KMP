package com.peto.ramap.designsystem.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppIconButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.review_close
import ramap.shared.generated.resources.review_photo_description
import ramap.shared.generated.resources.review_photo_position

@Composable
internal fun ReviewPhotoViewer(
    imageUrls: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { imageUrls.size })
    val scope = rememberCoroutineScope()
    val hasMultiplePhotos = imageUrls.size > 1

    ReviewPhotoViewerDialog(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(CommonColor.Black)
                    .safeDrawingPadding(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (hasMultiplePhotos) {
                    AppText(
                        text = stringResource(Res.string.review_photo_position, pagerState.currentPage + 1, imageUrls.size),
                        style = AppTextStyle.B2,
                        color = CommonColor.White,
                    )
                }
                AppIconButton(
                    icon = Res.drawable.ic_close,
                    contentDescription = stringResource(Res.string.review_close),
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    tint = CommonColor.White,
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                userScrollEnabled = hasMultiplePhotos,
            ) { page ->
                ReviewPhoto(
                    url = imageUrls[page],
                    contentDescription = stringResource(Res.string.review_photo_description, page + 1),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    shape = RectangleShape,
                    backgroundColor = CommonColor.Black,
                )
            }

            if (hasMultiplePhotos) {
                ReviewPhotoThumbnails(
                    imageUrls = imageUrls,
                    selectedPage = pagerState.currentPage,
                    onPhotoSelected = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
                )
            }
        }
    }
}

@Composable
internal expect fun ReviewPhotoViewerDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
)

@Composable
private fun ReviewPhotoThumbnails(
    imageUrls: List<String>,
    selectedPage: Int,
    onPhotoSelected: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val thumbnailShape = RoundedCornerShape(9.dp)
    LaunchedEffect(selectedPage) {
        val layout = listState.layoutInfo
        val isSelectionVisible =
            layout.visibleItemsInfo.any { item ->
                item.index == selectedPage &&
                    item.offset >= layout.viewportStartOffset &&
                    item.offset + item.size <= layout.viewportEndOffset
            }
        if (!isSelectionVisible) listState.animateScrollToItem(selectedPage)
    }

    LazyRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectableGroup(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        itemsIndexed(imageUrls) { index, url ->
            val selected = index == selectedPage
            ReviewPhoto(
                url = url,
                contentDescription = stringResource(Res.string.review_photo_description, index + 1),
                modifier =
                    Modifier
                        .size(54.dp)
                        .clip(thumbnailShape)
                        .selectable(
                            selected = selected,
                            role = Role.Button,
                            onClick = { onPhotoSelected(index) },
                        ).border(
                            width = 2.dp,
                            color = if (selected) CommonColor.White else Color.Transparent,
                            shape = thumbnailShape,
                        ).padding(4.dp)
                        .alpha(if (selected) 1f else UNSELECTED_THUMBNAIL_ALPHA),
                shape = RoundedCornerShape(5.dp),
                backgroundColor = CommonColor.Black,
            )
        }
    }
}

private const val UNSELECTED_THUMBNAIL_ALPHA = 0.65f
