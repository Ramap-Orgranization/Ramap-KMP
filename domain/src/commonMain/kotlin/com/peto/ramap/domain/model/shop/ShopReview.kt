package com.peto.ramap.domain.model.shop

data class ShopReview(
    val id: String,
    val shopId: String,
    val body: String,
    val createdAt: String,
) {
    companion object {
        fun isValidBody(body: String): Boolean = body.trim().encodeToByteArray().size in BODY_BYTE_LENGTH

        private val BODY_BYTE_LENGTH = 10..300
    }
}
