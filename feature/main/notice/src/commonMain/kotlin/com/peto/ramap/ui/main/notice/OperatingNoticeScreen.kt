package com.peto.ramap.ui.main.notice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.BookmarkedFilterButton
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.ShopsEmptyContent
import com.peto.ramap.designsystem.notice.OperatingNoticeBottomSheet
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeFeed
import com.peto.ramap.domain.model.notice.OperatingNoticeFeedItem
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.preview.OperatingNoticePreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.notice.component.OngoingOperatingNoticeShop
import com.peto.ramap.ui.main.notice.component.OperatingNoticeSectionTitle
import com.peto.ramap.ui.main.notice.component.OperatingNoticeSkeleton
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeUiState
import com.peto.ramap.ui.main.notice.model.OperatingNoticeSelection
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.news_load_error_illustration
import ramap.shared.generated.resources.operating_notice_empty
import ramap.shared.generated.resources.operating_notice_error_description
import ramap.shared.generated.resources.operating_notice_error_title
import ramap.shared.generated.resources.operating_notice_saved
import ramap.shared.generated.resources.operating_notice_scheduled_section
import ramap.shared.generated.resources.operating_notice_today_section

@Composable
fun OperatingNoticeScreen(
    uiState: OperatingNoticeUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onShopClick: (String) -> Unit,
    onNoticeClick: (OperatingNotice) -> Unit,
    onSavedToggled: (() -> Unit)? = null,
    onPersonalizationRetry: () -> Unit = {},
    isSourceUrlSupported: (String) -> Boolean,
    onSourceClick: (String) -> Unit,
    initialNoticeId: String? = null,
    onInitialNoticeOpened: () -> Unit = {},
    headerContent: (@Composable () -> Unit)? = null,
) {
    val todayTitle = stringResource(Res.string.operating_notice_today_section)
    val scheduledTitle = stringResource(Res.string.operating_notice_scheduled_section)
    val pullToRefreshState = rememberPullToRefreshState()
    val isEmpty = uiState.todayShops.isEmpty() && uiState.scheduledShops.isEmpty()
    val showHeaderAboveContent =
        uiState.isLoading ||
            uiState.showOperatingNoticeError ||
            uiState.savedFilterError ||
            (uiState.savedOnly && uiState.personalizationState == PersonalizationBootstrapState.Loading) ||
            isEmpty
    var selectedNotice by remember { mutableStateOf<OperatingNoticeSelection?>(null) }

    LaunchedEffect(initialNoticeId, uiState) {
        val selection = initialNoticeId?.let(uiState::selectNoticeById) ?: return@LaunchedEffect
        selectedNotice = selection
        onInitialNoticeOpened()
        onNoticeClick(selection.notice)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (onSavedToggled != null && headerContent == null) {
                BookmarkedFilterButton(
                    isActive = uiState.savedOnly,
                    onClick = onSavedToggled,
                    modifier = Modifier.align(Alignment.End).padding(horizontal = 16.dp, vertical = 8.dp),
                    contentDescription = stringResource(Res.string.operating_notice_saved),
                )
            }
            if (showHeaderAboveContent) headerContent?.invoke()
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                    state = pullToRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullToRefreshState,
                            isRefreshing = uiState.isRefreshing,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = CommonColor.White,
                            color = CommonColor.Black,
                        )
                    },
                ) {
                    when {
                        uiState.isLoading -> OperatingNoticeSkeleton(Modifier.fillMaxSize())
                        uiState.showOperatingNoticeError ->
                            LoadErrorContent(
                                image = Res.drawable.news_load_error_illustration,
                                title = stringResource(Res.string.operating_notice_error_title),
                                description = stringResource(Res.string.operating_notice_error_description),
                                onRetry = onRetry,
                                modifier = Modifier.fillMaxSize(),
                            )

                        uiState.savedOnly && uiState.personalizationState == PersonalizationBootstrapState.Loading ->
                            OperatingNoticeSkeleton(Modifier.fillMaxSize())
                        uiState.savedFilterError ->
                            LoadErrorContent(
                                image = Res.drawable.news_load_error_illustration,
                                title = stringResource(Res.string.operating_notice_error_title),
                                description = stringResource(Res.string.operating_notice_error_description),
                                onRetry = onPersonalizationRetry,
                                modifier = Modifier.fillMaxSize(),
                            )

                        isEmpty ->
                            ShopsEmptyContent(
                                title = stringResource(Res.string.operating_notice_empty),
                                modifier = Modifier.fillMaxSize(),
                                titleStyle = AppTextStyle.T1,
                            )

                        else ->
                            Box {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    headerContent?.let { header ->
                                        item(key = "header") {
                                            header()
                                        }
                                    }
                                    addNoticeSection(
                                        listScope = this,
                                        title = todayTitle,
                                        notices = uiState.todayShops,
                                        onNoticeClick = { notice ->
                                            onNoticeClick(notice)
                                            selectedNotice = uiState.selectNotice(notice, uiState.referenceDate)
                                        },
                                    )
                                    addNoticeSection(
                                        listScope = this,
                                        title = scheduledTitle,
                                        notices = uiState.scheduledShops,
                                        onNoticeClick = { notice ->
                                            onNoticeClick(notice)
                                            selectedNotice = uiState.selectScheduledNotice(notice)
                                        },
                                    )
                                }
                            }
                    }
                }
            }
        }
        selectedNotice?.let { selection ->
            OperatingNoticeBottomSheet(
                notice = selection.notice,
                notices = selection.notices,
                isSourceUrlSupported = isSourceUrlSupported,
                onSourceClick = onSourceClick,
                onShopClick = onShopClick,
                onDismiss = { selectedNotice = null },
            )
        }
    }
}

private fun addNoticeSection(
    listScope: LazyListScope,
    title: String,
    notices: List<OperatingNotice>,
    onNoticeClick: (OperatingNotice) -> Unit,
) {
    if (notices.isEmpty()) return
    listScope.item {
        OperatingNoticeSectionTitle(title = title, modifier = Modifier.padding(horizontal = 16.dp))
    }
    val rows = notices.chunked(4)
    listScope.items(
        count = rows.size,
        key = { index -> "$title-row-$index" },
    ) { rowIndex ->
        val rowNotices = rows[rowIndex]
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            rowNotices.forEach { notice ->
                OngoingOperatingNoticeShop(
                    notice = notice,
                    modifier = Modifier.weight(1f),
                    onClick = { onNoticeClick(notice) },
                )
            }
            repeat(4 - rowNotices.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Preview(name = "오늘")
@Composable
private fun OperatingNoticeScreenPreview(
    @PreviewParameter(OperatingNoticePreviewParameterProvider::class)
    notices: List<OperatingNotice>,
) {
    OperatingNoticePreviewContent(previewState(notices))
}

@Preview(name = "예정")
@Composable
private fun OperatingNoticeScheduledPreview() {
    OperatingNoticePreviewContent(previewState(OperatingNoticePreviewParameterProvider().values.first()))
}

@Preview(name = "영업 변동 없음")
@Composable
private fun OperatingNoticeEmptyPreview() {
    OperatingNoticePreviewContent(previewState(emptyList()))
}

private fun previewState(notices: List<OperatingNotice>) =
    OperatingNoticeUiState(
        feed =
            OperatingNoticeFeed(
                referenceDate = LocalDate(2026, 10, 14),
                items = notices.map { OperatingNoticeFeedItem(it) },
            ),
    )

@Composable
private fun OperatingNoticePreviewContent(state: OperatingNoticeUiState) {
    RamapTheme {
        OperatingNoticeScreen(
            uiState = state,
            onRefresh = {},
            onRetry = {},
            onShopClick = {},
            onNoticeClick = {},
            isSourceUrlSupported = { true },
            onSourceClick = {},
        )
    }
}
