package com.peto.ramap.ui.bookmark.importation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.component.RamenShopSearchResultList
import com.peto.ramap.designsystem.component.SettingsListPage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.importation.ImportationPreview
import com.peto.ramap.domain.model.importation.ImportationProvider
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.bookmark.importation.contract.ImportationIntent
import com.peto.ramap.ui.bookmark.importation.contract.ImportationSideEffect
import com.peto.ramap.ui.bookmark.importation.contract.ImportationUiState
import com.peto.ramap.ui.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.importation_analyze
import ramap.shared.generated.resources.importation_candidates_title
import ramap.shared.generated.resources.importation_confirm
import ramap.shared.generated.resources.importation_count
import ramap.shared.generated.resources.importation_hidden_count_label
import ramap.shared.generated.resources.importation_input_label
import ramap.shared.generated.resources.importation_registered_count_label
import ramap.shared.generated.resources.importation_remove_candidate
import ramap.shared.generated.resources.importation_reset
import ramap.shared.generated.resources.importation_screen_title
import ramap.shared.generated.resources.importation_unmatched_count_label
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.search_bar_clear_action

@Composable
fun ImportationRoute(
    onBack: () -> Unit,
    onImportCompleted: () -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: ImportationViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    ObserveAsEvents(viewModel.sideEffect) { effect ->
        when (effect) {
            is ImportationSideEffect.ShowToast -> toastManager.show(effect.toast)
            is ImportationSideEffect.ImportCompleted -> {
                toastManager.show(effect.toast)
                onImportCompleted()
            }
        }
    }

    ImportationScreen(
        uiState = uiState,
        onBack = onBack,
        onUrlChange = { viewModel.dispatch(ImportationIntent.UrlChanged(it)) },
        onAnalyze = { viewModel.dispatch(ImportationIntent.Analyze) },
        onRetry = { viewModel.dispatch(ImportationIntent.Retry) },
        onCandidateRemove = { viewModel.dispatch(ImportationIntent.CandidateRemoved(it)) },
        onConfirm = { viewModel.dispatch(ImportationIntent.Confirm) },
        onReset = { viewModel.dispatch(ImportationIntent.Reset) },
    )
}

@Composable
internal fun ImportationScreen(
    uiState: ImportationUiState,
    onBack: () -> Unit,
    onUrlChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onRetry: () -> Unit,
    onCandidateRemove: (String) -> Unit,
    onConfirm: () -> Unit,
    onReset: () -> Unit,
) {
    SettingsListPage(
        title = Res.string.importation_screen_title,
        onBack = onBack,
        showError = false,
        showInitialLoading = false,
        showOverlayLoading = uiState.isBusy,
        errorImage = Res.drawable.laduck_error_confused,
        errorDescription = Res.string.data_load_failure_message,
        onRetry = onRetry,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 30.dp)
                    .padding(bottom = 24.dp),
        ) {
            ImportationInput(uiState, onUrlChange, onAnalyze, onRetry)
            ImportationPreview(uiState, onCandidateRemove)
            if (uiState.preview != null) {
                Row {
                    AppButton(
                        onClick = onReset,
                        enabled = !uiState.isBusy,
                        modifier = Modifier.fillMaxWidth(0.3f).padding(top = 8.dp),
                        text = stringResource(Res.string.importation_reset),
                        backgroundColor = GrayColor.C100,
                        textColor = CommonColor.Black,
                        textStyle = AppTextStyle.T2,
                        cornerRadius = 8.dp,
                    )

                    Spacer(Modifier.width(10.dp))

                    AppButton(
                        onClick = onConfirm,
                        enabled = uiState.candidates.isNotEmpty() && !uiState.isBusy,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        text = stringResource(Res.string.importation_confirm),
                        backgroundColor = GrayColor.C500,
                        textColor = GrayColor.C100,
                        textStyle = AppTextStyle.T2,
                        cornerRadius = 8.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportationInput(
    uiState: ImportationUiState,
    onUrlChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val clearActionDescription = stringResource(Res.string.search_bar_clear_action)

        OutlinedTextField(
            value = uiState.url,
            onValueChange = onUrlChange,
            label = {
                AppText(
                    stringResource(Res.string.importation_input_label),
                    AppTextStyle.B2,
                    GrayColor.C300,
                )
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            enabled = !uiState.isBusy,
            shape = RoundedCornerShape(8.dp),
            trailingIcon = {
                if (uiState.url.isNotEmpty()) {
                    IconButton(
                        onClick = { onUrlChange("") },
                        enabled = !uiState.isBusy,
                    ) {
                        Text(
                            text = "×",
                            modifier =
                                Modifier.semantics {
                                    contentDescription = clearActionDescription
                                },
                            fontSize = 25.sp,
                            color = GrayColor.C300,
                        )
                    }
                }
            },
            singleLine = true,
        )
        AppButton(
            onClick = if (uiState.error == null) onAnalyze else onRetry,
            enabled = !uiState.isBusy,
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.importation_analyze),
            textColor = CommonColor.White,
            textStyle = AppTextStyle.T2,
            backgroundColor = GrayColor.C500,
            cornerRadius = 8.dp,
        )
    }
}

@Composable
private fun ImportationPreview(
    uiState: ImportationUiState,
    onCandidateRemove: (String) -> Unit,
) {
    val preview = uiState.preview ?: return
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(top = 10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ImportationStat(
                label = stringResource(Res.string.importation_registered_count_label),
                count = uiState.alreadyBookmarkedCount,
            )
            ImportationStat(
                label = stringResource(Res.string.importation_hidden_count_label),
                count = uiState.hiddenCount,
            )
            ImportationStat(
                label = stringResource(Res.string.importation_unmatched_count_label),
                count = preview.unmatchedPlaceNames.size,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AppText(
                text = stringResource(Res.string.importation_candidates_title),
                style = AppTextStyle.T2,
                color = GrayColor.C500,
            )

            AppText(
                text = stringResource(Res.string.importation_count, uiState.candidates.size),
                style = AppTextStyle.T2,
                color = InstagramColor.Pink,
            )
        }

        RamenShopSearchResultList(
            shops = uiState.candidates,
            onShopClick = {},
            categoryLabel = { category -> stringResource(CategoryResourceMapper.label(category)) },
            itemActionLabel = { stringResource(Res.string.importation_remove_candidate) },
            onItemAction = { shop -> onCandidateRemove(shop.id) },
            itemModifier = { Modifier.padding(vertical = 4.dp) },
        )
    }
}

@Composable
private fun ImportationStat(
    label: String,
    count: Int,
) {
    Column {
        AppText(text = label, style = AppTextStyle.C1, color = GrayColor.C300)
        AppText(
            text = stringResource(Res.string.importation_count, count),
            style = AppTextStyle.T2,
            color = GrayColor.C500,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun ImportationScreenPreview() {
    val samples = RamenShopPreviewParameterProvider().ramenShopPreviewSamples
    RamapTheme {
        ImportationScreen(
            uiState =
                ImportationUiState(
                    url = "https://kko.to/example",
                    preview =
                        ImportationPreview(
                            provider = ImportationProvider.KAKAO,
                            totalPlaceCount = 10,
                            matchedShopIds = samples.map { it.id }.toSet(),
                            unmatchedPlaceNames = listOf("모르는 라멘집 1", "모르는 라멘집 2"),
                        ),
                    candidates = RamenShops(samples),
                    alreadyBookmarkedCount = 1,
                    hiddenCount = 1,
                ),
            onBack = {},
            onUrlChange = {},
            onAnalyze = {},
            onRetry = {},
            onCandidateRemove = {},
            onConfirm = {},
            onReset = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun ImportationScreenEmptyPreview() {
    RamapTheme {
        ImportationScreen(
            uiState = ImportationUiState(),
            onBack = {},
            onUrlChange = {},
            onAnalyze = {},
            onRetry = {},
            onCandidateRemove = {},
            onConfirm = {},
            onReset = {},
        )
    }
}
