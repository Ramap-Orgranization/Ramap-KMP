package com.peto.ramap.ui.review.other

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.login.LoginButton
import com.peto.ramap.designsystem.dialog.LoginGuideDialog
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.profile.ProfileHeader
import com.peto.ramap.designsystem.review.ReviewCard
import com.peto.ramap.designsystem.review.ReviewEmptyContent
import com.peto.ramap.designsystem.review.ReviewPage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.paging.ObserveLoadMoreNearListEnd
import com.peto.ramap.ui.review.other.component.OtherReviewsErrorContent
import com.peto.ramap.ui.review.other.component.PrivateProfileCard
import com.peto.ramap.ui.review.other.component.ProfileBlockConfirmDialog
import com.peto.ramap.ui.review.other.component.ProfileBlockOverflowMenu
import com.peto.ramap.ui.review.other.contract.OtherReviewsEffect
import com.peto.ramap.ui.review.other.contract.OtherReviewsIntent
import com.peto.ramap.ui.review.other.contract.OtherReviewsLoadKey
import com.peto.ramap.ui.review.other.contract.OtherReviewsUiState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_profile_blocked
import ramap.shared.generated.resources.my_reviews_summary
import ramap.shared.generated.resources.review_blocked_empty
import ramap.shared.generated.resources.review_profile_private
import ramap.shared.generated.resources.review_profile_title
import ramap.shared.generated.resources.review_profile_unavailable
import ramap.shared.generated.resources.shop_review_empty

@Composable
fun OtherReviewsRoute(
    userId: String,
    onBack: () -> Unit,
    onLoginTypeSelected: (LoginType) -> Unit,
    onShowShop: (String) -> Unit,
    viewModel: OtherReviewsViewModel = koinViewModel(),
    toastManager: ToastManager = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLoginGuide by remember { mutableStateOf(false) }
    ObserveAsEvents(viewModel.sideEffect) { effect ->
        when (effect) {
            OtherReviewsEffect.LoginRequired -> showLoginGuide = true
            is OtherReviewsEffect.ShowToast -> toastManager.show(effect.data)
        }
    }
    LaunchedEffect(userId) {
        viewModel.dispatch(OtherReviewsIntent.OpenProfile(userId))
    }
    OtherReviewsContent(
        state = state,
        onBack = onBack,
        onShowShop = onShowShop,
        onIntent = viewModel::dispatch,
    )
    LoginGuideDialog(
        visible = showLoginGuide,
        onDismiss = { showLoginGuide = false },
        onLoginTypeSelected = { type ->
            showLoginGuide = false
            onLoginTypeSelected(type)
        },
        loginButton = { type, onClick -> LoginButton(type, onClick) },
    )
}

@Composable
internal fun OtherReviewsContent(
    state: OtherReviewsUiState,
    onBack: () -> Unit,
    onShowShop: (String) -> Unit,
    onIntent: (OtherReviewsIntent) -> Unit,
) {
    val listState = rememberLazyListState()
    var confirmBlockAction by remember(state.userId) { mutableStateOf(false) }
    val requestBlockAction: () -> Unit = {
        if (state.currentUserId == null) {
            onIntent(OtherReviewsIntent.ToggleBlock)
        } else {
            confirmBlockAction = true
        }
    }

    ObserveLoadMoreNearListEnd(
        listState = listState,
        itemThreshold = PREFETCH_ITEM_THRESHOLD,
        hasMore = state.hasMore,
        isLoading = state.loading,
        onLoadMore = { onIntent(OtherReviewsIntent.LoadMore) },
    )

    ReviewPage(
        title = stringResource(Res.string.review_profile_title),
        onBack = onBack,
        action = {
            if (state.showsBlockAction || state.showsBlockedContent) {
                ProfileBlockOverflowMenu(
                    userId = state.userId,
                    isBlocked = state.isBlocked,
                    enabled = !state.blocking,
                    onToggleBlock = requestBlockAction,
                )
            }
        },
    ) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (state.showsProfileHeader) {
                item {
                    state.profile?.let { profile ->
                        ProfileHeader(profile = profile)
                    }
                }
            }
            if (state.showsSummaryHeader) {
                item {
                    AppText(
                        text = stringResource(Res.string.my_reviews_summary),
                        style = AppTextStyle.T2,
                        color = GrayColor.C500,
                        modifier = Modifier.padding(start = 20.dp),
                    )
                }
            }
            if (state.showsInitialLoading) {
                item {
                    RamenLoadingIndicator(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .fillParentMaxHeight(),
                    )
                }
            }
            if (state.failed) {
                item {
                    OtherReviewsErrorContent(
                        onRetry = { onIntent(OtherReviewsIntent.Retry) },
                        modifier = Modifier.fillParentMaxHeight(0.5f),
                    )
                }
            }
            if (state.showsUnavailableProfile) {
                item {
                    PrivateProfileCard(
                        text =
                            stringResource(
                                if (state.isPrivate) Res.string.review_profile_private else Res.string.review_profile_unavailable,
                            ),
                        modifier = Modifier.fillParentMaxHeight(),
                    )
                }
            }
            if (state.showsBlockedContent) {
                item {
                    PrivateProfileCard(
                        text = stringResource(Res.string.review_blocked_empty),
                        image = Res.drawable.ic_profile_blocked,
                        modifier = Modifier.fillParentMaxHeight(0.65f),
                    )
                }
            }
            if (state.showsEmptyReviews) {
                item {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 30.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = GrayColor.C200,
                        )
                        ReviewEmptyContent(
                            text = stringResource(Res.string.shop_review_empty),
                        )
                    }
                }
            }
            items(
                items = state.reviews,
                key = { it.id },
            ) { review ->
                ReviewCard(
                    review = review,
                    onShopClick = { onShowShop(review.shopId) },
                )
            }
            if (state.showsAppendLoading) {
                item {
                    RamenLoadingIndicator(
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                    )
                }
            }
        }
    }
    ProfileBlockConfirmDialog(
        visible = confirmBlockAction && state.currentUserId != null,
        isBlocked = state.isBlocked,
        nickname = state.profile?.nickname.orEmpty(),
        blocking = state.blocking,
        onDismiss = { confirmBlockAction = false },
        onConfirm = {
            onIntent(OtherReviewsIntent.ToggleBlock)
            confirmBlockAction = false
        },
    )
}

private const val PREFETCH_ITEM_THRESHOLD = 3

@Preview(showBackground = true)
@Composable
private fun OtherReviewsContentPreview() {
    RamapTheme {
        OtherReviewsContent(
            state =
                OtherReviewsUiState(
                    profileAccess =
                        ProfileAccess.Visible(
                            PublicProfile(userId = "preview", nickname = "느긋한차슈", bio = "라멘을 좋아해요"),
                        ),
                    reviews =
                        listOf(
                            Review(
                                id = "review-1",
                                shopId = "shop-1",
                                body = "국물이 아주 진하고 맛있는 돈코츠 라멘입니다.",
                                createdAt = "2026-09-30T12:00:00Z",
                                author = ReviewAuthor("author", "느긋한차슈"),
                            ),
                        ),
                ),
            onBack = {},
            onShowShop = {},
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OtherReviewsContentLoadingPreview() {
    RamapTheme {
        OtherReviewsContent(
            state =
                OtherReviewsUiState(
                    loadState = LoadState.loading(OtherReviewsLoadKey.Page),
                ),
            onBack = {},
            onShowShop = {},
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OtherReviewsContentEmptyPreview() {
    RamapTheme {
        OtherReviewsContent(
            state =
                OtherReviewsUiState(
                    profileAccess =
                        ProfileAccess.Visible(
                            PublicProfile(userId = "preview", nickname = "느긋한차슈", bio = "라멘을 좋아해요"),
                        ),
                    reviews = emptyList(),
                ),
            onBack = {},
            onShowShop = {},
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OtherReviewsContentBlockedPreview() {
    RamapTheme {
        OtherReviewsContent(
            state =
                OtherReviewsUiState(
                    userId = "preview",
                    currentUserId = "me",
                    profileAccess = ProfileAccess.Blocked(PublicProfile(userId = "preview", nickname = "느긋한차슈")),
                ),
            onBack = {},
            onShowShop = {},
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OtherReviewsContentPrivatePreview() {
    RamapTheme {
        OtherReviewsContent(
            state =
                OtherReviewsUiState(
                    userId = "preview",
                    profileAccess = ProfileAccess.Private,
                ),
            onBack = {},
            onShowShop = {},
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OtherReviewsContentUnavailablePreview() {
    RamapTheme {
        OtherReviewsContent(
            state =
                OtherReviewsUiState(
                    profileAccess = ProfileAccess.Unavailable,
                ),
            onBack = {},
            onShowShop = {},
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OtherReviewsContentFailedPreview() {
    RamapTheme {
        OtherReviewsContent(
            state =
                OtherReviewsUiState(
                    profileAccess =
                        ProfileAccess.Visible(
                            PublicProfile(userId = "preview", nickname = "느긋한차슈", bio = "라멘을 좋아해요"),
                        ),
                    failed = true,
                ),
            onBack = {},
            onShowShop = {},
            onIntent = {},
        )
    }
}
