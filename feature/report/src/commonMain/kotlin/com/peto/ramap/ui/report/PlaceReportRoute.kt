package com.peto.ramap.ui.report

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.component.SettingsPage
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.report.contract.PlaceReportIntent
import com.peto.ramap.ui.report.contract.PlaceReportSideEffect
import com.peto.ramap.ui.report.contract.PlaceReportUiState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.place_report_action
import ramap.shared.generated.resources.place_report_description
import ramap.shared.generated.resources.place_report_placeholder
import ramap.shared.generated.resources.settings_report_menu

@Composable
fun PlaceReportRoute(
    onBack: () -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: PlaceReportViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is PlaceReportSideEffect.ShowToast -> toastManager.show(sideEffect.data)
        }
    }
    SettingsPage(Res.string.settings_report_menu, onBack) {
        PlaceReportContent(
            uiState = uiState,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun PlaceReportContent(
    uiState: PlaceReportUiState,
    viewModel: PlaceReportViewModel,
) {
    Box {
        SectionCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
        ) {
            AppText(
                stringResource(Res.string.place_report_description),
                AppTextStyle.B1,
                GrayColor.C400,
                Modifier.padding(top = 15.dp).padding(horizontal = 20.dp),
            )
            TextField(
                value = uiState.placeUrl,
                onValueChange = { viewModel.dispatch(PlaceReportIntent.OnPlaceUrlChanged(it)) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).padding(horizontal = 20.dp),
                placeholder = {
                    AppText(
                        stringResource(Res.string.place_report_placeholder),
                        AppTextStyle.B2,
                        GrayColor.C300,
                    )
                },
                minLines = 4,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = GrayColor.C050,
                        unfocusedContainerColor = GrayColor.C050,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
            )
            AppButton(
                text = stringResource(Res.string.place_report_action),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp),
                enabled = uiState.canSubmitPlaceUrl,
                onClick = { viewModel.dispatch(PlaceReportIntent.OnPlaceReportSubmit) },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (uiState.isSubmitting) {
            RamenLoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
