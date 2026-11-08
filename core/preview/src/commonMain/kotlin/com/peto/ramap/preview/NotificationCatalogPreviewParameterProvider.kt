package com.peto.ramap.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.notification.NotificationCatalog
import com.peto.ramap.domain.model.notification.NotificationType
import com.peto.ramap.domain.model.notification.NotificationTypeId

class NotificationCatalogPreviewParameterProvider : PreviewParameterProvider<NotificationCatalog> {
    override val values: Sequence<NotificationCatalog> =
        sequenceOf(
            NotificationCatalog(
                revision = 1L,
                types =
                    listOf(
                        NotificationType(NotificationTypeId("event"), "이벤트", "매장의 이벤트 소식", 0),
                        NotificationType(NotificationTypeId("temporary_closure"), "임시휴무", "매장의 임시휴무 소식", 1),
                        NotificationType(NotificationTypeId("delayed_opening"), "오픈 지연", "매장의 오픈 지연 소식", 2),
                        NotificationType(NotificationTypeId("early_closure"), "조기 마감", "매장의 조기 마감 소식", 3),
                        NotificationType(NotificationTypeId("shortened_hours"), "단축 영업", "매장의 단축 영업 소식", 4),
                        NotificationType(NotificationTypeId("temporary_suspension"), "영업 일시 중단", "매장의 영업 일시 중단 소식", 5),
                    ),
            ),
        )
}
