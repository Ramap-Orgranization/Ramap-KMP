package com.peto.ramap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.appnotice.AppNotice
import com.peto.ramap.domain.repository.AppNoticeRepository
import com.peto.ramap.platform.storage.AppNoticeStorage
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.app_notice_close
import ramap.shared.generated.resources.app_notice_do_not_show_again
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun AppNoticeGate(
    appNoticeRepository: AppNoticeRepository,
    appNoticeStorage: AppNoticeStorage,
    platform: String,
    content: @Composable () -> Unit,
) {
    var appNotice by remember { mutableStateOf<AppNotice?>(null) }
    var hasDismissedNotice by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(appNoticeRepository, appNoticeStorage, platform, hasDismissedNotice) {
        if (!hasDismissedNotice) {
            appNotice = fetchVisibleAppNotice(appNoticeRepository, appNoticeStorage, platform)
        }
    }

    content()

    val visibleNotice = appNotice?.takeUnless { hasDismissedNotice }
    if (visibleNotice != null) {
        CommonDialog(
            visible = true,
            confirmText = stringResource(Res.string.app_notice_close),
            dismissText = stringResource(Res.string.app_notice_do_not_show_again),
            onDismissRequest = { hasDismissedNotice = true },
            content = {
                AppText(
                    text = visibleNotice.title,
                    style = AppTextStyle.T1,
                    color = GrayColor.C500,
                    textAlign = TextAlign.Center,
                )
                AppText(
                    text = visibleNotice.message,
                    style = AppTextStyle.B2,
                    color = GrayColor.C400,
                    textAlign = TextAlign.Center,
                )
            },
            onConfirm = { hasDismissedNotice = true },
            onDismiss = {
                hasDismissedNotice = true
                coroutineScope.launch { hideAppNotice(appNoticeStorage, visibleNotice.id) }
            },
        )
    }
}

internal suspend fun fetchVisibleAppNotice(
    repository: AppNoticeRepository,
    storage: AppNoticeStorage,
    platform: String,
): AppNotice? =
    withTimeoutOrNull(APP_NOTICE_TIMEOUT_MS.milliseconds) {
        try {
            val appNotice =
                when (val result = repository.fetchActiveAppNotice(platform)) {
                    is RamapResult.Success -> result.data
                    is RamapResult.Error -> null
                }
            if (appNotice == null || isAppNoticeHidden(appNotice, storage.fetchHiddenNoticeId())) {
                null
            } else {
                appNotice
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            null
        }
    }

internal fun isAppNoticeHidden(
    appNotice: AppNotice,
    hiddenNoticeId: String?,
): Boolean = appNotice.id == hiddenNoticeId

private suspend fun hideAppNotice(
    storage: AppNoticeStorage,
    noticeId: String,
) {
    try {
        storage.hideNotice(noticeId)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        // 저장에 실패해도 현재 실행 중에는 공지를 다시 표시하지 않는다.
    }
}

private const val APP_NOTICE_TIMEOUT_MS = 5_000L
