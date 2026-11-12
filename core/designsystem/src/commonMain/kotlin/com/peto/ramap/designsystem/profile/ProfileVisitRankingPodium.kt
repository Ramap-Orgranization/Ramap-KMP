package com.peto.ramap.designsystem.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.profile.ProfileVisitedShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.BrandColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.profile_visit_rank

@Composable
internal fun ProfileVisitRankingPodiumStep(
    rank: Int,
    profileVisitedShop: ProfileVisitedShop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFirst = rank == 1
    val photoBorder = if (isFirst) Modifier.border(2.dp, BrandColor.Foreground, CircleShape) else Modifier
    Column(
        modifier = modifier.noRippleClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RemoteShopImage(
            url = profileVisitedShop.instagramProfileImageUrl,
            modifier = Modifier.size(48.dp).then(photoBorder),
        )
        Spacer(Modifier.height(8.dp))
        AppText(
            text = profileVisitedShop.name,
            style = AppTextStyle.B3,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(4.dp))
        ProfileVisitCountBadge(visitCount = profileVisitedShop.visitCount)
        Spacer(Modifier.height(8.dp))
        ProfileVisitRankingPodiumBase(rank = rank, stepHeight = PODIUM_HEIGHTS[rank - 1].dp)
    }
}

@Composable
internal fun ProfileVisitRankingPodiumBase(
    rank: Int,
    stepHeight: Dp,
) {
    val isFirst = rank == 1
    val topStart = if (rank == 3) 0.dp else 10.dp
    val topEnd = if (rank == 2) 0.dp else 10.dp
    val outerShape =
        RoundedCornerShape(
            topStart = topStart,
            topEnd = topEnd,
            bottomStart = if (rank == 2) 6.dp else 0.dp,
            bottomEnd = if (rank == 3) 6.dp else 0.dp,
        )
    val frontShape = RoundedCornerShape(topStart = topStart, topEnd = topEnd)
    val topColor = if (isFirst) BrandColor.SubtleFill else Color(0xFFF7F6F3)
    val frontBrush =
        remember(isFirst) {
            Brush.verticalGradient(
                colors =
                    if (isFirst) {
                        listOf(BrandColor.SubtleFill, BrandColor.Fill)
                    } else {
                        listOf(Color(0xFFF0EFEC), Color(0xFFE7E5E1))
                    },
            )
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(stepHeight)
                .clip(outerShape)
                .background(topColor),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = 6.dp)
                    .background(brush = frontBrush, shape = frontShape),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = stringResource(Res.string.profile_visit_rank, rank),
                style = AppTextStyle.H3Brand,
                color = GrayColor.C500,
                fontSize = if (isFirst) 24.sp else TextUnit.Unspecified,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileVisitRankingPodiumStepPreview() {
    RamapTheme {
        ProfileVisitRankingPodiumStep(
            rank = 1,
            profileVisitedShop =
                ProfileVisitedShop(
                    id = "1",
                    name = "멘야하나비",
                    visitCount = 15,
                ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
