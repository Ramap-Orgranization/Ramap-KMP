package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.ShopReview
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_review_empty
import ramap.shared.generated.resources.shop_review_invalid_body
import ramap.shared.generated.resources.shop_review_placeholder
import ramap.shared.generated.resources.shop_review_submit
import ramap.shared.generated.resources.shop_review_title
import ramap.shared.generated.resources.shop_review_write
import ramap.shared.generated.resources.shop_review_cancel

@Composable
internal fun ShopReviewContent(
    reviews: List<ShopReview>,
    onWriteClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppText(
            text = stringResource(Res.string.shop_review_title),
            style = AppTextStyle.T1,
            color = GrayColor.C500,
        )
        if (reviews.isEmpty()) {
            AppText(
                text = stringResource(Res.string.shop_review_empty),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
            )
        } else {
            reviews.forEach { review ->
                AppText(text = review.body, style = AppTextStyle.B2, color = GrayColor.C500)
            }
        }
        AppButton(
            text = stringResource(Res.string.shop_review_write),
            modifier = Modifier.fillMaxWidth(),
            onClick = onWriteClick,
        )
    }
}

@Composable
internal fun ShopReviewDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var body by remember { mutableStateOf("") }
    val isValid = ShopReview.isValidBody(body)

    LaunchedEffect(visible) {
        if (!visible) body = ""
    }

    CommonDialog(
        visible = visible,
        confirmText = stringResource(Res.string.shop_review_submit),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = isValid,
        onDismissRequest = onDismiss,
        content = {
            AppText(
                text = stringResource(Res.string.shop_review_write),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
            )
            AppText(
                text = stringResource(Res.string.shop_review_placeholder),
                modifier = Modifier.padding(top = 8.dp),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
            )
            TextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
            )
            if (body.isNotBlank() && !isValid) {
                AppText(
                    text = stringResource(Res.string.shop_review_invalid_body),
                    modifier = Modifier.padding(top = 8.dp),
                    style = AppTextStyle.C1,
                    color = GrayColor.C400,
                )
            }
        },
        onConfirm = { onSubmit(body.trim()) },
        onDismiss = onDismiss,
    )
}
