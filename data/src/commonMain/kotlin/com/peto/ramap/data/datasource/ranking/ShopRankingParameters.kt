package com.peto.ramap.data.datasource.ranking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopRankingParameters(
    @SerialName("p_area") val area: String?,
    @SerialName("p_sigungu") val district: String?,
    @SerialName("p_category_ids") val categoryIds: List<String>,
    @SerialName("p_cursor_like_count") val cursorLikeCount: Long?,
    @SerialName("p_cursor_name") val cursorName: String?,
    @SerialName("p_cursor_id") val cursorId: String?,
    @SerialName("p_limit") val limit: Int,
)
