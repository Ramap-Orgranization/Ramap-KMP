package com.peto.ramap.ui.main.notice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.designsystem.button.AppFloatingActionButton
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.NewsFilters
import com.peto.ramap.designsystem.component.ShopsEmptyContent
import com.peto.ramap.designsystem.notice.OperatingNoticeBottomSheet
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeFeed
import com.peto.ramap.domain.model.notice.OperatingNoticeFeedItem
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.preview.OperatingNoticePreviewParameterProvider
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
import ramap.shared.generated.resources.event_list_open
import ramap.shared.generated.resources.ic_event_fab
import ramap.shared.generated.resources.laduck_error_crying
import ramap.shared.generated.resources.operating_notice_empty
import ramap.shared.generated.resources.operating_notice_error_description
import ramap.shared.generated.resources.operating_notice_error_title
import ramap.shared.generated.resources.operating_notice_scheduled_section
import ramap.shared.generated.resources.operating_notice_today_section
import ramap.shared.generated.resources.search_result_empty_message

@Composable
internal fun OperatingNoticeScreen(
    uiState: OperatingNoticeUiState,
    onBack: () -> Unit,
    onEventListClick: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onShopClick: (String) -> Unit,
    onNoticeClick: (OperatingNotice) -> Unit,
    onQueryChanged: (String) -> Unit = {},
    onSavedToggled: () -> Unit = {},
    onPersonalizationRetry: () -> Unit = {},
    isSourceUrlSupported: (String) -> Boolean,
    onSourceClick: (String) -> Unit,
) {
    val todayTitle = stringResource(Res.string.operating_notice_today_section)
    val scheduledTitle = stringResource(Res.string.operating_notice_scheduled_section)
    val backEventState = rememberNavigationEventState<NavigationEventInfo>(NavigationEventInfo.None)
    val pullToRefreshState = rememberPullToRefreshState()
    var selectedNotice by remember { mutableStateOf<OperatingNoticeSelection?>(null) }

    NavigationBackHandler(
        state = backEventState,
        isBackEnabled = true,
        onBackCompleted = onBack,
    )
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CommonColor.White)
                .statusBarsPadding(),
    ) {
        NewsFilters(
            query = uiState.query,
            savedOnly = uiState.savedOnly,
            onQueryChanged = onQueryChanged,
            onSavedToggled = onSavedToggled,
        )
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
                            image = Res.drawable.laduck_error_crying,
                            title = stringResource(Res.string.operating_notice_error_title),
                            description = stringResource(Res.string.operating_notice_error_description),
                            onRetry = onRetry,
                            modifier = Modifier.fillMaxSize(),
                        )

                    uiState.savedOnly && uiState.personalizationState == PersonalizationBootstrapState.Loading ->
                        OperatingNoticeSkeleton(Modifier.fillMaxSize())
                    uiState.savedFilterError || (uiState.savedOnly && uiState.personalizationState == PersonalizationBootstrapState.Error) ->
                        LoadErrorContent(
                            image = Res.drawable.laduck_error_crying,
                            title = stringResource(Res.string.operating_notice_error_title),
                            description = stringResource(Res.string.operating_notice_error_description),
                            onRetry = onPersonalizationRetry,
                            modifier = Modifier.fillMaxSize(),
                        )

                    uiState.todayShops.isEmpty() && uiState.scheduledShops.isEmpty() ->
                        ShopsEmptyContent(
                            title =
                                stringResource(
                                    if (uiState.query.isNotBlank()) Res.string.search_result_empty_message else Res.string.operating_notice_empty,
                                ),
                            modifier = Modifier.fillMaxSize(),
                        )

                    else ->
                        Box {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
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
            AppFloatingActionButton(
                icon = Res.drawable.ic_event_fab,
                contentDescription = stringResource(Res.string.event_list_open),
                onClick = onEventListClick,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                containerColor = CommonColor.White,
            )
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
    listScope.item {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            notices.forEach { notice ->
                OngoingOperatingNoticeShop(notice = notice, onClick = { onNoticeClick(notice) })
            }
            repeat(10) { Spacer(modifier = Modifier.width(72.dp).height(0.dp)) }
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

@Preview(name = "검색 결과 없음")
@Composable
private fun OperatingNoticeSearchEmptyPreview() {
    OperatingNoticePreviewContent(previewState(OperatingNoticePreviewParameterProvider().values.first()).copy(query = "없는 매장"))
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
            onBack = {},
            onEventListClick = {},
            onRefresh = {},
            onRetry = {},
            onShopClick = {},
            onNoticeClick = {},
            isSourceUrlSupported = { true },
            onSourceClick = {},
        )
    }
}
