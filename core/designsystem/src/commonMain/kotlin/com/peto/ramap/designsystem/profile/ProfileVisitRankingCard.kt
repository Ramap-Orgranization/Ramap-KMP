package com.peto.ramap.designsystem.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppIconButton
import com.peto.ramap.designsystem.button.RetryButton
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.profile.ProfileVisitRanking
import com.peto.ramap.domain.model.profile.ProfileVisitedShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_keyboard_arrow_down
import ramap.shared.generated.resources.ic_report_warning_circle
import ramap.shared.generated.resources.profile_visit_count
import ramap.shared.generated.resources.profile_visit_rank
import ramap.shared.generated.resources.profile_visit_ranking_empty
import ramap.shared.generated.resources.profile_visit_ranking_failed
import ramap.shared.generated.resources.profile_visit_ranking_own_scope
import ramap.shared.generated.resources.profile_visit_ranking_public_scope
import ramap.shared.generated.resources.profile_visit_ranking_scope_description
import ramap.shared.generated.resources.profile_visit_ranking_title
import ramap.shared.generated.resources.review_collapse
import ramap.shared.generated.resources.review_more

@Composable
fun ProfileVisitRankingCard(
    ranking: ProfileVisitRanking?,
    onShopClick: (String) -> Unit,
    loading: Boolean,
    failed: Boolean,
    publicReviewsOnly: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (ranking == null && !loading && !failed) return
    var showScope by rememberSaveable(publicReviewsOnly) { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(CommonColor.White)
                .border(1.dp, GrayColor.C100, shape)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(
                text = stringResource(Res.string.profile_visit_ranking_title),
                style = AppTextStyle.T2,
                color = GrayColor.C500,
            )
            AppIconButton(
                icon = Res.drawable.ic_report_warning_circle,
                contentDescription = stringResource(Res.string.profile_visit_ranking_scope_description),
                onClick = { showScope = !showScope },
                modifier = Modifier.size(44.dp),
            )
        }
        if (showScope) {
            AppText(
                text = stringResource(if (publicReviewsOnly) Res.string.profile_visit_ranking_public_scope else Res.string.profile_visit_ranking_own_scope),
                style = AppTextStyle.C1,
                color = GrayColor.C400,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(GrayColor.C050, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        when {
            failed -> RankingError(onRetry)
            ranking == null -> RamenLoadingIndicator(Modifier.align(Alignment.CenterHorizontally).size(36.dp))
            ranking.values.isEmpty() ->
                AppText(
                    text = stringResource(Res.string.profile_visit_ranking_empty),
                    style = AppTextStyle.B2,
                    color = GrayColor.C400,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

            else -> RankingContent(ranking.values, onShopClick)
        }
    }
}

@Composable
private fun RankingError(onRetry: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = stringResource(Res.string.profile_visit_ranking_failed),
            style = AppTextStyle.C1,
            color = GrayColor.C400,
            modifier = Modifier.weight(1f),
        )
        RetryButton(onClick = onRetry)
    }
}

@Composable
private fun RankingContent(
    profileVisitedShops: List<ProfileVisitedShop>,
    onShopClick: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val canExpand = profileVisitedShops.size > PODIUM_SIZE
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = GrayColor.C100,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            for (rank in PODIUM_ORDER) {
                val shop = profileVisitedShops.getOrNull(rank - 1)
                if (shop == null) {
                    Spacer(Modifier.weight(1f))
                } else {
                    PodiumStep(
                        rank = rank,
                        profileVisitedShop = shop,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (canExpand) {
            RankingDisclosureButton(
                expanded = expanded,
                onClick = { expanded = !expanded },
            )
        }
        if (canExpand && expanded) {
            RankingChart(profileVisitedShops, onShopClick)
        }
    }
}

@Composable
private fun PodiumStep(
    rank: Int,
    profileVisitedShop: ProfileVisitedShop,
    modifier: Modifier = Modifier,
) {
    val stepHeight = PODIUM_HEIGHTS[rank - 1].dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppText(
            text = profileVisitedShop.name,
            style = AppTextStyle.B3,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(4.dp))
        AppText(
            text = stringResource(Res.string.profile_visit_count, profileVisitedShop.visitCount),
            style = AppTextStyle.C1,
            color = GrayColor.C400,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(stepHeight)
                    .background(
                        color = if (rank == 1) ChromaticColor.Orange300 else GrayColor.C100,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = stringResource(Res.string.profile_visit_rank, rank),
                style = AppTextStyle.T2,
                color = GrayColor.C500,
            )
        }
    }
}

@Composable
private fun RankingDisclosureButton(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = stringResource(if (expanded) Res.string.review_collapse else Res.string.review_more),
            style = AppTextStyle.B3,
            color = GrayColor.C500,
        )
        Icon(
            painter = painterResource(Res.drawable.ic_keyboard_arrow_down),
            contentDescription = null,
            tint = GrayColor.C400,
            modifier =
                Modifier
                    .size(16.dp)
                    .rotate(if (expanded) 180f else 0f),
        )
    }
}

@Composable
private fun RankingChart(
    profileVisitedShops: List<ProfileVisitedShop>,
    onShopClick: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = GrayColor.C100,
                        start = Offset.Zero,
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        profileVisitedShops.drop(PODIUM_SIZE).forEachIndexed { index, shop ->
            ProfileVisitRankingRow(
                rank = PODIUM_SIZE + index + 1,
                profileVisitedShop = shop,
                onShopClick = onShopClick,
            )
        }
    }
}

private const val PODIUM_SIZE = 3
private val PODIUM_ORDER = listOf(2, 1, 3)
private val PODIUM_HEIGHTS = listOf(52, 40, 28)

@Preview(showBackground = true)
@Composable
private fun ProfileVisitRankingCardPreview() {
    RamapTheme {
        ProfileVisitRankingCard(
            ranking =
                ProfileVisitRanking(
                    values =
                        listOf(
                            ProfileVisitedShop(id = "1", name = "멘야하나비", visitCount = 15),
                            ProfileVisitedShop(id = "2", name = "라멘모토", visitCount = 12),
                            ProfileVisitedShop(id = "3", name = "오레노라멘", visitCount = 8),
                            ProfileVisitedShop(id = "4", name = "부타이", visitCount = 5),
                        ),
                ),
            onShopClick = {},
            loading = false,
            failed = false,
            publicReviewsOnly = true,
            onRetry = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
