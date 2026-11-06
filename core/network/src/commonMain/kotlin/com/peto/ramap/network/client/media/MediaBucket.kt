package com.peto.ramap.network.client.media

import kotlin.jvm.JvmInline

@JvmInline
value class MediaBucket(
    val value: String,
) {
    /**
     * 외부 공개(Public) 접근이 허용된 저장소(Bucket)인지 확인한다.
     */
    val isPublic: Boolean
        get() = value in PUBLIC_BUCKET_NAMES

    /**
     * Bucket 이름의 기본 형식 유효성을 검사한다.
     * (공백이 아니어야 하며, 경로 구분자나 제어 문자를 포함하지 않아야 함)
     */
    val isValid: Boolean
        get() =
            value.isNotBlank() &&
                !value.contains('/') &&
                !value.contains('\\') &&
                !value.contains("..")

    companion object {
        val SHOP_PROFILE_IMAGES = MediaBucket(SHOP_PROFILE_IMAGES_NAME)
        val EVENT_IMAGES = MediaBucket(EVENT_IMAGES_NAME)
        val MENU_IMAGES = MediaBucket(MENU_IMAGES_NAME)

        const val SHOP_PROFILE_IMAGES_NAME = "shop-profile-images"
        const val EVENT_IMAGES_NAME = "event-images"
        const val MENU_IMAGES_NAME = "menu-images"

        private val PUBLIC_BUCKET_NAMES =
            setOf(
                SHOP_PROFILE_IMAGES_NAME,
                EVENT_IMAGES_NAME,
                MENU_IMAGES_NAME,
            )

        /**
         * 주어진 버킷 이름으로 [MediaBucket]을 생성하며, 공개 버킷인 경우에만 반환한다.
         */
        fun fromPublic(name: String?): MediaBucket? {
            if (name.isNullOrBlank()) return null
            val bucket = MediaBucket(name)
            return if (bucket.isPublic) bucket else null
        }
    }
}
