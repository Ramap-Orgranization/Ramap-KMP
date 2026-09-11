package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.data.model.AdminDelayedOpening
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor

@Composable
internal fun AdminDelayedOpeningManager(
    notices: List<AdminDelayedOpening>,
    koreaToday: String?,
    releasingId: String?,
    onRefresh: () -> Unit,
    onRelease: (String) -> Unit,
) {
    AppButton(stringResource(R.string.admin_event_status_refresh), onRefresh, modifier = Modifier.fillMaxWidth())
    if (notices.isEmpty()) {
        AppText(stringResource(R.string.admin_delayed_opening_empty), style = AppTextStyle.B2, color = GrayColor.C500)
        return
    }
    val (today, scheduled) = partitionByKoreaToday(notices, koreaToday)
    Column {
        DelayedOpeningSection(
            title = stringResource(R.string.admin_delayed_opening_today),
            emptyMessage = stringResource(R.string.admin_delayed_opening_today_empty),
            notices = today,
            releasingId = releasingId,
            onRelease = onRelease,
        )
        DelayedOpeningSection(
            title = stringResource(R.string.admin_delayed_opening_scheduled),
            emptyMessage = stringResource(R.string.admin_delayed_opening_scheduled_empty),
            notices = scheduled,
            releasingId = releasingId,
            onRelease = onRelease,
        )
    }
}

internal fun partitionByKoreaToday(
    notices: List<AdminDelayedOpening>,
    koreaToday: String?,
): Pair<List<AdminDelayedOpening>, List<AdminDelayedOpening>> = notices.partition { it.noticeDate == koreaToday }

@Composable
private fun DelayedOpeningSection(
    title: String,
    emptyMessage: String,
    notices: List<AdminDelayedOpening>,
    releasingId: String?,
    onRelease: (String) -> Unit,
) {
    AppText(title, style = AppTextStyle.B2, color = CommonColor.Black)
    if (notices.isEmpty()) {
        AppText(emptyMessage, style = AppTextStyle.B2, color = GrayColor.C500)
        return
    }
    notices.forEach { notice ->
        AppText("${notice.noticeDate} ${notice.startTime ?: stringResource(R.string.admin_notice_undetermined)} · ${notice.description}", style = AppTextStyle.B2, color = GrayColor.C500)
        AppButton(
            text = stringResource(R.string.admin_delayed_opening_release),
            onClick = { onRelease(notice.id) },
            isLoading = releasingId == notice.id,
            enabled = releasingId == null,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CommonColor.Black,
        )
    }
}
