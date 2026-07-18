package com.peto.ramap.ui.report

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.component.SettingsPage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.report.contract.PlaceReportIntent
import com.peto.ramap.ui.report.contract.PlaceReportSideEffect
import com.peto.ramap.ui.report.contract.PlaceReportUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_refresh
import ramap.shared.generated.resources.place_report_action
import ramap.shared.generated.resources.place_report_description
import ramap.shared.generated.resources.place_report_location_address
import ramap.shared.generated.resources.place_report_location_address_failure
import ramap.shared.generated.resources.place_report_location_empty
import ramap.shared.generated.resources.place_report_location_refresh
import ramap.shared.generated.resources.place_report_location_section_title
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
        PlaceReportContent(uiState = uiState, viewModel = viewModel)
    }
}

@Composable
private fun PlaceReportContent(
    uiState: PlaceReportUiState,
    viewModel: PlaceReportViewModel,
) {
    val currentAddress =
        when {
            uiState.currentAddress != null ->
                stringResource(
                    Res.string.place_report_location_address,
                    uiState.currentAddress,
                )

            uiState.currentLocation != null -> stringResource(Res.string.place_report_location_address_failure)
            else -> stringResource(Res.string.place_report_location_empty)
        }
    val refreshDescription = stringResource(Res.string.place_report_location_refresh)

    SectionCard {
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
        AppText(
            stringResource(Res.string.place_report_location_section_title),
            AppTextStyle.B1,
            GrayColor.C500,
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 20.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(
                currentAddress,
                AppTextStyle.B2,
                GrayColor.C400,
                Modifier.weight(1f).padding(horizontal = 20.dp),
            )
            IconButton(
                enabled = uiState.currentLocation != null && !uiState.isAddressRefreshing,
                modifier =
                    Modifier
                        .padding(horizontal = 20.dp)
                        .semantics {
                            contentDescription = refreshDescription
                        },
                onClick = { viewModel.dispatch(PlaceReportIntent.OnCurrentAddressRefresh) },
            ) {
                Icon(
                    painterResource(Res.drawable.ic_refresh),
                    stringResource(Res.string.place_report_location_refresh),
                )
            }
        }
        AppButton(
            text = stringResource(Res.string.place_report_action),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp),
            enabled = uiState.currentLocation != null,
            onClick = { viewModel.dispatch(PlaceReportIntent.OnCurrentLocationReportSubmit) },
        )

        Spacer(modifier = Modifier.height(10.dp))
    }
}
