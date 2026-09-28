package com.peto.ramap.ui.review.write.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.component.RamenShopSummary
import com.peto.ramap.designsystem.component.RamenShopSummarySkeleton
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import com.peto.ramap.designsystem.review.ReviewPage
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.review.write.contract.ReviewWriteIntent
import com.peto.ramap.ui.review.write.contract.ReviewWriteUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_review_submit
import ramap.shared.generated.resources.shop_review_write

@Composable
internal fun ReviewWriteRouteContent(
    state: ReviewWriteUiState,
    onBack: () -> Unit,
    onIntent: (ReviewWriteIntent) -> Unit,
    onPickImage: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ReviewPage(
            title = stringResource(Res.string.shop_review_write),
            onBack = onBack,
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 15.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                when {
                    state.shop != null -> {
                        item {
                            RamenShopSummary(
                                shop = state.shop,
                                categoryLabel = { category -> stringResource(CategoryResourceMapper.label(category)) },
                            )
                        }
                    }

                    state.isLoadingShop -> {
                        item {
                            RamenShopSummarySkeleton(
                                contentPadding = PaddingValues(start = 8.dp, bottom = 5.dp),
                            )
                        }
                    }
                }
                item {
                    PhotoEditor(
                        state = state,
                        onPick = onPickImage,
                        dispatch = onIntent,
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReviewTextEditor(state = state, onIntent = onIntent)
                    }
                }
                item {
                    ReviewVisibilitySetting(state = state, onIntent = onIntent)
                }
                item {
                    AppButton(
                        text = stringResource(Res.string.shop_review_submit),
                        onClick = { onIntent(ReviewWriteIntent.Submit) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                        enabled = state.canSubmit,
                        cornerRadius = 10.dp,
                    )
                }
            }
        }

        if (state.isSubmitting) {
            RamenLoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewWriteRoutePreview() {
    RamapTheme {
        ReviewWriteRouteContent(
            state =
                ReviewWriteUiState(
                    body = "정말 맛있는 라멘집입니다! 국물이 진하고 차슈가 부드러워요.",
                ),
            onBack = {},
            onIntent = {},
            onPickImage = {},
        )
    }
}
