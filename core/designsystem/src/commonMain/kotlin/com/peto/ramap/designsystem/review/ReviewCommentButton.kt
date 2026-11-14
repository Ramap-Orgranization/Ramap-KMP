package com.peto.ramap.designsystem.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_review_comment_collapsed
import ramap.shared.generated.resources.ic_review_comment_expanded
import ramap.shared.generated.resources.review_comments
import ramap.shared.generated.resources.review_comments_collapse
import ramap.shared.generated.resources.review_comments_expand
import ramap.shared.generated.resources.review_comments_total

@Composable
internal fun ReviewCommentButton(
    count: Int?,
    expanded: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val label = count?.let { stringResource(Res.string.review_comments_total, it) } ?: stringResource(Res.string.review_comments)
    val action = stringResource(if (expanded) Res.string.review_comments_collapse else Res.string.review_comments_expand)
    val icon = if (expanded) Res.drawable.ic_review_comment_expanded else Res.drawable.ic_review_comment_collapsed
    Row(
        modifier =
            Modifier
                .heightIn(min = 40.dp)
                .semantics(mergeDescendants = true) { contentDescription = label }
                .noRippleClickable(enabled = enabled, role = Role.Button, onClickLabel = action, onClick = onToggle)
                .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        AppText(text = count?.toString() ?: "–", style = AppTextStyle.B2, color = GrayColor.C500)
    }
}
