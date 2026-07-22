package com.peto.ramap.data.datasource.ranking

import com.peto.ramap.domain.model.rank.RankingQuery
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShopRankingParameters(
    @SerialName("p_area") val area: String?,
    @SerialName("p_category_ids") val categoryIds: List<String>,
    @SerialName("p_cursor_like_count") val cursorLikeCount: Long?,
    @SerialName("p_cursor_name") val cursorName: String?,
    @SerialName("p_cursor_id") val cursorId: String?,
    @SerialName("p_limit") val limit: Int,
)

fun RankingQuery.toRequest(): ShopRankingParameters =
    ShopRankingParameters(
        area = area?.name,
        categoryIds = categories.map { category -> category.id },
        cursorLikeCount = cursor?.likeCount,
        cursorName = cursor?.name,
        cursorId = cursor?.shopId,
        limit = limit,
    )
