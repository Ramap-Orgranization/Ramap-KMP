package com.peto.ramap.ui.main.notice

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
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
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.ShopListEmptyContent
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.preview.OperatingNoticePreviewParameterProvider
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.notice.component.OngoingOperatingNoticeShop
import com.peto.ramap.ui.main.notice.component.OperatingNoticeBottomSheet
import com.peto.ramap.ui.main.notice.component.OperatingNoticeSectionTitle
import com.peto.ramap.ui.main.notice.component.OperatingNoticeSkeleton
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_open
import ramap.shared.generated.resources.ic_event_fab
import ramap.shared.generated.resources.laduck_error_crying
import ramap.shared.generated.resources.operating_notice_empty
import ramap.shared.generated.resources.operating_notice_error_description
import ramap.shared.generated.resources.operating_notice_error_title
import ramap.shared.generated.resources.operating_notice_section

@Composable
internal fun OperatingNoticeScreen(
    uiState: OperatingNoticeUiState,
    onBack: () -> Unit,
    onEventListClick: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onShopClick: (String) -> Unit,
    isSourceUrlSupported: (String) -> Boolean,
    onSourceClick: (String) -> Unit,
) {
    val backEventState = rememberNavigationEventState<NavigationEventInfo>(NavigationEventInfo.None)
    val pullToRefreshState = rememberPullToRefreshState()
    var selectedNotice by remember { mutableStateOf<OperatingNotice?>(null) }

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

                    uiState.operatingNotices.isEmpty() ->
                        ShopListEmptyContent(
                            title = stringResource(Res.string.operating_notice_empty),
                            modifier = Modifier.fillMaxSize(),
                        )

                    else ->
                        Box {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (uiState.operatingNotices.isNotEmpty()) {
                                    item {
                                        OperatingNoticeSectionTitle(
                                            title = stringResource(Res.string.operating_notice_section),
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                        )
                                    }
                                    item {
                                        FlowRow(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 5.dp),
                                            horizontalArrangement =
                                                Arrangement.spacedBy(
                                                    10.dp,
                                                    Alignment.CenterHorizontally,
                                                ),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            uiState.operatingNotices.forEach { notice ->
                                                OngoingOperatingNoticeShop(
                                                    notice = notice,
                                                    onClick = { selectedNotice = notice },
                                                )
                                            }
                                            repeat(10) {
                                                Spacer(
                                                    modifier = Modifier.width(72.dp).height(0.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
            }
                FloatingActionButton(
                    onClick = onEventListClick,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp),
                    shape = CircleShape,
                    containerColor = CommonColor.White,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_event_fab),
                        contentDescription = stringResource(Res.string.event_list_open),
                    )
                }
        }
    }
    selectedNotice?.let { notice ->
        OperatingNoticeBottomSheet(
            notice = notice,
            isSourceUrlSupported = isSourceUrlSupported,
            onSourceClick = onSourceClick,
            onShopClick = onShopClick,
            onDismiss = { selectedNotice = null },
        )
    }
}

@Preview
@Composable
private fun OperatingNoticeScreenPreview(
    @PreviewParameter(OperatingNoticePreviewParameterProvider::class)
    notices: List<OperatingNotice>,
) {
    RamapTheme {
        OperatingNoticeScreen(
            uiState = OperatingNoticeUiState(operatingNotices = notices),
            onBack = {},
            onEventListClick = {},
            onRefresh = {},
            onRetry = {},
            onShopClick = {},
            isSourceUrlSupported = { true },
            onSourceClick = {},
        )
    }
}
