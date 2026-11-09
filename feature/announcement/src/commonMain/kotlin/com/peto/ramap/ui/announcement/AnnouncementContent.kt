package com.peto.ramap.ui.announcement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.NewBadge
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.topbar.BackTopBar
import com.peto.ramap.domain.model.announcement.Announcement
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.announcement.component.AnnouncementDialog
import com.peto.ramap.ui.announcement.contract.AnnouncementIntent
import com.peto.ramap.ui.announcement.contract.AnnouncementUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.announcement_empty
import ramap.shared.generated.resources.announcement_error_description
import ramap.shared.generated.resources.announcement_error_title
import ramap.shared.generated.resources.announcement_title
import ramap.shared.generated.resources.notification_load_error_illustration
import kotlin.time.Instant

@Composable
internal fun AnnouncementContent(
    state: AnnouncementUiState,
    onBack: () -> Unit,
    onIntent: (AnnouncementIntent) -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        BackTopBar(
            title = stringResource(Res.string.announcement_title),
            onBack = onBack,
        )
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(AnnouncementIntent.Refresh) },
            modifier = Modifier.weight(1f),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = state.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = CommonColor.White,
                    color = CommonColor.Black,
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                if (state.failed) {
                    item {
                        LoadErrorContent(
                            image = Res.drawable.notification_load_error_illustration,
                            title = stringResource(Res.string.announcement_error_title),
                            description = stringResource(Res.string.announcement_error_description),
                            onRetry = { onIntent(AnnouncementIntent.Retry) },
                            modifier =
                                if (state.announcements.isEmpty()) {
                                    Modifier.fillParentMaxSize()
                                } else {
                                    Modifier.fillMaxWidth().padding(vertical = 16.dp)
                                },
                            compact = state.announcements.isNotEmpty(),
                        )
                    }
                }
                items(
                    items = state.announcements,
                    key = { it.id },
                ) { announcement ->
                    AnnouncementRow(
                        announcement = announcement,
                        isUnread = announcement.isUnread(state.readIds),
                        onClick = { onIntent(AnnouncementIntent.Open(announcement.id)) },
                    )
                    HorizontalDivider(color = GrayColor.C100)
                }
            }
            if (state.announcements.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        state.loading -> CircularProgressIndicator(color = CommonColor.Black)
                        !state.failed && !state.isRefreshing ->
                            AppText(
                                text = stringResource(Res.string.announcement_empty),
                                style = AppTextStyle.B2,
                                color = GrayColor.C400,
                            )
                    }
                }
            }
        }
    }
    state.selected?.let { announcement ->
        AnnouncementDialog(
            announcement = announcement,
            onDismiss = { onIntent(AnnouncementIntent.Dismiss) },
        )
    }
}

@Composable
private fun AnnouncementRow(
    announcement: Announcement,
    isUnread: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AppText(
                text = announcement.title,
                style = AppTextStyle.B1,
                color = GrayColor.C500,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            AppText(
                text = announcement.formattedPublishedDate("-"),
                style = AppTextStyle.C1,
                color = GrayColor.C400,
            )
        }
        if (isUnread) NewBadge(backgroundColor = ChromaticColor.Orange400)
    }
}

@Preview(showBackground = true)
@Composable
private fun AnnouncementContentPreview() {
    RamapTheme {
        AnnouncementContent(
            state =
                AnnouncementUiState(
                    announcements =
                        listOf(
                            Announcement(
                                id = "1",
                                title = "라맵 1.2.0 업데이트 안내",
                                bodyMarkdown = "안녕하세요. 라맵입니다.\n\n새로운 기능이 추가되었습니다.",
                                publishedAt = Instant.parse("2026-11-01T00:00:00Z"),
                            ),
                            Announcement(
                                id = "2",
                                title = "서비스 점검 안내",
                                bodyMarkdown = "서비스 점검이 진행될 예정입니다.",
                                publishedAt = Instant.parse("2026-10-25T00:00:00Z"),
                            ),
                        ),
                    readIds = setOf("2"),
                ),
            onBack = {},
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AnnouncementContentErrorPreview() {
    RamapTheme {
        AnnouncementContent(
            state = AnnouncementUiState(failed = true),
            onBack = {},
            onIntent = {},
        )
    }
}
