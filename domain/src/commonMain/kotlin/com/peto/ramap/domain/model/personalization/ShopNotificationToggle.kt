package com.peto.ramap.domain.model.personalization

/** 현재 개인화 상태에서 요청할 수 있는 알림 구독 변경. */
sealed interface ShopNotificationToggle {
    data object HiddenShop : ShopNotificationToggle

    data class Allowed(
        val enabled: Boolean,
    ) : ShopNotificationToggle
}
