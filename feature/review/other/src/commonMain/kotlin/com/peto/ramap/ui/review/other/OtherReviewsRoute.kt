package com.peto.ramap.ui.review.other

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.login.LoginButton
import com.peto.ramap.designsystem.component.RamenShopSummary
import com.peto.ramap.designsystem.dialog.LoginGuideDialog
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.profile.ProfileHeader
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import com.peto.ramap.designsystem.review.ReviewCard
import com.peto.ramap.designsystem.review.ReviewCardActions
import com.peto.ramap.designsystem.review.ReviewEmptyContent
import com.peto.ramap.designsystem.review.ReviewPage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.ProfileReview
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.paging.ObserveLoadMoreNearListEnd
import com.peto.ramap.ui.review.other.component.OtherProfileTabs
import com.peto.ramap.ui.review.other.component.OtherReviewsErrorContent
import com.peto.ramap.ui.review.other.component.PrivateProfileCard
import com.peto.ramap.ui.review.other.component.ProfileBlockConfirmDialog
import com.peto.ramap.ui.review.other.component.ProfileBlockOverflowMenu
import com.peto.ramap.ui.review.other.component.ProfileFollowButton
import com.peto.ramap.ui.review.other.contract.OtherReviewsEffect
import com.peto.ramap.ui.review.other.contract.OtherReviewsIntent
import com.peto.ramap.ui.review.other.contract.OtherReviewsLoadKey
import com.peto.ramap.ui.review.other.contract.OtherReviewsTab
import com.peto.ramap.ui.review.other.contract.OtherReviewsUiState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmarked_shops_empty_title
import ramap.shared.generated.resources.follow_content_private
import ramap.shared.generated.resources.follow_followers
import ramap.shared.generated.resources.follow_following
import ramap.shared.generated.resources.ic_profile_blocked
import ramap.shared.generated.resources.map_shop_detail_error_title
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
    onEventNavigate: (ShopEvent) -> Unit,
    shopDetailContent: @Composable (String, () -> Unit, (String) -> Unit, (ShopEvent) -> Unit) -> Unit,
    viewModel: OtherReviewsViewModel = koinViewModel(),
    toastManager: ToastManager = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLoginGuide by remember { mutableStateOf(false) }
    var selectedShopId by rememberSaveable(userId) { mutableStateOf<String?>(null) }
    ObserveAsEvents(viewModel.sideEffect) { effect ->
        when (effect) {
            OtherReviewsEffect.LoginRequired -> showLoginGuide = true
            is OtherReviewsEffect.ShowToast -> toastManager.show(effect.data)
        }
    }
    LaunchedEffect(userId) {
        viewModel.dispatch(OtherReviewsIntent.OpenProfile(userId))
    }
    Box(modifier = Modifier.fillMaxSize()) {
        OtherReviewsContent(
            state = state,
            onBack = onBack,
            onOpenShopDetail = { selectedShopId = it },
            onIntent = viewModel::dispatch,
        )
        selectedShopId?.let { shopId ->
            shopDetailContent(
                shopId,
                { selectedShopId = null },
                { targetShopId ->
                    selectedShopId = null
                    onShowShop(targetShopId)
                },
                { event ->
                    selectedShopId = null
                    onEventNavigate(event)
                },
            )
        }
    }
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
    onOpenShopDetail: (String) -> Unit,
    onIntent: (OtherReviewsIntent) -> Unit,
) {
    val reviewListState = rememberLazyListState()
    val savedShopsListState = rememberLazyListState()
    val listState = if (state.selectedTab == OtherReviewsTab.Reviews) reviewListState else savedShopsListState
    LaunchedEffect(state.userId) {
        reviewListState.scrollToItem(0)
        savedShopsListState.scrollToItem(0)
    }
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
        hasMore = state.activeHasMore && !state.activeFailed,
        isLoading = state.activeLoading,
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
                        ProfileHeader(
                            nickname = profile.nickname,
                            avatarUrl = profile.avatarUrl,
                            bio = profile.bio,
                        )
                    }
                }
            }
            val visibleAccess = state.profileAccess as? ProfileAccess.Visible
            if (visibleAccess != null && visibleAccess.profile.userId != state.currentUserId) {
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ProfileFollowButton(
                            state = visibleAccess.followState,
                            isLoading = state.following,
                            onClick = { onIntent(OtherReviewsIntent.ToggleFollow) },
                        )
                    }
                }
            }
            if (state.contentRestricted) {
                item {
                    PrivateProfileCard(text = stringResource(Res.string.follow_content_private))
                }
            }
            if (state.profileAccess is ProfileAccess.Visible) {
                item {
                    ProfileFollowCounts(
                        followerCount = state.profileAccess.followerCount,
                        followingCount = state.profileAccess.followingCount,
                    )
                }
                stickyHeader(key = "profile-tabs") {
                    OtherProfileTabs(
                        selectedTab = state.selectedTab,
                        reviewCount = state.profileAccess.reviewCount.takeIf { state.profileAccess.canReadReviews },
                        savedShopCount = state.profileAccess.savedShopCount.takeIf { state.profileAccess.canReadSavedShops },
                        onSelect = { onIntent(OtherReviewsIntent.SelectTab(it)) },
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
            if (state.activeFailed) {
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
                items = if (state.selectedTab == OtherReviewsTab.Reviews) state.reviews else emptyList(),
                key = { it.review.id },
            ) { profileReview ->
                ReviewCard(
                    review = profileReview.review,
                    actions =
                        ReviewCardActions(
                            onReviewClick =
                                profileReview.review.shopId.takeIf { it.isNotBlank() }?.let { shopId ->
                                    { onOpenShopDetail(shopId) }
                                },
                        ),
                    header = {
                        ProfileReviewShopHeader(
                            profileReview = profileReview,
                            onOpenShopDetail = onOpenShopDetail,
                        )
                    },
                )
            }
            if (state.showsEmptySavedShops) {
                item {
                    ReviewEmptyContent(text = stringResource(Res.string.bookmarked_shops_empty_title))
                }
            }
            items(
                items = if (state.selectedTab == OtherReviewsTab.SavedShops) state.savedShops else emptyList(),
                key = { it.id },
            ) { shop ->
                RamenShopSummary(
                    shop = shop,
                    categoryLabel = { stringResource(CategoryResourceMapper.label(it)) },
                    containerColor = CommonColor.White,
                    leadingContent = {},
                    onClick = { onOpenShopDetail(shop.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
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

@Composable
private fun ProfileReviewShopHeader(
    profileReview: ProfileReview,
    onOpenShopDetail: (String) -> Unit,
) {
    val shop = profileReview.shop
    val modifier = Modifier.padding(horizontal = 5.dp).padding(top = 8.dp)
    if (shop != null) {
        RamenShopSummary(
            shop = shop,
            categoryLabel = { stringResource(CategoryResourceMapper.label(it)) },
            containerColor = CommonColor.White,
            leadingContent = {},
            onClick = { onOpenShopDetail(shop.id) },
            modifier = modifier,
        )
        return
    }
    AppText(
        text = stringResource(Res.string.map_shop_detail_error_title),
        style = AppTextStyle.B1,
        color = GrayColor.C300,
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (profileReview.review.shopId.isNotBlank()) {
                        Modifier.noRippleClickable { onOpenShopDetail(profileReview.review.shopId) }
                    } else {
                        Modifier
                    },
                ),
    )
}

private const val PREFETCH_ITEM_THRESHOLD = 3

@Preview(showBackground = true)
@Composable
private fun OtherReviewsContentPreview() {
    val shop =
        RamenShop(
            id = "preview-shop",
            name = "라멘 키레이",
            address = "서울 마포구 동교로9길 23",
            location = Location(lat = 37.55, lng = 126.91),
            kakaoPlaceUrl = null,
            instagramUrl = null,
            menuCategories = MenuCategories(Category.entries.take(2)),
            isVisible = true,
            createdAt = "2026-09-30T12:00:00Z",
            updatedAt = "2026-09-30T12:00:00Z",
        )
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
                            ProfileReview(
                                review =
                                    Review(
                                        id = "review-1",
                                        shopId = shop.id,
                                        body = "국물이 아주 진하고 맛있는 돈코츠 라멘입니다.",
                                        createdAt = "2026-09-30T12:00:00Z",
                                        author = ReviewAuthor("author", "느긋한차슈"),
                                    ),
                                shop = shop,
                            ),
                        ),
                ),
            onBack = {},
            onOpenShopDetail = {},
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileReviewMissingShopPreview() {
    RamapTheme {
        ProfileReviewShopHeader(
            profileReview =
                ProfileReview(
                    review =
                        Review(
                            id = "review-missing-shop",
                            shopId = "missing-shop",
                            body = "맛있는 라멘이에요",
                            createdAt = "2026-09-30T12:00:00Z",
                            author = ReviewAuthor("author", "느긋한차슈"),
                        ),
                ),
            onOpenShopDetail = {},
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
            onOpenShopDetail = {},
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
            onOpenShopDetail = {},
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
            onOpenShopDetail = {},
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
            onOpenShopDetail = {},
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
            onOpenShopDetail = {},
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
            onOpenShopDetail = {},
            onIntent = {},
        )
    }
}

@Composable
private fun ProfileFollowCounts(
    followerCount: Long?,
    followingCount: Long?,
    modifier: Modifier = Modifier,
) {
    if (followerCount == null || followingCount == null) return
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        HorizontalDivider(color = GrayColor.C100)
        Row(
            modifier = Modifier.padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileFollowCount(
                count = followerCount,
                label = stringResource(Res.string.follow_followers),
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(
                modifier = Modifier.height(36.dp),
                color = GrayColor.C100,
            )
            ProfileFollowCount(
                count = followingCount,
                label = stringResource(Res.string.follow_following),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProfileFollowCount(
    count: Long,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AppText(
            text = count.toString(),
            style = AppTextStyle.T1,
            color = GrayColor.C500,
        )
        AppText(
            text = label,
            style = AppTextStyle.C1,
            color = GrayColor.C300,
        )
    }
}
