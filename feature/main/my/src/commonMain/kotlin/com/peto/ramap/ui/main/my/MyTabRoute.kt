package com.peto.ramap.ui.main.my

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.profile.ProfileHeader
import com.peto.ramap.designsystem.profile.ProfileVisitRankingCard
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileVisibility
import com.peto.ramap.domain.model.profile.ProfileVisitRanking
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SurfaceColor
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.main.my.component.BlockedUsersDialog
import com.peto.ramap.ui.main.my.component.GuestProfileHeader
import com.peto.ramap.ui.main.my.component.MyTabMenuList
import com.peto.ramap.ui.main.my.component.MyTabSkeleton
import com.peto.ramap.ui.main.my.component.MyTabTopBar
import com.peto.ramap.ui.main.my.component.ProfileLoadFailedBanner
import com.peto.ramap.ui.main.my.component.ProfileVisibilityDialog
import com.peto.ramap.ui.main.my.component.UnblockConfirmDialog
import com.peto.ramap.ui.main.my.contract.MyTabIntent
import com.peto.ramap.ui.main.my.contract.MyTabLoadKey
import com.peto.ramap.ui.main.my.contract.MyTabSideEffect
import com.peto.ramap.ui.main.my.contract.MyTabUiState
import com.peto.ramap.ui.refresh.RefreshOnReturnEffect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyTabRoute(
    onSettingsNavigate: () -> Unit,
    onReportNavigate: () -> Unit,
    onSubscribedShopsNavigate: () -> Unit,
    onBookmarkedShopsNavigate: () -> Unit,
    onProfileNavigate: () -> Unit,
    onAnnouncementsNavigate: () -> Unit,
    onFollowNavigate: () -> Unit = {},
    onMyReviewsNavigate: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onOpenShopDetail: (String) -> Unit,
    onLoginClick: (LoginType) -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: MyTabViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var returningFromProfileEdit by rememberSaveable { mutableStateOf(false) }
    var returningFromReviews by rememberSaveable { mutableStateOf(false) }
    var returningFromBlockedProfile by rememberSaveable { mutableStateOf(false) }
    var returningFromFollows by rememberSaveable { mutableStateOf(false) }
    RefreshOnReturnEffect {
        viewModel.dispatch(MyTabIntent.RefreshAnnouncements)
        if (returningFromProfileEdit) {
            returningFromProfileEdit = false
            viewModel.dispatch(MyTabIntent.ReturnedFromProfileEdit)
        } else if (returningFromReviews) {
            returningFromReviews = false
            viewModel.dispatch(MyTabIntent.ReturnedFromReviews)
        } else if (returningFromBlockedProfile) {
            returningFromBlockedProfile = false
            viewModel.dispatch(MyTabIntent.ReturnedFromBlockedProfile)
        } else if (returningFromFollows) {
            returningFromFollows = false
            viewModel.dispatch(MyTabIntent.ReturnedFromFollows)
        } else {
            viewModel.dispatch(MyTabIntent.ReturnedToScreen)
        }
    }
    var isVisibilityDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isBlockedUsersDialogOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.userId) {
        isVisibilityDialogOpen = false
        isBlockedUsersDialogOpen = false
    }
    ObserveAsEvents(viewModel.sideEffect) { effect ->
        when (effect) {
            is MyTabSideEffect.ShowToast -> toastManager.show(effect.data)
            MyTabSideEffect.VisibilitySaved -> isVisibilityDialogOpen = false
            MyTabSideEffect.OpenBlockedUsersDialog -> isBlockedUsersDialogOpen = true
            MyTabSideEffect.CloseBlockedUsersDialog -> isBlockedUsersDialogOpen = false
        }
    }
    MyTabContent(
        state = state,
        isVisibilityDialogOpen = isVisibilityDialogOpen,
        isBlockedUsersDialogOpen = isBlockedUsersDialogOpen,
        onSettingsClick = onSettingsNavigate,
        onVisibilityClick = { if (state.profile != null) isVisibilityDialogOpen = true },
        onVisibilityDismiss = { if (!state.savingVisibility) isVisibilityDialogOpen = false },
        onVisibilitySave = { visibility ->
            viewModel.dispatch(MyTabIntent.SaveProfileVisibility(visibility))
        },
        onAnnouncementsClick = onAnnouncementsNavigate,
        onFollowClick = {
            returningFromFollows = true
            onFollowNavigate()
        },
        onBlockedUsersClick = { viewModel.dispatch(MyTabIntent.OpenBlockedUsers) },
        onBlockedUsersDismiss = {
            isBlockedUsersDialogOpen = false
            viewModel.dispatch(MyTabIntent.DismissBlockedUsers)
        },
        onBlockedUserProfileClick = { userId ->
            isBlockedUsersDialogOpen = false
            viewModel.dispatch(MyTabIntent.DismissBlockedUsers)
            returningFromBlockedProfile = true
            onOpenProfile(userId)
        },
        onUnblockClick = { userId -> viewModel.dispatch(MyTabIntent.RequestUnblock(userId)) },
        onUnblockDismiss = { viewModel.dispatch(MyTabIntent.DismissUnblock) },
        onUnblockConfirm = { viewModel.dispatch(MyTabIntent.ConfirmUnblock) },
        onProfileClick = {
            returningFromProfileEdit = true
            onProfileNavigate()
        },
        onMyReviewsClick = {
            returningFromReviews = true
            onMyReviewsNavigate()
        },
        onRetryClick = { viewModel.dispatch(MyTabIntent.Refresh) },
        onOpenShopDetail = onOpenShopDetail,
        onBookmarkedShopsClick = onBookmarkedShopsNavigate,
        onSubscribedShopsClick = onSubscribedShopsNavigate,
        onReportClick = onReportNavigate,
        onLoginClick = onLoginClick,
    )
}

@Composable
internal fun MyTabContent(
    state: MyTabUiState,
    isVisibilityDialogOpen: Boolean = false,
    isBlockedUsersDialogOpen: Boolean = false,
    onSettingsClick: () -> Unit,
    onVisibilityClick: () -> Unit,
    onVisibilityDismiss: () -> Unit,
    onVisibilitySave: (ProfileVisibility) -> Unit,
    onAnnouncementsClick: () -> Unit = {},
    onFollowClick: () -> Unit = {},
    onBlockedUsersClick: () -> Unit,
    onBlockedUsersDismiss: () -> Unit,
    onBlockedUserProfileClick: (String) -> Unit,
    onUnblockClick: (String) -> Unit,
    onUnblockDismiss: () -> Unit,
    onUnblockConfirm: () -> Unit,
    onProfileClick: () -> Unit,
    onMyReviewsClick: () -> Unit = {},
    onRetryClick: () -> Unit,
    onOpenShopDetail: (String) -> Unit,
    onBookmarkedShopsClick: () -> Unit,
    onSubscribedShopsClick: () -> Unit,
    onReportClick: () -> Unit,
    onLoginClick: (LoginType) -> Unit,
) {
    val isLoading = !state.sessionResolved || (state.loading && state.profile == null)
    val isGuest = state.sessionResolved && state.userId == null
    val isPublic = state.profile?.isPublic == true

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(SurfaceColor.Background)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .verticalScroll(rememberScrollState()),
    ) {
        MyTabTopBar(
            isLoggedIn = state.userId != null,
            isPublic = isPublic,
            isProfileLoaded = state.profile != null,
            onVisibilityClick = onVisibilityClick,
            onSettingsClick = onSettingsClick,
        )
        ProfileVisibilityDialog(
            visible = isVisibilityDialogOpen,
            profile = state.profile,
            savingVisibility = state.savingVisibility,
            onDismiss = onVisibilityDismiss,
            onSave = onVisibilitySave,
        )
        BlockedUsersDialog(
            visible = isBlockedUsersDialogOpen && state.pendingUnblockUser == null,
            users = state.blockedUsers,
            onDismiss = onBlockedUsersDismiss,
            onProfileClick = onBlockedUserProfileClick,
            onUnblockClick = onUnblockClick,
        )
        UnblockConfirmDialog(
            visible = isBlockedUsersDialogOpen && state.pendingUnblockUser != null,
            pendingUnblockUser = state.pendingUnblockUser,
            unblocking = state.unblocking,
            onDismiss = onUnblockDismiss,
            onConfirm = onUnblockConfirm,
        )
        when {
            isLoading -> MyTabSkeleton()

            isGuest -> GuestProfileHeader(onLoginClick = onLoginClick)

            state.failed -> ProfileLoadFailedBanner(onRetryClick = onRetryClick)

            else ->
                ProfileHeader(
                    nickname = state.profile?.nickname,
                    avatarUrl = state.profile?.avatarUrl,
                    bio = state.profile?.bio,
                    showEmptyBio = state.profile != null,
                    onProfileClick = onProfileClick,
                )
        }
        val showsVisitRanking = !state.failed && state.profile != null && state.visitRanking?.hasPodium == true
        if (showsVisitRanking) {
            key(state.userId) {
                ProfileVisitRankingCard(
                    ranking = state.visitRanking,
                    onShopClick = onOpenShopDetail,
                    modifier = Modifier.padding(start = 12.dp, top = 16.dp, end = 12.dp),
                )
            }
        }
        if (state.sessionResolved) {
            val menuTopPadding =
                when {
                    isGuest -> 40.dp
                    showsVisitRanking -> 12.dp
                    else -> 26.dp
                }
            Spacer(modifier = Modifier.height(menuTopPadding))
            MyTabMenuList(
                isGuest = isGuest,
                hasUnreadAnnouncements = state.hasUnreadAnnouncements,
                hasPendingFollowRequests = state.hasPendingFollowRequests,
                hasUnreadFollowConnections = state.hasUnreadFollowConnections,
                reviewCount = state.reviewCount,
                loadingReviewCount = state.loadingReviewCount,
                blockedUserCount = state.blockedUserCount,
                loadingBlockedUserCount = state.loadingBlockedUserCount,
                bookmarkedCount = state.bookmarkedCount,
                notificationCount = state.notificationCount,
                isLoading = isLoading,
                onAnnouncementsClick = onAnnouncementsClick,
                onFollowClick = onFollowClick,
                onMyReviewsClick = onMyReviewsClick,
                onBlockedUsersClick = onBlockedUsersClick,
                onBookmarkedShopsClick = onBookmarkedShopsClick,
                onSubscribedShopsClick = onSubscribedShopsClick,
                onReportClick = onReportClick,
            )
        }
    }
}

@Preview
@Composable
internal fun MyProfileLoadingPreview() =
    MyTabPreview(
        state =
            MyTabUiState(
                userId = "preview",
                sessionResolved = true,
                loadState = LoadState.loading(MyTabLoadKey.Fetch),
            ),
    )

@Preview(name = "조회 실패와 다시 시도", group = "내 프로필 조회", widthDp = 360, heightDp = 780)
@Composable
internal fun MyProfileLoadFailedPreview() =
    MyTabPreview(
        state = MyTabUiState(userId = "preview", sessionResolved = true, failed = true),
    )

@Preview(name = "조회 성공 · 순위 없음", group = "내 프로필 조회", widthDp = 360, heightDp = 780)
@Composable
internal fun MyProfileWithoutRankingPreview() =
    MyTabPreview(
        state =
            MyTabUiState(
                userId = "preview",
                sessionResolved = true,
                profile = AccountProfile(userId = "preview", nickname = "느긋한차슈", bio = "라멘을 좋아해요"),
                visitRanking = ProfileVisitRanking(emptyList()),
                bookmarkedCount = 12,
                notificationCount = 3,
                reviewCount = 0,
                blockedUserCount = 2,
                hasPendingFollowRequests = true,
                hasUnreadFollowConnections = true,
            ),
    )

@Composable
private fun MyTabPreview(state: MyTabUiState) {
    RamapTheme {
        MyTabContent(
            state = state,
            onSettingsClick = {},
            onVisibilityClick = {},
            onVisibilityDismiss = {},
            onVisibilitySave = {},
            onBlockedUsersClick = {},
            onBlockedUsersDismiss = {},
            onBlockedUserProfileClick = {},
            onUnblockClick = {},
            onUnblockDismiss = {},
            onUnblockConfirm = {},
            onProfileClick = {},
            onRetryClick = {},
            onOpenShopDetail = {},
            onBookmarkedShopsClick = {},
            onSubscribedShopsClick = {},
            onReportClick = {},
            onLoginClick = {},
        )
    }
}
