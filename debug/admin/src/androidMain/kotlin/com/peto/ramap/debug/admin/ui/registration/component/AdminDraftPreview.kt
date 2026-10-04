package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.data.model.AdminDraft
import com.peto.ramap.debug.admin.data.model.AdminExternalVenue
import com.peto.ramap.debug.admin.ui.registration.formatDateRange
import com.peto.ramap.debug.admin.ui.registration.toOperatingNoticeType
import com.peto.ramap.designsystem.notice.OperatingNoticeSchedule
import com.peto.ramap.designsystem.resource.operatingnotice.ShopOperatingNoticeResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource as composeStringResource

@Composable
internal fun AdminDraftPreview(
    draft: AdminDraft,
    businessHours: BusinessHours? = null,
    modifier: Modifier = Modifier,
    onTitleChanged: (String) -> Unit = {},
    onDescriptionChanged: (String) -> Unit = {},
    externalVenues: List<AdminExternalVenue> = emptyList(),
    onVenueChanged: (String, String, String, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, GrayColor.C200, RoundedCornerShape(10.dp))
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppText(
            text = stringResource(R.string.admin_registration_preview_title),
            style = AppTextStyle.T2,
            color = GrayColor.C500,
        )
        AppText(
            text = stringResource(R.string.admin_registration_title_label),
            style = AppTextStyle.B1,
            color = GrayColor.C400,
        )
        AdminTitleField(draft.title, onTitleChanged)
        PreviewRow(stringResource(R.string.admin_registration_shop), draft.shopName)
        AdminExternalVenueField(
            venueName = draft.venueName.orEmpty(),
            venues = externalVenues,
            onNewVenueNameChanged = { onVenueChanged(it, draft.venueAddress.orEmpty(), "", draft.venueInstagramUrl.orEmpty(), draft.venueNaverMapUrl.orEmpty(), draft.venueKakaoMapUrl.orEmpty()) },
            onVenueSelected = { venue -> onVenueChanged(venue.name, venue.address.orEmpty(), venue.id, venue.instagramUrl.orEmpty(), venue.naverMapUrl.orEmpty(), venue.kakaoMapUrl.orEmpty()) },
        )
        AdminSourceField(draft.venueAddress.orEmpty(), { onVenueChanged(draft.venueName.orEmpty(), it, "", draft.venueInstagramUrl.orEmpty(), draft.venueNaverMapUrl.orEmpty(), draft.venueKakaoMapUrl.orEmpty()) }, R.string.admin_registration_venue_address_placeholder, readOnly = draft.externalVenueId != null)
        AdminSourceField(draft.venueInstagramUrl.orEmpty(), { onVenueChanged(draft.venueName.orEmpty(), draft.venueAddress.orEmpty(), "", it, draft.venueNaverMapUrl.orEmpty(), draft.venueKakaoMapUrl.orEmpty()) }, R.string.admin_registration_venue_instagram_placeholder, readOnly = draft.externalVenueId != null)
        AdminSourceField(draft.venueNaverMapUrl.orEmpty(), { onVenueChanged(draft.venueName.orEmpty(), draft.venueAddress.orEmpty(), "", draft.venueInstagramUrl.orEmpty(), it, draft.venueKakaoMapUrl.orEmpty()) }, R.string.admin_registration_venue_naver_map_placeholder, readOnly = draft.externalVenueId != null)
        AdminSourceField(draft.venueKakaoMapUrl.orEmpty(), { onVenueChanged(draft.venueName.orEmpty(), draft.venueAddress.orEmpty(), "", draft.venueInstagramUrl.orEmpty(), draft.venueNaverMapUrl.orEmpty(), it) }, R.string.admin_registration_venue_kakao_map_placeholder, readOnly = draft.externalVenueId != null)
        PreviewRow(
            stringResource(R.string.admin_registration_collaborators),
            draft.participants.joinToString(", ") { participant -> participant.name },
        )
        PreviewRow(stringResource(R.string.admin_registration_source), draft.sourceUrl)
        PreviewRow(
            stringResource(R.string.admin_registration_date_range),
            formatDateRange(draft.startDate, draft.endDate),
        )
        PreviewRow(
            stringResource(R.string.admin_registration_detailed_classification),
            draft.noticeType?.let { toOperatingNoticeType(it)?.label() },
        )
        OperatingNoticeSchedule(days = draft.dailySchedules.map { it.toDomain() }, businessHours = businessHours)
        PreviewRow(stringResource(R.string.admin_registration_start_time), draft.startTime)
        PreviewRow(stringResource(R.string.admin_registration_end_time), draft.endTime)
        AppText(
            text = stringResource(R.string.admin_registration_description),
            style = AppTextStyle.B1,
            color = GrayColor.C400,
        )
        OutlinedTextField(
            value = draft.description.orEmpty(),
            onValueChange = onDescriptionChanged,
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 10,
            placeholder = {
                Text(
                    text = stringResource(R.string.admin_registration_preview_empty),
                    color = GrayColor.C200,
                )
            },
            shape = RoundedCornerShape(10.dp),
            colors = previewTextFieldColors(),
        )
        if (draft.uncertainties.isNotEmpty()) {
            AppText(
                text = draft.uncertainties.joinToString("\n"),
                modifier = Modifier.fillMaxWidth(),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
            )
        }
    }
}

@Composable
internal fun AdminTitleField(
    title: String?,
    onTitleChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = title.orEmpty(),
        onValueChange = onTitleChanged,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(R.string.admin_registration_title_placeholder),
                color = GrayColor.C200,
            )
        },
        shape = RoundedCornerShape(10.dp),
        colors = previewTextFieldColors(),
    )
}

@Composable
private fun previewTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GrayColor.C200,
        unfocusedBorderColor = GrayColor.C200,
        cursorColor = GrayColor.C500,
    )

@Composable
private fun PreviewRow(
    label: String,
    value: String?,
) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppText(
            text = label,
            style = AppTextStyle.B2,
            color = GrayColor.C300,
        )
        AppText(
            text = value,
            modifier = Modifier.weight(1f),
            style = AppTextStyle.B2,
            color = GrayColor.C400,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun OperatingNoticeType.label(): String =
    when (this) {
        OperatingNoticeType.OPERATING_NOTICE,
        OperatingNoticeType.TEMPORARY_CLOSURE,
        OperatingNoticeType.EARLY_CLOSING,
        OperatingNoticeType.LATE_OPENING,
        -> composeStringResource(ShopOperatingNoticeResourceMapper.typeLabel(this))
    }

@Preview(showBackground = true)
@Composable
private fun AdminDraftPreviewPreview() {
    RamapTheme {
        AdminDraftPreview(
            draft =
                AdminDraft(
                    shopName = "멘야준",
                    title = "라멘 페스티벌 팝업",
                    eventType = "POPUP",
                    venueName = "라멘 페스티벌",
                    venueAddress = "서울시 마포구",
                    venueInstagramUrl = "https://www.instagram.com/ramen_festival/",
                    venueNaverMapUrl = "https://map.naver.com/p/entry/place/1",
                    venueKakaoMapUrl = "https://place.map.kakao.com/1",
                    sourceUrl = "https://instagram.com/p/...",
                    startDate = "2024-05-10",
                    endDate = "2024-05-16",
                    description = "특별한 팝업 이벤트입니다.",
                    evidencePath = "evidence/path.jpg",
                    uncertainties = listOf("운영 시간은 변동될 수 있습니다."),
                ),
            modifier = Modifier.padding(16.dp),
            externalVenues = previewExternalVenues(),
        )
    }
}

@Preview(name = "저장된 외부 장소 초안", showBackground = true)
@Composable
private fun AdminStoredExternalVenueDraftPreview() {
    RamapTheme {
        AdminDraftPreview(
            draft =
                AdminDraft(
                    shopName = "멘야준",
                    title = "라멘 페스티벌 팝업",
                    eventType = "POPUP",
                    venueName = "라멘 페스티벌",
                    venueAddress = "서울시 마포구 월드컵로 1",
                    externalVenueId = "venue-ramen-festival",
                    venueInstagramUrl = "https://www.instagram.com/ramen_festival/",
                    venueNaverMapUrl = "https://map.naver.com/p/entry/place/1",
                    venueKakaoMapUrl = "https://place.map.kakao.com/1",
                    startDate = "2024-05-10",
                    endDate = "2024-05-16",
                    description = "특별한 팝업 이벤트입니다.",
                ),
            externalVenues = previewExternalVenues(),
            modifier = Modifier.padding(16.dp),
        )
    }
}
