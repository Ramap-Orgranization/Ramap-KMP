package com.peto.ramap.designsystem.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheetConfig
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.auth.supportedLoginTypes
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.login_guide_close
import ramap.shared.generated.resources.login_illustration
import ramap.shared.generated.resources.login_required_description

@Composable
fun LoginGuideDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onLoginTypeSelected: (LoginType) -> Unit,
    loginButton: @Composable (LoginType, () -> Unit) -> Unit,
) {
    CommonBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        config =
            CommonBottomSheetConfig(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                showHandle = false,
                isNavigationBarPadded = true,
            ),
    ) { _ ->
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(Res.drawable.login_illustration),
                    contentDescription = null,
                    modifier = Modifier.size(width = 200.dp, height = 144.dp),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(20.dp))
                AppText(
                    text = stringResource(Res.string.login_required_description),
                    modifier = Modifier.fillMaxWidth(),
                    style = AppTextStyle.H4,
                    color = GrayColor.C500,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(28.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    supportedLoginTypes().forEach { loginType ->
                        loginButton(loginType) { onLoginTypeSelected(loginType) }
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(48.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_close),
                    contentDescription = stringResource(Res.string.login_guide_close),
                    modifier = Modifier.size(30.dp),
                    tint = GrayColor.C300,
                )
            }
        }
    }
}
