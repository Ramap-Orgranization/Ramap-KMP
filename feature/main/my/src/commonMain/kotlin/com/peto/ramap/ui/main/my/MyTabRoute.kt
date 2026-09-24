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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.profile.ProfileAvatar
import com.peto.ramap.designsystem.profile.ProfileDotField
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.MyTabColor
import com.peto.ramap.theme.ProfileColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.my.contract.MyTabIntent
import com.peto.ramap.ui.main.my.contract.MyTabUiState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_chevron_right
import ramap.shared.generated.resources.ic_notification
import ramap.shared.generated.resources.ic_profile_bookmark
import ramap.shared.generated.resources.ic_profile_edit
import ramap.shared.generated.resources.ic_profile_report
import ramap.shared.generated.resources.ic_setting
import ramap.shared.generated.resources.ic_visibility_off
import ramap.shared.generated.resources.profile_bio_empty
import ramap.shared.generated.resources.profile_edit
import ramap.shared.generated.resources.profile_instagram_empty
import ramap.shared.generated.resources.profile_instagram_handle
import ramap.shared.generated.resources.profile_load_failed
import ramap.shared.generated.resources.profile_photo
import ramap.shared.generated.resources.profile_retry
import ramap.shared.generated.resources.profile_title
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
    viewModel: MyTabViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.dispatch(MyTabIntent.Refresh) }
    MyTabContent(
        state = state,
        onSettingsClick = onSettingsNavigate,
        onProfileClick = onProfileNavigate,
        onRetryClick = { viewModel.dispatch(MyTabIntent.Refresh) },
        onBookmarkedShopsClick = onBookmarkedShopsNavigate,
        onSubscribedShopsClick = onSubscribedShopsNavigate,
        onHiddenShopsClick = onHiddenShopsNavigate,
        onReportClick = onReportNavigate,
    )
}

@Composable
internal fun MyTabContent(
    state: MyTabUiState,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onRetryClick: () -> Unit,
    onBookmarkedShopsClick: () -> Unit,
    onSubscribedShopsClick: () -> Unit,
    onHiddenShopsClick: () -> Unit,
    onReportClick: () -> Unit,
) {
    val profileEditDescription = stringResource(Res.string.profile_edit)
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
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(Res.drawable.ic_setting),
                    contentDescription = stringResource(Res.string.settings_title),
                    tint = MyTabColor.Ink,
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = profileEditDescription }
                    .noRippleClickable(role = Role.Button, onClick = onProfileClick),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!state.sessionResolved || (state.loading && state.profile == null)) {
                RamenLoadingIndicator(modifier = Modifier.height(120.dp))
            } else {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(108.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ProfileDotField(modifier = Modifier.fillMaxSize())
                    Box(modifier = Modifier.size(86.dp)) {
                        ProfileAvatar(
                            model = state.profile?.avatarUrl,
                            description = stringResource(Res.string.profile_photo),
                            modifier = Modifier.size(86.dp),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 4.dp, y = 4.dp)
                                    .size(30.dp)
                                    .border(2.dp, Color.White, CircleShape)
                                    .clip(CircleShape)
                                    .background(ProfileColor.Orange),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_profile_edit),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White,
                            )
                        }
                    }
                }
                AppText(
                    text = state.profile?.nickname ?: stringResource(Res.string.profile_title),
                    style = AppTextStyle.T1,
                    color = MyTabColor.Ink,
                )
            }
            val profile = state.profile
            val bio = profile?.bio.orEmpty()
            if (bio.isNotBlank()) {
                AppText(
                    text = bio,
                    style = AppTextStyle.C1,
                    color = MyTabColor.Muted,
                    modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp),
                    textAlign = TextAlign.Center,
                )
            } else if (profile != null && !state.failed) {
                AppText(
                    text = stringResource(Res.string.profile_bio_empty),
                    style = AppTextStyle.C1,
                    color = MyTabColor.Muted,
                    modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp),
                    textAlign = TextAlign.Center,
                )
            }
            val instagramUsername = profile?.instagramUsername.orEmpty()
            if (instagramUsername.isNotBlank()) {
                AppText(
                    text = stringResource(Res.string.profile_instagram_handle, instagramUsername),
                    style = AppTextStyle.C1,
                    color = MyTabColor.Muted,
                    modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp),
                    textAlign = TextAlign.Center,
                )
            } else if (profile != null && !state.failed) {
                AppText(
                    text = stringResource(Res.string.profile_instagram_empty),
                    style = AppTextStyle.C1,
                    color = MyTabColor.Muted,
                    modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (state.failed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = stringResource(Res.string.profile_load_failed),
                    style = AppTextStyle.C1,
                    color = MyTabColor.Muted,
                )
                TextButton(onClick = onRetryClick) {
                    AppText(
                        text = stringResource(Res.string.profile_retry),
                        style = AppTextStyle.B4,
                        color = MyTabColor.Ink,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(26.dp))
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .border(1.dp, MyTabColor.Border, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)),
        ) {
            if (state.userId != null) {
                MyMenuRow(
                    icon = Res.drawable.ic_profile_bookmark,
                    title = Res.string.settings_bookmarked_shops_menu,
                    count = state.bookmarkedCount,
                    iconBackground = Color(0xFFFFF3E9),
                    iconTint = Color(0xFFE77730),
                    onClick = onBookmarkedShopsClick,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = 1.dp,
                    color = MyTabColor.Border,
                )
                MyMenuRow(
                    icon = Res.drawable.ic_notification,
                    title = Res.string.settings_subscribed_shops_menu,
                    count = state.notificationCount,
                    iconBackground = Color(0xFFFFF8E9),
                    iconTint = Color(0xFFEAA827),
                    onClick = onSubscribedShopsClick,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = 1.dp,
                    color = MyTabColor.Border,
                )
                MyMenuRow(
                    icon = Res.drawable.ic_visibility_off,
                    title = Res.string.settings_hidden_shops_menu,
                    count = state.hiddenCount,
                    iconBackground = Color(0xFFF2F4F8),
                    iconTint = Color(0xFF8491A3),
                    onClick = onHiddenShopsClick,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = 1.dp,
                    color = MyTabColor.Border,
                )
            }
            MyMenuRow(
                icon = Res.drawable.ic_profile_report,
                title = Res.string.settings_report_menu,
                count = null,
                iconBackground = Color(0xFFFFF0F1),
                iconTint = Color(0xFFF25871),
                onClick = onReportClick,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MyMenuRow(
    icon: DrawableResource,
    title: StringResource,
    count: Int?,
    iconBackground: Color,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
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
            color = MyTabColor.Ink,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
        )
        if (count != null) {
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
                            instagramUsername = "",
                        ),
                    failed = false,
                    bookmarkedCount = 12,
                    notificationCount = 3,
                    hiddenCount = 0,
                ),
            onSettingsClick = {},
            onProfileClick = {},
            onRetryClick = {},
            onBookmarkedShopsClick = {},
            onSubscribedShopsClick = {},
            onHiddenShopsClick = {},
            onReportClick = {},
        )
    }
}
