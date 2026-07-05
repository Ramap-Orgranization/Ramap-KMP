package com.peto.ramap.designsystem.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.core.extension.noRippleClickable
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_toast_default
import ramap.shared.generated.resources.ic_toast_error
import ramap.shared.generated.resources.ic_toast_success
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ToastHost(
    toastManager: ToastManager,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 80.dp,
) {
    var current by remember { mutableStateOf<ToastData?>(null) }
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var dismissJob by remember { mutableStateOf<Job?>(null) }
    val animationMS = 200

    LaunchedEffect(toastManager) {
        toastManager.toasts.collectLatest { toast ->
            dismissJob?.cancel()
            current = toast
            visible = true

            dismissJob =
                scope.launch {
                    delay(toast.durationMills.milliseconds)
                    visible = false
                    delay(animationMS.toLong().milliseconds)
                    if (current == toast) current = null
                }
        }
    }

    fun dismiss() {
        dismissJob?.cancel()
        visible = false
        scope.launch {
            delay(animationMS.toLong().milliseconds)
            current = null
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible && current != null,
            enter =
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(animationMS),
                ) + fadeIn(tween(animationMS)),
            exit =
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(animationMS),
                ) + fadeOut(tween(animationMS)),
        ) {
            current?.let { toast ->
                ToastItem(
                    data = toast,
                    bottomPadding = bottomPadding,
                    onDismiss = ::dismiss,
                )
            }
        }
    }
}

@Composable
private fun ToastItem(
    data: ToastData,
    bottomPadding: Dp,
    onDismiss: () -> Unit,
) {
    val res =
        when (data.type) {
            ToastType.SUCCESS -> painterResource(Res.drawable.ic_toast_success)
            ToastType.ERROR -> painterResource(Res.drawable.ic_toast_error)
            ToastType.DEFAULT -> painterResource(Res.drawable.ic_toast_default)
        }

    Surface(
        modifier =
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = bottomPadding)
                .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GrayColor.C300),
        color = GrayColor.C100,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(
                        vertical = 12.dp,
                        horizontal = 16.dp,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = res,
                contentDescription = "toast icon",
                modifier = Modifier.size(24.dp),
            )

            Spacer(Modifier.width(3.5.dp))

            AppText(
                text = stringResource(data.message),
                style = AppTextStyle.B1,
                color = GrayColor.C500,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
            )

            data.action?.let {
                ActionButton(
                    action = it,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    action: ToastAction,
    onDismiss: () -> Unit,
) {
    AppText(
        text = stringResource(action.label),
        style = AppTextStyle.B1,
        color = CommonColor.White,
        modifier =
            Modifier
                .background(GrayColor.C300, RoundedCornerShape(8.dp))
                .padding(vertical = 5.5.dp, horizontal = 12.dp)
                .noRippleClickable(
                    onClick = {
                        action.onClick()
                        onDismiss()
                    },
                ),
    )
}
