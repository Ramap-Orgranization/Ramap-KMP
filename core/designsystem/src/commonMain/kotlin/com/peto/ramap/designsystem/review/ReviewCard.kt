package com.peto.ramap.designsystem.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.shop.ShopReview
import com.peto.ramap.theme.AppTextStyle
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_author_unknown
import ramap.shared.generated.resources.review_open_shop
import ramap.shared.generated.resources.review_photo_description
import ramap.shared.generated.resources.review_report
import ramap.shared.generated.resources.review_status_pending
import ramap.shared.generated.resources.review_status_rejected
import ramap.shared.generated.resources.review_status_removed

@Composable
fun ReviewCard(
    review: ShopReview,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
    onReport: (() -> Unit)? = null,
    onShopClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        border = BorderStroke(1.dp, colors.outlineVariant),
        color = colors.surface,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            top = 12.dp,
                            end = 4.dp,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable(
                                enabled = review.author.userId.isNotBlank(),
                                role = Role.Button,
                            ) {
                                onOpenProfile(review.author.userId)
                            },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ReviewPixelAvatar(
                        seed = review.author.userId,
                        modifier = Modifier.size(44.dp),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        AppText(
                            text =
                                review.author.nickname.ifBlank {
                                    stringResource(Res.string.review_author_unknown)
                                },
                            style = AppTextStyle.B2,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        AppText(
                            text = review.createdAt.take(10).replace('-', '.'),
                            style = AppTextStyle.C2,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                if (onReport != null) {
                    TextButton(
                        onClick = onReport,
                    ) {
                        AppText(
                            text = stringResource(Res.string.review_report),
                            style = AppTextStyle.B3,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            AppText(
                text = review.body,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = AppTextStyle.B1,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (review.imageUrls.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    itemsIndexed(review.imageUrls) { index, url ->
                        AsyncImage(
                            model = url,
                            contentDescription =
                                stringResource(
                                    Res.string.review_photo_description,
                                    index + 1,
                                ),
                            modifier = Modifier.size(220.dp),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
            if (review.moderationStatus != ReviewModerationStatus.PUBLISHED) {
                AppText(
                    text =
                        stringResource(
                            when (review.moderationStatus) {
                                ReviewModerationStatus.PENDING -> Res.string.review_status_pending
                                ReviewModerationStatus.REJECTED -> Res.string.review_status_rejected
                                else -> Res.string.review_status_removed
                            },
                        ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = colors.onSurfaceVariant,
                    style = AppTextStyle.C1,
                )
            }
            if (onShopClick != null) {
                TextButton(
                    onClick = onShopClick,
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    AppText(
                        text = stringResource(Res.string.review_open_shop),
                        style = AppTextStyle.B3,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(
                modifier = Modifier.height(0.dp),
            )
        }
    }
}
