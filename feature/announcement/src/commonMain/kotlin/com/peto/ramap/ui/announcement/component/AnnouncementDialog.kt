package com.peto.ramap.ui.announcement.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppMarkdown
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.announcement.Announcement
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.announcement_detail_error_description
import ramap.shared.generated.resources.announcement_detail_error_title
import ramap.shared.generated.resources.announcement_title
import ramap.shared.generated.resources.ic_news
import ramap.shared.generated.resources.notification_load_error_illustration
import ramap.shared.generated.resources.review_close

@Composable
internal fun AnnouncementDialog(
    announcement: Announcement,
    onDismiss: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val contentMaxHeight = maxHeight * 0.6f
        CommonDialog(
            visible = true,
            confirmText = stringResource(Res.string.review_close),
            onDismissRequest = onDismiss,
            onConfirm = onDismiss,
        ) {
            key(announcement.id) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = contentMaxHeight)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AnnouncementHeader(announcement = announcement)
                    HorizontalDivider(color = GrayColor.C100)
                    if (announcement.bodyMarkdown.isBlank()) {
                        AnnouncementBodyError(modifier = Modifier.fillMaxWidth())
                    } else {
                        AppMarkdown(
                            content = announcement.bodyMarkdown,
                            modifier = Modifier.fillMaxWidth(),
                            errorContent = { modifier -> AnnouncementBodyError(modifier = modifier) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementBodyError(modifier: Modifier) {
    LoadErrorContent(
        image = Res.drawable.notification_load_error_illustration,
        title = stringResource(Res.string.announcement_detail_error_title),
        description = stringResource(Res.string.announcement_detail_error_description),
        modifier = modifier,
        compact = true,
    )
}

@Composable
private fun AnnouncementHeader(announcement: Announcement) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_news),
                contentDescription = null,
                tint = GrayColor.C400,
                modifier = Modifier.size(18.dp),
            )
            AppText(
                text = stringResource(Res.string.announcement_title),
                style = AppTextStyle.B4,
                color = GrayColor.C400,
            )
        }
        AppText(
            text = announcement.title,
            style = AppTextStyle.H3,
            color = GrayColor.C500,
        )
        AppText(
            text =
                announcement.publishedAt
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .toString()
                    .replace('-', '.'),
            style = AppTextStyle.C1,
            color = GrayColor.C300,
        )
    }
}
