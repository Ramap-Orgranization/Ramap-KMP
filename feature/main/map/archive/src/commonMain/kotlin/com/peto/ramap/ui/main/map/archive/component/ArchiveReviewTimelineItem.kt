package com.peto.ramap.ui.main.map.archive.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.review.ReviewCardActions
import com.peto.ramap.designsystem.review.ReviewCardImages
import com.peto.ramap.designsystem.review.ReviewCardOverflowMenu
import com.peto.ramap.designsystem.review.ReviewLikeCount
import com.peto.ramap.designsystem.review.reviewDateLabel
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.preview.MyReviewPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.archive_review_date_and_visit
import ramap.shared.generated.resources.review_visit_number

@Composable
internal fun ArchiveReviewTimelineItem(
    review: Review,
    isLatest: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    actionsEnabled: Boolean,
    onPhotoDownloadFailed: (String) -> Unit,
    onPhotoDownloadSucceeded: (String) -> Unit,
    photoRetryKeys: Map<String, Int>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .drawBehind {
                    val center = Offset(x = 4.dp.toPx(), y = 20.dp.toPx())
                    drawLine(
                        color = GrayColor.C100,
                        start = Offset(x = center.x, y = if (isLatest) center.y else 0f),
                        end = Offset(x = center.x, y = size.height - if (isLast) 24.dp.toPx() else 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawCircle(
                        color = ChromaticColor.Orange400,
                        radius = 3.5.dp.toPx(),
                        center = center,
                    )
                }.padding(start = 24.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppText(
                text =
                    stringResource(
                        Res.string.archive_review_date_and_visit,
                        reviewDateLabel(review.createdAt),
                        stringResource(Res.string.review_visit_number, review.visitNumber),
                    ),
                style = AppTextStyle.B1,
                color = GrayColor.C300,
                modifier = Modifier.weight(1f),
            )
            ReviewCardOverflowMenu(
                review = review,
                actions = ReviewCardActions(onEdit = onEdit, onDelete = onDelete),
                actionsEnabled = actionsEnabled,
            )
        }
        AppText(text = review.body, style = AppTextStyle.B2, color = GrayColor.C500)
        ReviewCardImages(
            imageUrls = review.imageUrls,
            imageSize = 132.dp,
            contentPadding = PaddingValues(0.dp),
            centerSingleImage = false,
            onImageLoadFailed = { url ->
                review.images.firstOrNull { it.url == url }?.let { onPhotoDownloadFailed(it.reference) }
            },
            onImageLoadSucceeded = { url ->
                review.images.firstOrNull { it.url == url }?.let { onPhotoDownloadSucceeded(it.reference) }
            },
            imageRetryKeys = review.images.associate { it.url.orEmpty() to (photoRetryKeys[it.reference] ?: 0) },
        )
        ReviewLikeCount(count = review.likeCount)
    }
}

@Preview(name = "단일 기록", showBackground = true, widthDp = 360)
@Composable
private fun ArchiveReviewTimelineItemPreview(
    @PreviewParameter(MyReviewPreviewParameterProvider::class) row: MyReview,
) {
    RamapTheme {
        ArchiveReviewTimelineItem(
            review = row.review.copy(visitNumber = 3, likeCount = 8),
            isLatest = true,
            isLast = true,
            onEdit = {},
            onDelete = {},
            actionsEnabled = true,
            onPhotoDownloadFailed = {},
            onPhotoDownloadSucceeded = {},
            photoRetryKeys = emptyMap(),
        )
    }
}

@Preview(name = "여러 방문 기록", showBackground = true, widthDp = 360)
@Composable
private fun ArchiveReviewTimelinePreview() {
    val rows = MyReviewPreviewParameterProvider().myReviewPreviewSamples.sortedByDescending { it.review.createdAt }
    RamapTheme {
        Column {
            rows.forEachIndexed { index, row ->
                ArchiveReviewTimelineItem(
                    review = row.review.copy(visitNumber = rows.size - index, likeCount = (rows.size - index - 1) * 3),
                    isLatest = index == 0,
                    isLast = index == rows.lastIndex,
                    onEdit = {},
                    onDelete = {},
                    actionsEnabled = true,
                    onPhotoDownloadFailed = {},
                    onPhotoDownloadSucceeded = {},
                    photoRetryKeys = emptyMap(),
                )
            }
        }
    }
}
