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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.my.component.BlockedUsersDialog
import com.peto.ramap.ui.main.my.component.GuestProfileHeader
import com.peto.ramap.ui.main.my.component.MyMenuRow
import com.peto.ramap.ui.main.my.component.MyProfileHeader
import com.peto.ramap.ui.main.my.component.MyTabSkeleton
import com.peto.ramap.ui.main.my.contract.MyTabIntent
import com.peto.ramap.ui.main.my.contract.MyTabSideEffect
import com.peto.ramap.ui.main.my.contract.MyTabUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_notification
import ramap.shared.generated.resources.ic_person
import ramap.shared.generated.resources.ic_profile_bookmark
import ramap.shared.generated.resources.ic_profile_private
import ramap.shared.generated.resources.ic_profile_public
import ramap.shared.generated.resources.ic_profile_report
import ramap.shared.generated.resources.ic_setting
import ramap.shared.generated.resources.ic_visibility_off
import ramap.shared.generated.resources.profile_discard
import ramap.shared.generated.resources.profile_load_failed
import ramap.shared.generated.resources.profile_retry
import ramap.shared.generated.resources.profile_visibility
import ramap.shared.generated.resources.profile_visibility_body
import ramap.shared.generated.resources.profile_visibility_private
import ramap.shared.generated.resources.profile_visibility_public
import ramap.shared.generated.resources.profile_visibility_status
import ramap.shared.generated.resources.review_blocked_users
import ramap.shared.generated.resources.settings_bookmarked_shops_menu
import ramap.shared.generated.resources.settings_hidden_shops_menu
import ramap.shared.generated.resources.settings_report_menu
import ramap.shared.generated.resources.settings_subscribed_shops_menu
import ramap.shared.generated.resources.settings_title

@Composable
fun MyTabRoute(
    onSettingsNavigate: () -> Unit,
    onReportNavigate: () -> Unit,
    onHiddenShopsNavigate: () -> Unit,
    onSubscribedShopsNavigate: () -> Unit,
    onBookmarkedShopsNavigate: () -> Unit,
    onProfileNavigate: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onLoginClick: (LoginType) -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: MyTabViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var isVisibilityDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isBlockedUsersDialogOpen by rememberSaveable { mutableStateOf(false) }
    ObserveAsEvents(viewModel.sideEffect) { effect ->
        when (effect) {
            is MyTabSideEffect.ShowToast -> toastManager.show(effect.data)
            MyTabSideEffect.OpenBlockedUsersDialog -> isBlockedUsersDialogOpen = true
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(MyTabIntent.Refresh)
    }
    MyTabContent(
        state = state,
        isVisibilityDialogOpen = isVisibilityDialogOpen,
        isBlockedUsersDialogOpen = isBlockedUsersDialogOpen,
        onSettingsClick = onSettingsNavigate,
        onVisibilityClick = { if (state.profile != null) isVisibilityDialogOpen = true },
        onVisibilityDismiss = { if (!state.savingVisibility) isVisibilityDialogOpen = false },
        onVisibilitySave = { isPublic ->
            isVisibilityDialogOpen = false
            viewModel.dispatch(MyTabIntent.SaveVisibility(isPublic))
        },
        onBlockedUsersClick = { viewModel.dispatch(MyTabIntent.OpenBlockedUsers) },
        onBlockedUsersDismiss = {
            isBlockedUsersDialogOpen = false
            viewModel.dispatch(MyTabIntent.DismissBlockedUsers)
        },
        onBlockedUserProfileClick = { userId ->
            isBlockedUsersDialogOpen = false
            viewModel.dispatch(MyTabIntent.DismissBlockedUsers)
            onOpenProfile(userId)
        },
        onProfileClick = onProfileNavigate,
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
    onVisibilitySave: (Boolean) -> Unit,
    onBlockedUsersClick: () -> Unit,
    onBlockedUsersDismiss: () -> Unit,
    onBlockedUserProfileClick: (String) -> Unit,
    onProfileClick: () -> Unit,
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
            confirmText =
                stringResource(
                    if (isPublic) Res.string.profile_visibility_private else Res.string.profile_visibility_public,
                ),
            dismissText = stringResource(Res.string.profile_discard),
            confirmEnabled = !state.savingVisibility,
            onDismissRequest = onVisibilityDismiss,
            onConfirm = { onVisibilitySave(!isPublic) },
            onDismiss = onVisibilityDismiss,
            content = {
                AppText(
                    text = stringResource(Res.string.profile_visibility),
                    style = AppTextStyle.T1,
                    color = GrayColor.C500,
                    textAlign = TextAlign.Center,
                )
                AppText(
                    text = stringResource(Res.string.profile_visibility_body),
                    style = AppTextStyle.B2,
                    color = GrayColor.C400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                AppText(
                    text =
                        stringResource(
                            Res.string.profile_visibility_status,
                            stringResource(
                                if (isPublic) Res.string.profile_visibility_public else Res.string.profile_visibility_private,
                            ),
                        ),
                    style = AppTextStyle.B2,
                    color = GrayColor.C400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            },
        )
        BlockedUsersDialog(
            visible = isBlockedUsersDialogOpen,
            users = state.blockedUsers,
            onDismiss = onBlockedUsersDismiss,
            onProfileClick = onBlockedUserProfileClick,
        )
        when {
            isLoading -> MyTabSkeleton()

            isGuest -> GuestProfileHeader(onLoginClick = onLoginClick)

            else ->
                MyProfileHeader(
                    profile = state.profile,
                    failed = state.failed,
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
                        title = Res.string.review_blocked_users,
                        count = null,
                        hasCount = false,
                        iconBackground = GrayColor.C050,
                        iconTint = GrayColor.C300,
                        isLoading = isLoading,
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
                ),
            onSettingsClick = {},
            onVisibilityClick = {},
            onVisibilityDismiss = {},
            onVisibilitySave = {},
            onBlockedUsersClick = {},
            onBlockedUsersDismiss = {},
            onBlockedUserProfileClick = {},
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
