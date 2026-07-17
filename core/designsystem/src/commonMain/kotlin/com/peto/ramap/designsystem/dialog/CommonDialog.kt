package com.peto.ramap.designsystem.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.DimmedColor
import com.peto.ramap.theme.GrayColor

@Composable
fun CommonDialog(
    modifier: Modifier = Modifier,
    visible: Boolean,
    confirmText: String,
    dismissText: String? = null,
    confirmEnabled: Boolean = true,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(modifier),
            contentAlignment = Alignment.Center,
        ) {
            DialogScrim(visible = true, onDismissRequest = onDismissRequest)

            DialogContent(
                modifier =
                    Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                visible = true,
                confirmText = confirmText,
                dismissText = dismissText,
                confirmEnabled = confirmEnabled,
                content = content,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun DialogScrim(
    visible: Boolean,
    onDismissRequest: () -> Unit,
) {
    val fadeDuration = 160

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(fadeDuration)),
        exit = fadeOut(animationSpec = tween(fadeDuration)),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(DimmedColor.D070)
                    .noRippleClickable(onClick = onDismissRequest),
        )
    }
}

@Composable
private fun DialogContent(
    modifier: Modifier,
    visible: Boolean,
    confirmText: String,
    dismissText: String? = null,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    val fadeDuration = 160

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(fadeDuration)),
        exit = fadeOut(animationSpec = tween(fadeDuration)),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CommonColor.White,
            modifier = modifier,
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 28.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                content()

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                ) {
                    if (dismissText != null && onDismiss != null) {
                        AppButton(
                            modifier = Modifier.weight(1f),
                            text = dismissText,
                            textColor = GrayColor.C500,
                            backgroundColor = CommonColor.White,
                            border = BorderStroke(1.dp, GrayColor.C500),
                            onClick = onDismiss,
                        )

                        Spacer(Modifier.width(8.dp))
                    }

                    AppButton(
                        modifier = Modifier.weight(1f),
                        text = confirmText,
                        enabled = confirmEnabled,
                        onClick = onConfirm,
                    )
                }
            }
        }
    }
}
