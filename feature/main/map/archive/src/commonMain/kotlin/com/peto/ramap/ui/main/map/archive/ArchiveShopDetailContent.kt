package com.peto.ramap.ui.main.map.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppIconButton
import com.peto.ramap.designsystem.button.RetryButton
import com.peto.ramap.designsystem.dialog.ReviewDeleteDialog
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.preview.MyReviewPreviewParameterProvider
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.map.archive.component.ArchiveReviewTimelineItem
import com.peto.ramap.ui.main.map.archive.model.ArchiveShopDetailTab
import com.peto.ramap.ui.main.map.archive.model.ArchiveShopDetailUiModel
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.archive_shop_information_tab
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.my_reviews
import ramap.shared.generated.resources.my_reviews_filter_number
import ramap.shared.generated.resources.review_close

@Composable
fun ArchiveShopDetailContent(
    request: ArchiveShopDetailUiModel,
    onEditReview: (String, String) -> Unit,
    sheetContent: @Composable (LazyListState, ArchiveShopDetailTab, LazyListScope.() -> Unit) -> Unit,
    shopInformationContent: LazyListScope.() -> Unit,
) {
    key(request.shop.id) {
        var selectedTab by rememberSaveable { mutableStateOf(ArchiveShopDetailTab.RECORDS) }
        val recordListState = rememberLazyListState()
        val informationListState = rememberLazyListState()
        sheetContent(if (selectedTab == ArchiveShopDetailTab.RECORDS) recordListState else informationListState, selectedTab) {
            stickyHeader(key = "archive_detail_tabs", contentType = "tabs") {
                ArchiveDetailTabs(
                    selectedTab = selectedTab,
                    reviewCount = request.reviews.size,
                    onSelect = { selectedTab = it },
                    onDismiss = request.onDismiss,
                )
            }
            if (selectedTab == ArchiveShopDetailTab.INFORMATION) {
                shopInformationContent()
                return@sheetContent
            }
            if (request.hasReviewPhotoError) {
                item(key = "archive_photo_retry", contentType = "status") {
                    RetryButton(onClick = request.onRetryReviewPhotos, modifier = Modifier.padding(bottom = 12.dp))
                }
            }
            itemsIndexed(
                items = request.reviews,
                key = { _, row -> row.review.id },
            ) { index, row ->
                ArchiveReviewTimelineItem(
                    review = row.review,
                    isLatest = index == 0,
                    isLast = index == request.reviews.lastIndex,
                    onEdit = { onEditReview(request.shop.id, row.review.id) },
                    onDelete = { request.onRequestReviewDeletion(row.review.id) },
                    actionsEnabled = !request.isDeletingReview,
                    onPhotoDownloadFailed = request.onReviewPhotoDownloadFailed,
                    onPhotoDownloadSucceeded = request.onReviewPhotoDownloadSucceeded,
                    photoRetryKeys = request.reviewPhotoRetryKeys,
                )
            }
            if (request.isReviewPhotosLoading) {
                item(key = "archive_photos_loading", contentType = "status") {
                    RamenLoadingIndicator(modifier = Modifier.fillMaxWidth().height(48.dp))
                }
            }
        }
        ReviewDeleteDialog(
            visible = request.deleteTargetReviewId != null,
            isDeleting = request.isDeletingReview,
            onDismissRequest = request.onDismissReviewDeletion,
            onConfirm = request.onConfirmReviewDeletion,
        )
    }
}

@Composable
private fun ArchiveDetailTabs(
    selectedTab: ArchiveShopDetailTab,
    reviewCount: Int,
    onSelect: (ArchiveShopDetailTab) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(CommonColor.White)
                .padding(top = 10.dp, bottom = 12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                        .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArchiveShopDetailTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    Box(
                        modifier =
                            Modifier
                                .background(
                                    color = if (isSelected) ChromaticColor.Orange400 else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp),
                                ).border(
                                    width = 1.dp,
                                    color = if (isSelected) ChromaticColor.Orange400 else GrayColor.C100,
                                    shape = RoundedCornerShape(10.dp),
                                ).semantics { this.selected = isSelected }
                                .noRippleClickable(
                                    role = Role.Tab,
                                    onClick = { onSelect(tab) },
                                ).sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                                .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (tab) {
                            ArchiveShopDetailTab.RECORDS -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AppText(
                                        text = stringResource(Res.string.my_reviews),
                                        style = AppTextStyle.B1,
                                        color = if (isSelected) CommonColor.White else GrayColor.C500,
                                    )
                                    AppText(
                                        text = stringResource(Res.string.my_reviews_filter_number, reviewCount),
                                        style = AppTextStyle.B1,
                                        color = if (isSelected) CommonColor.White else ChromaticColor.Orange400,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }

                            ArchiveShopDetailTab.INFORMATION -> {
                                AppText(
                                    text = stringResource(Res.string.archive_shop_information_tab),
                                    style = AppTextStyle.B1,
                                    color = if (isSelected) CommonColor.White else GrayColor.C300,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
            AppIconButton(
                icon = Res.drawable.ic_close,
                contentDescription = stringResource(Res.string.review_close),
                onClick = onDismiss,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ArchiveShopDetailContentPreview() {
    val shop = RamenShopPreviewParameterProvider().ramenShopPreviewSamples.first()
    val reviews = MyReviewPreviewParameterProvider().myReviewPreviewSamples
    val request =
        ArchiveShopDetailUiModel(
            shop = shop,
            reviews = reviews,
            isReviewPhotosLoading = false,
            hasReviewPhotoError = false,
            reviewPhotoRetryKeys = emptyMap(),
            onRetryReviewPhotos = {},
            onReviewPhotoDownloadFailed = {},
            onReviewPhotoDownloadSucceeded = {},
            deleteTargetReviewId = null,
            isDeletingReview = false,
            onRequestReviewDeletion = {},
            onDismissReviewDeletion = {},
            onConfirmReviewDeletion = {},
            onDismiss = {},
        )
    RamapTheme {
        ArchiveShopDetailContent(
            request = request,
            onEditReview = { _, _ -> },
            sheetContent = { listState, _, content ->
                LazyColumn(state = listState) {
                    content()
                }
            },
            shopInformationContent = {
                item(key = "shop_info_preview") {
                    Column(modifier = Modifier.padding(20.dp)) {
                        AppText(text = "매장 정보 내용", style = AppTextStyle.B1, color = GrayColor.C500)
                    }
                }
            },
        )
    }
}
