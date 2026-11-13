package com.peto.ramap.ui.onboarding.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.login.LoginButton
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.auth.supportedLoginTypes
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SurfaceColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.login_illustration
import ramap.shared.generated.resources.onboarding_login_benefit
import ramap.shared.generated.resources.onboarding_login_continue_as_guest
import ramap.shared.generated.resources.onboarding_login_title

@Composable
internal fun OnboardingLoginContent(
    isSigningIn: Boolean,
    isCompleting: Boolean,
    onLoginTypeSelected: (LoginType) -> Unit,
    onContinueWithoutLogin: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(SurfaceColor.Background)
                .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxSize().padding(horizontal = 24.dp),
        ) {
            if (isCompleting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.login_illustration),
                        contentDescription = null,
                        modifier = Modifier.size(144.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppText(
                        text = stringResource(Res.string.onboarding_login_title),
                        style = AppTextStyle.H3,
                        color = GrayColor.C500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AppText(
                        text = stringResource(Res.string.onboarding_login_benefit),
                        style = AppTextStyle.B2,
                        color = GrayColor.C300,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(28.dp))

                    if (isSigningIn) {
                        RamenLoadingIndicator()
                    } else {
                        LoginOptions(
                            enabled = !isCompleting,
                            onLoginTypeSelected = onLoginTypeSelected,
                            onContinueWithoutLogin = onContinueWithoutLogin,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginOptions(
    enabled: Boolean,
    onLoginTypeSelected: (LoginType) -> Unit,
    onContinueWithoutLogin: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        supportedLoginTypes().forEach { type ->
            LoginButton(
                type = type,
                onClick = { onLoginTypeSelected(type) },
                enabled = enabled,
            )
        }

        AppText(
            text = stringResource(Res.string.onboarding_login_continue_as_guest),
            style = AppTextStyle.B2,
            color = GrayColor.C400,
            textAlign = TextAlign.Center,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.fillMaxWidth().noRippleClickable(enabled = enabled, onClick = onContinueWithoutLogin),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingLoginContentPreview() {
    RamapTheme {
        OnboardingLoginContent(
            isSigningIn = false,
            isCompleting = false,
            onLoginTypeSelected = {},
            onContinueWithoutLogin = {},
        )
    }
}
