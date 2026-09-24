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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.profile.ProfileAvatar
import com.peto.ramap.designsystem.profile.ProfileDotField
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_chevron_right
import ramap.shared.generated.resources.ic_notification
import ramap.shared.generated.resources.ic_profile_bookmark
import ramap.shared.generated.resources.ic_profile_report
import ramap.shared.generated.resources.ic_setting
import ramap.shared.generated.resources.ic_visibility_off
import ramap.shared.generated.resources.profile_load_failed
import ramap.shared.generated.resources.profile_photo
import ramap.shared.generated.resources.profile_retry
import ramap.shared.generated.resources.profile_title
import ramap.shared.generated.resources.settings_bookmarked_shops_menu
import ramap.shared.generated.resources.settings_hidden_shops_menu
import ramap.shared.generated.resources.settings_report_menu
import ramap.shared.generated.resources.settings_subscribed_shops_menu
import ramap.shared.generated.resources.settings_title

private val Ink = Color(0xFF1E2633)
private val Muted = Color(0xFF84909F)
private val Border = Color(0xFFE8EBF1)

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
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    MyTabContent(
        state = state,
        onSettingsClick = onSettingsNavigate,
        onProfileClick = onProfileNavigate,
        onRetryClick = viewModel::refresh,
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
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .verticalScroll(rememberScrollState()),
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(end = 14.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onSettingsClick) {
                Icon(painterResource(Res.drawable.ic_setting), stringResource(Res.string.settings_title), tint = Ink)
            }
        }
        Column(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onProfileClick),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().height(108.dp), contentAlignment = Alignment.Center) {
                ProfileDotField(Modifier.fillMaxSize())
                ProfileAvatar(state.profile?.avatarUrl, stringResource(Res.string.profile_photo), Modifier.size(86.dp))
            }
            if (state.loading && state.profile == null) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Ink)
            } else {
                Text(state.profile?.nickname ?: stringResource(Res.string.profile_title), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            val bio = state.profile?.bio.orEmpty()
            if (bio.isNotBlank()) {
                Text(bio, Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp), color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        if (state.failed) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.profile_load_failed), color = Muted, fontSize = 12.sp)
                TextButton(onClick = onRetryClick) { Text(stringResource(Res.string.profile_retry)) }
            }
        }
        Spacer(Modifier.height(26.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp)),
        ) {
            MyMenuRow(Res.drawable.ic_profile_bookmark, Res.string.settings_bookmarked_shops_menu, state.bookmarkedCount, Color(0xFFFFF3E9), Color(0xFFE77730), onBookmarkedShopsClick)
            HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 1.dp, color = Border)
            MyMenuRow(Res.drawable.ic_notification, Res.string.settings_subscribed_shops_menu, state.notificationCount, Color(0xFFFFF8E9), Color(0xFFEAA827), onSubscribedShopsClick)
            HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 1.dp, color = Border)
            MyMenuRow(Res.drawable.ic_visibility_off, Res.string.settings_hidden_shops_menu, state.hiddenCount, Color(0xFFF2F4F8), Color(0xFF8491A3), onHiddenShopsClick)
            HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 1.dp, color = Border)
            MyMenuRow(Res.drawable.ic_profile_report, Res.string.settings_report_menu, null, Color(0xFFFFF0F1), Color(0xFFF25871), onReportClick)
        }
        Spacer(Modifier.height(24.dp))
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
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(iconBackground), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(18.dp), tint = iconTint)
        }
        Text(stringResource(title), Modifier.weight(1f).padding(start = 12.dp), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (count != null) {
            Text(count.toString(), Modifier.clip(RoundedCornerShape(8.dp)).background(iconBackground).padding(horizontal = 7.dp, vertical = 3.dp), color = iconTint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Image(painterResource(Res.drawable.ic_chevron_right), null, Modifier.padding(start = 8.dp).size(16.dp))
    }
}
