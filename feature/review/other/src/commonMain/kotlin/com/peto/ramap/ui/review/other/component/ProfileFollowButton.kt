package com.peto.ramap.ui.review.other.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.domain.model.community.FollowState
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.resource.FollowResourceMapper
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileFollowButton(
    state: FollowState,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor =
        when (state) {
            FollowState.NONE -> ChromaticColor.Orange400
            FollowState.FOLLOWING -> CommonColor.White
            FollowState.PENDING -> GrayColor.C050
        }
    val textColor =
        when (state) {
            FollowState.NONE -> CommonColor.White
            FollowState.FOLLOWING -> GrayColor.C500
            FollowState.PENDING -> GrayColor.C300
        }

    AppButton(
        text = stringResource(FollowResourceMapper.stateActionLabel(state)),
        onClick = onClick,
        modifier =
            modifier
                .width(182.dp)
                .height(44.dp),
        textColor = textColor,
        textStyle = AppTextStyle.T3,
        backgroundColor = backgroundColor,
        cornerRadius = 24.dp,
        border = if (state == FollowState.FOLLOWING) BorderStroke(1.dp, GrayColor.C100) else null,
        icon = FollowResourceMapper.stateIcon(state),
        isLoading = isLoading,
        iconSize = 17.dp,
        iconTint = if (state == FollowState.FOLLOWING) ChromaticColor.Orange400 else textColor,
    )
}
