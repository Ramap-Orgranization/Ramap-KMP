package com.peto.ramap.ui.main.my

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.profile.ProfileHeader
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileVisibility
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.my.component.BlockedUsersDialog
import com.peto.ramap.ui.main.my.component.GuestProfileHeader
import com.peto.ramap.ui.main.my.component.MyMenuRow
import com.peto.ramap.ui.main.my.component.MyTabSkeleton
import com.peto.ramap.ui.main.my.contract.MyTabIntent
import com.peto.ramap.ui.main.my.contract.MyTabSideEffect
import com.peto.ramap.ui.main.my.contract.MyTabUiState
import com.peto.ramap.ui.refresh.RefreshOnReturnEffect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.follow_manage
import ramap.shared.generated.resources.follow_requests
import ramap.shared.generated.resources.follow_reviews_visibility
import ramap.shared.generated.resources.follow_saved_shops_visibility
import ramap.shared.generated.resources.ic_notification
import ramap.shared.generated.resources.ic_person
import ramap.shared.generated.resources.ic_profile_blocked
import ramap.shared.generated.resources.ic_profile_bookmark
import ramap.shared.generated.resources.ic_profile_private
import ramap.shared.generated.resources.ic_profile_public
import ramap.shared.generated.resources.ic_profile_report
import ramap.shared.generated.resources.ic_review
import ramap.shared.generated.resources.ic_setting
import ramap.shared.generated.resources.ic_visibility_off
import ramap.shared.generated.resources.my_reviews
import ramap.shared.generated.resources.profile_discard
import ramap.shared.generated.resources.profile_load_failed
import ramap.shared.generated.resources.profile_retry
import ramap.shared.generated.resources.profile_visibility
import ramap.shared.generated.resources.profile_visibility_public
import ramap.shared.generated.resources.review_blocked_users
import ramap.shared.generated.resources.review_save
import ramap.shared.generated.resources.review_unblock
import ramap.shared.generated.resources.review_unblock_confirm
import ramap.shared.generated.resources.settings_bookmarked_shops_menu
import ramap.shared.generated.resources.settings_hidden_shops_menu
import ramap.shared.generated.resources.settings_report_menu
import ramap.shared.generated.resources.settings_subscribed_shops_menu
import ramap.shared.generated.resources.settings_title
import ramap.shared.generated.resources.shop_review_cancel

@Composable
fun MyTabRoute(
    onSettingsNavigate: () -> Unit,
    onReportNavigate: () -> Unit,
    onHiddenShopsNavigate: () -> Unit,
    onSubscribedShopsNavigate: () -> Unit,
    onBookmarkedShopsNavigate: () -> Unit,
    onProfileNavigate: () -> Unit,
    onFollowNavigate: () -> Unit = {},
    onMyReviewsNavigate: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
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
        onBookmarkedShopsClick = onBookmarkedShopsNavigate,
        onSubscribedShopsClick = onSubscribedShopsNavigate,
        onHiddenShopsClick = onHiddenShopsNavigate,
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
    onBookmarkedShopsClick: () -> Unit,
    onSubscribedShopsClick: () -> Unit,
    onHiddenShopsClick: () -> Unit,
    onReportClick: () -> Unit,
    onLoginClick: (LoginType) -> Unit,
) {
    val isLoading = !state.sessionResolved || (state.loading && state.profile == null)
    val isGuest = state.sessionResolved && state.userId == null
    val isPublic = state.profile?.isPublic == true
    var draftPublic by remember(isVisibilityDialogOpen, state.profile) { mutableStateOf(isPublic) }
    var draftReviews by remember(isVisibilityDialogOpen, state.profile) {
        mutableStateOf(state.profile?.followersCanReadReviews ?: true)
    }
    var draftSavedShops by remember(isVisibilityDialogOpen, state.profile) {
        mutableStateOf(state.profile?.followersCanReadSavedShops ?: true)
    }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CommonColor.White)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(end = 14.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            if (state.userId != null) {
                IconButton(onClick = onVisibilityClick, enabled = state.profile != null) {
                    Image(
                        painter =
                            painterResource(
                                if (isPublic) Res.drawable.ic_profile_public else Res.drawable.ic_profile_private,
                            ),
                        contentDescription = stringResource(Res.string.profile_visibility),
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(Res.drawable.ic_setting),
                    contentDescription = stringResource(Res.string.settings_title),
                    tint = GrayColor.C500,
                )
            }
        }
        CommonDialog(
            visible = isVisibilityDialogOpen,
            confirmText = stringResource(Res.string.review_save),
            dismissText = stringResource(Res.string.profile_discard),
            confirmEnabled = !state.savingVisibility,
            onDismissRequest = onVisibilityDismiss,
            onConfirm = {
                onVisibilitySave(ProfileVisibility(draftPublic, draftReviews, draftSavedShops))
            },
            onDismiss = onVisibilityDismiss,
            content = {
                AppText(
                    text = stringResource(Res.string.profile_visibility),
                    style = AppTextStyle.T1,
                    color = GrayColor.C500,
                    textAlign = TextAlign.Center,
                )
                VisibilitySwitch(
                    label = stringResource(Res.string.profile_visibility_public),
                    checked = draftPublic,
                    enabled = !state.savingVisibility,
                    onCheckedChange = { draftPublic = it },
                )
                VisibilitySwitch(
                    label = stringResource(Res.string.follow_reviews_visibility),
                    checked = draftReviews,
                    enabled = !state.savingVisibility,
                    onCheckedChange = { draftReviews = it },
                )
                VisibilitySwitch(
                    label = stringResource(Res.string.follow_saved_shops_visibility),
                    checked = draftSavedShops,
                    enabled = !state.savingVisibility,
                    onCheckedChange = { draftSavedShops = it },
                )
            },
        )
        BlockedUsersDialog(
            visible = isBlockedUsersDialogOpen && state.pendingUnblockUser == null,
            users = state.blockedUsers,
            onDismiss = onBlockedUsersDismiss,
            onProfileClick = onBlockedUserProfileClick,
            onUnblockClick = onUnblockClick,
        )
        CommonDialog(
            visible = isBlockedUsersDialogOpen && state.pendingUnblockUser != null,
            confirmText = stringResource(Res.string.review_unblock),
            dismissText = stringResource(Res.string.shop_review_cancel),
            confirmEnabled = !state.unblocking,
            confirmIsLoading = state.unblocking,
            dismissEnabled = !state.unblocking,
            dismissOnBackPress = !state.unblocking,
            dismissOnClickOutside = !state.unblocking,
            onDismissRequest = onUnblockDismiss,
            onDismiss = onUnblockDismiss,
            onConfirm = onUnblockConfirm,
        ) {
            AppText(
                text =
                    stringResource(
                        Res.string.review_unblock_confirm,
                        state.pendingUnblockUser?.nickname.orEmpty(),
                    ),
                style = AppTextStyle.B2,
                color = GrayColor.C500,
            )
        }
        when {
            isLoading -> MyTabSkeleton()

            isGuest -> GuestProfileHeader(onLoginClick = onLoginClick)

            else ->
                ProfileHeader(
                    nickname = state.profile?.nickname,
                    avatarUrl = state.profile?.avatarUrl,
                    bio = state.profile?.bio,
                    showEmptyBio = (state.profile != null) && !state.failed,
                    onProfileClick = onProfileClick,
                )
        }
        if (state.failed && !isGuest) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = stringResource(Res.string.profile_load_failed),
                    style = AppTextStyle.C1,
                    color = GrayColor.C400,
                )
                TextButton(onClick = onRetryClick) {
                    AppText(
                        text = stringResource(Res.string.profile_retry),
                        style = AppTextStyle.B4,
                        color = GrayColor.C500,
                    )
                }
            }
        }
        if (state.sessionResolved) {
            Spacer(modifier = Modifier.height(if (isGuest) 40.dp else 26.dp))
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isGuest) 22.dp else 12.dp)
                        .border(1.dp, GrayColor.C100, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp)),
            ) {
                if (!isGuest) {
                    MyMenuRow(
                        icon = Res.drawable.ic_person,
                        title = Res.string.follow_manage,
                        count = null,
                        iconBackground = ChromaticColor.Orange050,
                        iconTint = ChromaticColor.Orange400,
                        hasCount = false,
                        notification = if (state.hasPendingFollowRequests) Res.string.follow_requests else null,
                        onClick = onFollowClick,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 1.dp,
                        color = GrayColor.C100,
                    )
                    MyMenuRow(
                        icon = Res.drawable.ic_review,
                        title = Res.string.my_reviews,
                        count = state.reviewCount,
                        iconBackground = ChromaticColor.Purple050,
                        iconTint = ChromaticColor.Purple300,
                        isLoading = state.loadingReviewCount,
                        onClick = onMyReviewsClick,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 1.dp,
                        color = GrayColor.C100,
                    )
                    MyMenuRow(
                        icon = Res.drawable.ic_profile_blocked,
                        title = Res.string.review_blocked_users,
                        count = state.blockedUserCount,
                        iconBackground = GrayColor.C050,
                        iconTint = GrayColor.C300,
                        isLoading = state.loadingBlockedUserCount,
                        onClick = onBlockedUsersClick,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 1.dp,
                        color = GrayColor.C100,
                    )
                    MyMenuRow(
                        icon = Res.drawable.ic_profile_bookmark,
                        title = Res.string.settings_bookmarked_shops_menu,
                        count = state.bookmarkedCount,
                        iconBackground = ChromaticColor.Orange050,
                        iconTint = ChromaticColor.Orange300,
                        isLoading = isLoading,
                        onClick = onBookmarkedShopsClick,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 1.dp,
                        color = GrayColor.C100,
                    )
                    MyMenuRow(
                        icon = Res.drawable.ic_notification,
                        title = Res.string.settings_subscribed_shops_menu,
                        count = state.notificationCount,
                        iconBackground = ChromaticColor.Yellow050,
                        iconTint = ChromaticColor.Yellow300,
                        isLoading = isLoading,
                        onClick = onSubscribedShopsClick,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 1.dp,
                        color = GrayColor.C100,
                    )
                    MyMenuRow(
                        icon = Res.drawable.ic_visibility_off,
                        title = Res.string.settings_hidden_shops_menu,
                        count = state.hiddenCount,
                        iconBackground = GrayColor.C050,
                        iconTint = GrayColor.C300,
                        isLoading = isLoading,
                        onClick = onHiddenShopsClick,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 1.dp,
                        color = GrayColor.C100,
                    )
                }
                MyMenuRow(
                    icon = Res.drawable.ic_profile_report,
                    title = Res.string.settings_report_menu,
                    count = null,
                    iconBackground = ChromaticColor.Red050,
                    iconTint = ChromaticColor.Red300,
                    hasCount = false,
                    onClick = onReportClick,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview
@Composable
private fun MyTabRoutePreview() {
    RamapTheme {
        MyTabContent(
            state =
                MyTabUiState(
                    userId = "preview",
                    sessionResolved = true,
                    profile =
                        AccountProfile(
                            userId = "preview",
                            nickname = "느긋한차슈",
                            bio = "",
                        ),
                    failed = false,
                    bookmarkedCount = 12,
                    notificationCount = 3,
                    hiddenCount = 0,
                    reviewCount = 8,
                    blockedUserCount = 2,
                    hasPendingFollowRequests = true,
                ),
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
            onBookmarkedShopsClick = {},
            onSubscribedShopsClick = {},
            onHiddenShopsClick = {},
            onReportClick = {},
            onLoginClick = {},
        )
    }
}

@Composable
private fun VisibilitySwitch(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppText(text = label, style = AppTextStyle.B2, color = GrayColor.C500, modifier = Modifier.weight(1f))
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}
