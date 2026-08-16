package com.peto.ramap.designsystem.resource.event

import com.peto.ramap.designsystem.resource.UiText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_cancelled_notice
import ramap.shared.generated.resources.event_collaborator_person
import ramap.shared.generated.resources.event_collaborator_shop
import ramap.shared.generated.resources.event_sold_out_notice
import ramap.shared.generated.resources.event_status_cancelled
import ramap.shared.generated.resources.event_status_sold_out
import ramap.shared.generated.resources.event_status_today
import ramap.shared.generated.resources.event_status_upcoming
import ramap.shared.generated.resources.event_type_collab
import ramap.shared.generated.resources.event_type_limited_menu
import ramap.shared.generated.resources.event_type_popup
import ramap.shared.generated.resources.event_type_store_renewal
import ramap.shared.generated.resources.event_type_summer_limited
import ramap.shared.generated.resources.shop_event_notice_collab_participant_today
import ramap.shared.generated.resources.shop_event_notice_collab_participant_upcoming
import ramap.shared.generated.resources.shop_event_notice_collab_today
import ramap.shared.generated.resources.shop_event_notice_collab_upcoming
import ramap.shared.generated.resources.shop_event_notice_collab_upcoming_with_shop
import ramap.shared.generated.resources.shop_event_notice_limited_menu_today
import ramap.shared.generated.resources.shop_event_notice_limited_menu_upcoming
import ramap.shared.generated.resources.shop_event_notice_participant_today
import ramap.shared.generated.resources.shop_event_notice_participant_upcoming
import ramap.shared.generated.resources.shop_event_notice_popup_today
import ramap.shared.generated.resources.shop_event_notice_popup_upcoming
import ramap.shared.generated.resources.shop_event_notice_store_renewal_today
import ramap.shared.generated.resources.shop_event_notice_store_renewal_upcoming

object ShopEventResourceMapper {
    fun dateLabel(event: ShopEvent): StringResource =
        statusLabel(event) ?: if (event.isToday) Res.string.event_status_today else Res.string.event_status_upcoming

    fun statusLabel(event: ShopEvent): StringResource? =
        statusLabel(
            isCancelled = event.isCancelledToday,
            isSoldOut = event.isSoldOutToday,
        )

    fun statusLabel(
        isCancelled: Boolean,
        isSoldOut: Boolean,
    ): StringResource? =
        when {
            isCancelled -> Res.string.event_status_cancelled
            isSoldOut -> Res.string.event_status_sold_out
            else -> null
        }

    fun typeLabel(type: ShopEventType): StringResource =
        when (type) {
            ShopEventType.COLLAB -> Res.string.event_type_collab
            ShopEventType.POPUP -> Res.string.event_type_popup
            ShopEventType.LIMITED_MENU -> Res.string.event_type_limited_menu
            ShopEventType.SUMMER_LIMITED -> Res.string.event_type_summer_limited
            ShopEventType.STORE_RENEWAL -> Res.string.event_type_store_renewal
        }

    fun collaboratorLabel(event: ShopEvent): StringResource =
        if (event.collaboratorShops.isEmpty()) {
            Res.string.event_collaborator_person
        } else {
            Res.string.event_collaborator_shop
        }

    fun notice(event: ShopEvent): UiText? {
        if (event.type == ShopEventType.STORE_RENEWAL && event.isToday && !event.isStartDateToday) return null
        if (event.isCancelledToday) {
            return UiText(Res.string.event_cancelled_notice)
        }
        if (event.isSoldOutToday) {
            return UiText(Res.string.event_sold_out_notice)
        }
        event.upcomingCollaborationPartnerName?.let { partnerName ->
            return UiText(
                resource = Res.string.shop_event_notice_collab_upcoming_with_shop,
                arguments = listOf(partnerName),
            )
        }
        if (event.isVenue) return venueNotice(event)
        return participantNotice(event)
    }

    private fun venueNotice(event: ShopEvent): UiText {
        val resource =
            when (event.type) {
                ShopEventType.COLLAB ->
                    if (event.isToday) {
                        Res.string.shop_event_notice_collab_today
                    } else {
                        Res.string.shop_event_notice_collab_upcoming
                    }

                ShopEventType.POPUP ->
                    if (event.isToday) {
                        Res.string.shop_event_notice_popup_today
                    } else {
                        Res.string.shop_event_notice_popup_upcoming
                    }

                ShopEventType.LIMITED_MENU ->
                    if (event.isToday) {
                        Res.string.shop_event_notice_limited_menu_today
                    } else {
                        Res.string.shop_event_notice_limited_menu_upcoming
                    }

                ShopEventType.SUMMER_LIMITED ->
                    if (event.isToday) {
                        Res.string.shop_event_notice_limited_menu_today
                    } else {
                        Res.string.shop_event_notice_limited_menu_upcoming
                    }

                ShopEventType.STORE_RENEWAL ->
                    if (event.isToday) {
                        Res.string.shop_event_notice_store_renewal_today
                    } else {
                        Res.string.shop_event_notice_store_renewal_upcoming
                    }
            }
        return UiText(resource)
    }

    private fun participantNotice(event: ShopEvent): UiText {
        val resource =
            if (event.type == ShopEventType.COLLAB) {
                if (event.isToday) {
                    Res.string.shop_event_notice_collab_participant_today
                } else {
                    Res.string.shop_event_notice_collab_participant_upcoming
                }
            } else if (event.isToday) {
                Res.string.shop_event_notice_participant_today
            } else {
                Res.string.shop_event_notice_participant_upcoming
            }
        return UiText(resource, listOf(event.venueShop.name))
    }
}
