package com.peto.ramap.ui.profile.follow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.review.ReviewEmptyContent
import com.peto.ramap.designsystem.review.ReviewPage
import com.peto.ramap.designsystem.tab.AppSegmentedTab
import com.peto.ramap.designsystem.tab.AppSegmentedTabs
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.community.FollowAction
import com.peto.ramap.domain.model.community.FollowList
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.paging.ObserveLoadMoreNearListEnd
import com.peto.ramap.ui.profile.follow.component.FollowUserRow
import com.peto.ramap.ui.profile.follow.component.RemoveFollowerDialog
import com.peto.ramap.ui.resource.FollowResourceMapper
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.follow_count
import ramap.shared.generated.resources.follow_empty
import ramap.shared.generated.resources.follow_empty_illustration
import ramap.shared.generated.resources.follow_manage
import ramap.shared.generated.resources.profile_retry

@Composable
fun FollowContent(
    state: FollowState,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onIntent: (FollowIntent) -> Unit,
) {
    var removeTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    val removeTarget = state.profiles.firstOrNull { it.userId == removeTargetId }
    LaunchedEffect(state.currentUserId, state.selectedList) { removeTargetId = null }
    LaunchedEffect(removeTarget) { if (removeTarget == null) removeTargetId = null }
    Box(modifier = Modifier.fillMaxSize().background(CommonColor.White)) {
        ReviewPage(title = stringResource(Res.string.follow_manage), onBack = onBack) {
            val lists = state.availableLists
            val tabs =
                lists.map { list ->
                    AppSegmentedTab(
                        label = stringResource(FollowResourceMapper.listLabel(list)),
                        count = state.counts?.count(list)?.let { stringResource(Res.string.follow_count, it) },
                        hasCountBadge = list == FollowList.REQUESTS,
                    )
                }
            AppSegmentedTabs(
                tabs = tabs,
                selectedIndex = lists.indexOf(state.selectedList),
                onSelect = { onIntent(FollowIntent.Select(lists[it])) },
                enabled = !state.mutating,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            )
            if (state.countsFailed) {
                AppButton(
                    text = stringResource(Res.string.profile_retry),
                    onClick = { onIntent(FollowIntent.Retry) },
                    enabled = !state.mutating,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                )
            }
            key(state.selectedList) {
                val listState = rememberLazyListState()
                ObserveLoadMoreNearListEnd(
                    listState = listState,
                    itemThreshold = 3,
                    hasMore = state.hasMore && !state.failed,
                    isLoading = state.loading || state.mutating,
                    onLoadMore = { onIntent(FollowIntent.LoadMore) },
                )
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(state.profiles, key = { it.userId }) { profile ->
                        FollowUserRow(
                            profile = profile,
                            list = state.selectedList,
                            enabled = !state.mutating,
                            actingAction = state.actingAction.takeIf { state.actingUserId == profile.userId },
                            onOpenProfile = { onOpenProfile(profile.userId) },
                            onRemove = { removeTargetId = profile.userId },
                            onAction = { action -> onIntent(FollowIntent.Change(profile.userId, action)) },
                        )
                    }
                    if (state.loading) item { RamenLoadingIndicator(modifier = Modifier.fillMaxWidth()) }
                    if (state.failed) {
                        item {
                            AppButton(text = stringResource(Res.string.profile_retry), onClick = { onIntent(FollowIntent.Retry) })
                        }
                    }
                    if (!state.loading && !state.failed && state.profiles.isEmpty()) {
                        item {
                            if (state.selectedList == FollowList.REQUESTS) {
                                AppText(
                                    text = stringResource(Res.string.follow_empty),
                                    style = AppTextStyle.B2,
                                    color = GrayColor.C300,
                                    modifier = Modifier.padding(vertical = 32.dp),
                                )
                            } else {
                                ReviewEmptyContent(
                                    text = stringResource(FollowResourceMapper.emptyLabel(state.selectedList)),
                                    image = Res.drawable.follow_empty_illustration,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        RemoveFollowerDialog(
            visible = removeTarget != null,
            removing = state.mutating,
            onDismiss = { removeTargetId = null },
            onConfirm = { removeTarget?.let { onIntent(FollowIntent.Change(it.userId, FollowAction.REMOVE)) } },
        )
    }
}
