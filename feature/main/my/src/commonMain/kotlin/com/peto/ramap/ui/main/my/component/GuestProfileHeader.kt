package com.peto.ramap.ui.main.my.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.login.LoginButton
import com.peto.ramap.designsystem.profile.ProfileAvatar
import com.peto.ramap.designsystem.profile.ProfileDotField
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.auth.supportedLoginTypes
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.login_required_message
import ramap.shared.generated.resources.profile_photo

@Composable
internal fun GuestProfileHeader(
    onLoginClick: (LoginType) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        ProfileDotField(modifier = Modifier.fillMaxSize())
        ProfileAvatar(
            model = null,
            description = stringResource(Res.string.profile_photo),
            modifier = Modifier.size(128.dp),
        )
    }
    AppText(
        text = stringResource(Res.string.login_required_message),
        style = AppTextStyle.H3Brand,
        color = GrayColor.C400,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Column(
        modifier = Modifier.padding(top = 15.dp, start = 22.dp, end = 22.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        supportedLoginTypes().forEach { type ->
            LoginButton(
                type = type,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onLoginClick(type) },
            )
        }
    }
}
