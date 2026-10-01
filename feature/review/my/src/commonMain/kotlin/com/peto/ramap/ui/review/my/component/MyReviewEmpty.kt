package com.peto.ramap.ui.review.my.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.peto.ramap.designsystem.review.ReviewEmptyContent
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.my_reviews_empty_all
import ramap.shared.generated.resources.my_reviews_empty_private
import ramap.shared.generated.resources.my_reviews_empty_public

@Composable
internal fun MyReviewEmpty(
    filter: MyReviewVisibility,
    modifier: Modifier = Modifier,
) {
    val emptyText =
        when (filter) {
            MyReviewVisibility.ALL -> Res.string.my_reviews_empty_all
            MyReviewVisibility.PUBLIC -> Res.string.my_reviews_empty_public
            MyReviewVisibility.PRIVATE -> Res.string.my_reviews_empty_private
        }

    ReviewEmptyContent(
        text = stringResource(emptyText),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun MyReviewEmptyPreview() {
    RamapTheme {
        MyReviewEmpty(filter = MyReviewVisibility.ALL)
    }
}
