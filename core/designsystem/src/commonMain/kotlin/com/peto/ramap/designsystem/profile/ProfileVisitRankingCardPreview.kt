package com.peto.ramap.designsystem.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.domain.model.profile.ProfileVisitRanking
import com.peto.ramap.domain.model.profile.ProfileVisitedShop
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Preview(name = "조회 결과 없음 · 카드 숨김", group = "방문 통계", widthDp = 360)
@Composable
internal fun VisitRankingUnavailablePreview() = VisitRankingPreview()

@Preview(name = "매장 0곳 · 카드 숨김", group = "방문 통계", widthDp = 360)
@Composable
internal fun VisitRankingEmptyPreview() = VisitRankingPreview(ranking = sampleRanking())

@Preview(name = "매장 1곳 · 카드 숨김", group = "방문 통계", widthDp = 360)
@Composable
internal fun VisitRankingOneShopPreview() = VisitRankingPreview(ranking = sampleRanking(15))

@Preview(name = "매장 2곳 · 카드 숨김", group = "방문 통계", widthDp = 360)
@Composable
internal fun VisitRankingTwoShopsPreview() = VisitRankingPreview(ranking = sampleRanking(15, 12))

@Preview(name = "1·2번째 동률 · 카드 숨김", group = "방문 통계", widthDp = 360)
@Composable
internal fun VisitRankingFirstTiePreview() = VisitRankingPreview(ranking = sampleRanking(15, 15, 8, 5))

@Preview(name = "2·3번째 동률 · 카드 숨김", group = "방문 통계", widthDp = 360)
@Composable
internal fun VisitRankingSecondTiePreview() = VisitRankingPreview(ranking = sampleRanking(15, 8, 8, 5))

@Preview(name = "3·4번째 동률 · 카드 숨김", group = "방문 통계", widthDp = 360)
@Composable
internal fun VisitRankingThirdTiePreview() = VisitRankingPreview(ranking = sampleRanking(15, 12, 8, 8))

@Preview(name = "시상대만 표시", group = "방문 통계", widthDp = 360)
@Composable
internal fun VisitRankingPodiumPreview() = VisitRankingPreview(ranking = sampleRanking(15, 12, 8))

@Preview(name = "더보기 접힘", group = "방문 통계", widthDp = 360)
@Composable
internal fun VisitRankingCollapsedPreview() = VisitRankingPreview(ranking = sampleRanking(15, 12, 8, 5))

@Preview(name = "더보기 펼침 · 10위까지 표시", group = "방문 통계", widthDp = 360)
@Composable
internal fun VisitRankingExpandedPreview() =
    VisitRankingPreview(
        ranking = sampleRanking(15, 13, 10, 7, 6, 5, 4, 3, 2, 1),
        showMore = true,
    )

@Preview(name = "더보기 펼침 · 동률 표시", group = "방문 통계", widthDp = 360)
@Composable
internal fun VisitRankingExpandedWithTiesPreview() =
    VisitRankingPreview(
        ranking = sampleRanking(15, 13, 10, 7, 7, 5, 4, 3, 2, 1),
        showMore = true,
    )

@Composable
private fun VisitRankingPreview(
    ranking: ProfileVisitRanking? = null,
    showMore: Boolean = false,
) {
    RamapTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .background(GrayColor.C050)
                    .padding(12.dp),
        ) {
            ProfileVisitRankingCard(
                ranking = ranking,
                onShopClick = {},
                modifier = Modifier,
                initiallyShowMore = showMore,
            )
        }
    }
}

private fun sampleRanking(vararg visitCounts: Long): ProfileVisitRanking =
    ProfileVisitRanking(
        values =
            visitCounts.mapIndexed { index, count ->
                ProfileVisitedShop(
                    id = index.toString(),
                    name = sampleShopNames[index],
                    visitCount = count,
                )
            },
    )

private val sampleShopNames =
    listOf("멘야하나비", "라멘모토", "오레노라멘", "부타이", "라멘베라보", "이츠모라멘 수원점", "세상끝의라멘", "하쿠텐", "토리키치", "라멘집 아저씨")
