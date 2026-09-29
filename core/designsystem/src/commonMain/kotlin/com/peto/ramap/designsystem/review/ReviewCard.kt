package com.peto.ramap.designsystem.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.menu.AppDropdownMenu
import com.peto.ramap.designsystem.menu.AppDropdownMenuItem
import com.peto.ramap.designsystem.profile.ProfileAvatar
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.preview.ReviewPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_heart_filled
import ramap.shared.generated.resources.ic_heart_outline
import ramap.shared.generated.resources.ic_more_vert
import ramap.shared.generated.resources.ic_review_private
import ramap.shared.generated.resources.review_author_review_count
import ramap.shared.generated.resources.review_author_unknown
import ramap.shared.generated.resources.review_collapse
import ramap.shared.generated.resources.review_delete
import ramap.shared.generated.resources.review_edit
import ramap.shared.generated.resources.review_like
import ramap.shared.generated.resources.review_more
import ramap.shared.generated.resources.review_open_shop
import ramap.shared.generated.resources.review_photo_description
import ramap.shared.generated.resources.review_private_status
import ramap.shared.generated.resources.review_report
import ramap.shared.generated.resources.review_status_removed
import ramap.shared.generated.resources.review_visit_number
import ramap.shared.generated.resources.shop_detail_more_actions

@Composable
fun ReviewCard(
    review: Review,
    modifier: Modifier = Modifier,
    currentUserId: String? = null,
    currentProfileIsPublic: Boolean? = null,
    onOpenProfile: (String) -> Unit = {},
    onReport: (() -> Unit)? = null,
    onShopClick: (() -> Unit)? = null,
    onLike: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isLikeLoading: Boolean = false,
    actionsEnabled: Boolean = true,
) {
    var expanded by remember(review.id, review.body) { mutableStateOf(false) }
    var hasOverflow by remember(review.id, review.body) { mutableStateOf(false) }
    var menuExpanded by remember(review.id) { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = CommonColor.White,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .noRippleClickable(
                                enabled = review.author.userId.isNotBlank(),
                                role = Role.Button,
                            ) {
                                onOpenProfile(review.author.userId)
                            },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ProfileAvatar(
                        model = review.author.avatarUrl,
                        description = review.author.nickname,
                        modifier = Modifier.size(44.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        AppText(
                            text =
                                review.author.nickname.ifBlank {
                                    stringResource(Res.string.review_author_unknown)
                                },
                            style = AppTextStyle.T2,
                            color = GrayColor.C500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AppText(
                            text = stringResource(Res.string.review_author_review_count, review.author.reviewCount),
                            style = AppTextStyle.B2,
                            color = GrayColor.C300,
                        )
                    }
                }
                if (onEdit != null || onDelete != null || onReport != null) {
                    Box {
                        Image(
                            painter = painterResource(Res.drawable.ic_more_vert),
                            contentDescription = stringResource(Res.string.shop_detail_more_actions),
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .noRippleClickable(
                                        enabled = actionsEnabled,
                                        role = Role.Button,
                                    ) {
                                        menuExpanded = true
                                    },
                            colorFilter = ColorFilter.tint(GrayColor.C500),
                        )
                        AppDropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            if (onEdit != null) {
                                AppDropdownMenuItem(
                                    text = stringResource(Res.string.review_edit),
                                    enabled = actionsEnabled,
                                    onClick = {
                                        menuExpanded = false
                                        onEdit()
                                    },
                                )
                            }
                            if (onDelete != null) {
                                AppDropdownMenuItem(
                                    text = stringResource(Res.string.review_delete),
                                    enabled = actionsEnabled,
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    },
                                )
                            }
                            if (onReport != null) {
                                AppDropdownMenuItem(
                                    text = stringResource(Res.string.review_report),
                                    enabled = actionsEnabled,
                                    onClick = {
                                        menuExpanded = false
                                        onReport()
                                    },
                                )
                            }
                        }
                    }
                }
            }
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
                            modifier =
                                Modifier
                                    .size(220.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppText(
                    text = review.body,
                    style = AppTextStyle.B2,
                    color = GrayColor.C500,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { layout ->
                        if (!expanded) hasOverflow = layout.hasVisualOverflow
                    },
                )
                if (hasOverflow) {
                    AppText(
                        text = stringResource(if (expanded) Res.string.review_collapse else Res.string.review_more),
                        modifier = Modifier.clickable(role = Role.Button) { expanded = !expanded },
                        style = AppTextStyle.C1,
                        color = GrayColor.C300,
                    )
                }
            }
            if (review.moderationStatus == ReviewModerationStatus.REMOVED) {
                AppText(
                    text = stringResource(Res.string.review_status_removed),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = GrayColor.C300,
                    style = AppTextStyle.C1,
                )
            }
            if ((!review.isPublic || currentProfileIsPublic == false) &&
                !currentUserId.isNullOrBlank() &&
                review.author.userId == currentUserId
            ) {
                Row(
                    modifier =
                        Modifier
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(GrayColor.C050)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_review_private),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        colorFilter = ColorFilter.tint(GrayColor.C300),
                    )
                    AppText(
                        text = stringResource(Res.string.review_private_status),
                        style = AppTextStyle.C1,
                        color = GrayColor.C300,
                    )
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .noRippleClickable(
                                enabled = onLike != null && !isLikeLoading && actionsEnabled,
                            ) {
                                onLike?.invoke()
                            },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Image(
                        painter = painterResource(if (review.isLiked) Res.drawable.ic_heart_filled else Res.drawable.ic_heart_outline),
                        contentDescription = stringResource(Res.string.review_like, review.likeCount),
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(if (review.isLiked) SystemColor.Warning else GrayColor.C300),
                    )
                    AppText(
                        text = review.likeCount.toString(),
                        style = AppTextStyle.B2,
                        color = GrayColor.C500,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    AppText(
                        text = review.createdAt.take(10).replace('-', '.'),
                        style = AppTextStyle.B2,
                        color = GrayColor.C300,
                    )
                    AppText(
                        text = " · ",
                        style = AppTextStyle.T2,
                        color = GrayColor.C500,
                    )
                    AppText(
                        text =
                            stringResource(
                                Res.string.review_visit_number,
                                review.visitNumber,
                            ),
                        style = AppTextStyle.B2,
                        color = GrayColor.C300,
                    )
                }
            }
            if (onShopClick != null) {
                AppButton(
                    text = stringResource(Res.string.review_open_shop),
                    onClick = onShopClick,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    textStyle = AppTextStyle.B3,
                    textColor = GrayColor.C500,
                    backgroundColor = CommonColor.White,
                    cornerRadius = 0.dp,
                )
            }
            Spacer(
                modifier = Modifier.height(4.dp),
            )
            HorizontalDivider(color = GrayColor.C100)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewCardPreview(
    @PreviewParameter(ReviewPreviewParameterProvider::class) review: Review,
) {
    RamapTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            ReviewCard(
                review = review,
                onLike = {},
                onShopClick = {},
            )
            ReviewCard(
                review = review.copy(isPublic = false),
                currentUserId = review.author.userId,
                onEdit = {},
                onDelete = {},
            )
            ReviewCard(
                review = review.copy(isPublic = true),
                currentUserId = review.author.userId,
                currentProfileIsPublic = false,
                onEdit = {},
                onDelete = {},
            )
            ReviewCard(
                review = review,
                onReport = {},
            )
        }
    }
}
