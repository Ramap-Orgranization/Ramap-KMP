package com.peto.ramap.ui.main.my

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.login.LoginButton
import com.peto.ramap.designsystem.component.Skeleton
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.profile.ProfileAvatar
import com.peto.ramap.designsystem.profile.ProfileDotField
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.auth.supportedLoginTypes
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.my.component.MyTabSkeleton
import com.peto.ramap.ui.main.my.contract.MyTabIntent
import com.peto.ramap.ui.main.my.contract.MyTabSideEffect
import com.peto.ramap.ui.main.my.contract.MyTabUiState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_chevron_right
import ramap.shared.generated.resources.ic_notification
import ramap.shared.generated.resources.ic_person
import ramap.shared.generated.resources.ic_profile_bookmark
import ramap.shared.generated.resources.ic_profile_edit
import ramap.shared.generated.resources.ic_profile_report
import ramap.shared.generated.resources.ic_setting
import ramap.shared.generated.resources.ic_visibility_off
import ramap.shared.generated.resources.login_required_message
import ramap.shared.generated.resources.profile_bio_empty
import ramap.shared.generated.resources.profile_discard
import ramap.shared.generated.resources.profile_edit
import ramap.shared.generated.resources.profile_load_failed
import ramap.shared.generated.resources.profile_photo
import ramap.shared.generated.resources.profile_retry
import ramap.shared.generated.resources.profile_title
import ramap.shared.generated.resources.profile_visibility
import ramap.shared.generated.resources.profile_visibility_body
import ramap.shared.generated.resources.profile_visibility_private
import ramap.shared.generated.resources.profile_visibility_public
import ramap.shared.generated.resources.profile_visibility_status
import ramap.shared.generated.resources.review_blocked_users
import ramap.shared.generated.resources.review_blocked_users_empty
import ramap.shared.generated.resources.review_close
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
    val profileEditDescription = stringResource(Res.string.profile_edit)
    val isLoading = !state.sessionResolved || (state.loading && state.profile == null)
    val isGuest = state.sessionResolved && state.userId == null
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.White)
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
            IconButton(onClick = onVisibilityClick, enabled = state.profile != null) {
                Icon(
                    painter = painterResource(Res.drawable.ic_visibility_off),
                    contentDescription = stringResource(Res.string.profile_visibility),
                    tint = GrayColor.C500,
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(Res.drawable.ic_setting),
                    contentDescription = stringResource(Res.string.settings_title),
                    tint = GrayColor.C500,
                )
            }
        }
        val isPublic = state.profile?.isPublic == true
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

            else -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = profileEditDescription }
                            .noRippleClickable(
                                role = Role.Button,
                                onClick = onProfileClick,
                            ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ProfileDotField(modifier = Modifier.fillMaxSize())
                        Box(modifier = Modifier.size(128.dp)) {
                            ProfileAvatar(
                                model = state.profile?.avatarUrl,
                                description = stringResource(Res.string.profile_photo),
                                modifier = Modifier.size(128.dp),
                            )
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 6.dp, y = 6.dp)
                                        .size(40.dp)
                                        .border(2.dp, Color.White, CircleShape)
                                        .clip(CircleShape)
                                        .background(ChromaticColor.Orange400),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_profile_edit),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                    AppText(
                        text = state.profile?.nickname ?: stringResource(Res.string.profile_title),
                        style = AppTextStyle.T1,
                        color = GrayColor.C500,
                    )
                    val profile = state.profile
                    val bio = profile?.bio.orEmpty()
                    if (bio.isNotBlank()) {
                        AppText(
                            text = bio,
                            style = AppTextStyle.C1,
                            color = GrayColor.C400,
                            modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp),
                            textAlign = TextAlign.Center,
                        )
                    } else if (!state.failed) {
                        AppText(
                            text = stringResource(Res.string.profile_bio_empty),
                            style = AppTextStyle.C1,
                            color = GrayColor.C400,
                            modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
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
                        iconBackground = Color(0xFFF2F4F8),
                        iconTint = Color(0xFF8491A3),
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
                        iconBackground = Color(0xFFFFF3E9),
                        iconTint = Color(0xFFE77730),
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
                        iconBackground = Color(0xFFFFF8E9),
                        iconTint = Color(0xFFEAA827),
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
                        iconBackground = Color(0xFFF2F4F8),
                        iconTint = Color(0xFF8491A3),
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
                    iconBackground = Color(0xFFFFF0F1),
                    iconTint = Color(0xFFF25871),
                    hasCount = false,
                    height = if (isGuest) 59.dp else 52.dp,
                    onClick = onReportClick,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BlockedUsersDialog(
    visible: Boolean,
    users: List<PublicProfile>,
    onDismiss: () -> Unit,
    onProfileClick: (String) -> Unit,
) {
    if (!visible) return
    CommonDialog(
        visible = visible,
        confirmText = stringResource(Res.string.review_close),
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        content = {
            AppText(
                text = stringResource(Res.string.review_blocked_users),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (users.isEmpty()) {
                AppText(
                    text = stringResource(Res.string.review_blocked_users_empty),
                    style = AppTextStyle.B2,
                    color = GrayColor.C400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                ) {
                    items(users, key = { it.userId }) { profile ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onProfileClick(profile.userId) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppText(
                                text = profile.nickname,
                                style = AppTextStyle.B1,
                                color = GrayColor.C500,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun GuestProfileHeader(
    onLoginClick: (LoginType) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        ProfileDotField(modifier = Modifier.fillMaxSize())
        ProfileAvatar(
            model = null,
            description = stringResource(Res.string.profile_photo),
            modifier = Modifier.size(128.dp),
        )
    }
    AppText(
        text = stringResource(Res.string.login_required_message),
        style = AppTextStyle.H3Brand,
        color = GrayColor.C400,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Column(
        modifier = Modifier.padding(top = 15.dp, start = 22.dp, end = 22.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        supportedLoginTypes().forEach { type ->
            LoginButton(
                type = type,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onLoginClick(type) },
            )
        }
    }
}

@Composable
private fun MyMenuRow(
    icon: DrawableResource,
    title: StringResource,
    count: Int?,
    iconBackground: Color,
    iconTint: Color,
    hasCount: Boolean = true,
    isLoading: Boolean = false,
    height: Dp = 52.dp,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = iconTint,
            )
        }
        AppText(
            text = stringResource(title),
            style = AppTextStyle.B1,
            color = GrayColor.C500,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
        )
        if (hasCount && isLoading && count == null) {
            Skeleton(
                modifier = Modifier.size(width = 24.dp, height = 18.dp),
                shape = RoundedCornerShape(8.dp),
            )
        } else if (count != null) {
            AppText(
                text = count.toString(),
                style = AppTextStyle.C2,
                color = iconTint,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBackground)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
        Image(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = null,
            modifier =
                Modifier
                    .padding(start = 8.dp)
                    .size(16.dp),
        )
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
