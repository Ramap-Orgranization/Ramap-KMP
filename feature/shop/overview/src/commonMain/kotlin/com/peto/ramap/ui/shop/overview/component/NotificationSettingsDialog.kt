package com.peto.ramap.ui.shop.overview.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.peto.ramap.domain.model.notification.NotificationCatalog
import com.peto.ramap.domain.model.notification.NotificationSelection
import com.peto.ramap.domain.model.notification.NotificationTypeId
import com.peto.ramap.preview.NotificationCatalogPreviewParameterProvider
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun NotificationSettingsDialog(
    catalog: NotificationCatalog?,
    selection: NotificationSelection?,
    isLoading: Boolean,
    isSaving: Boolean,
    hasLoadFailure: Boolean,
    hasRevisionMismatch: Boolean,
    showPermissionGuide: Boolean,
    onTypeToggled: (NotificationTypeId) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSave: () -> Unit,
    onOpenSettings: () -> Unit,
    toastHost: @Composable (Modifier) -> Unit = {},
) {
    val canEdit = catalog != null && selection != null && !isLoading && !isSaving && !hasLoadFailure && !hasRevisionMismatch
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier =
                    Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .heightIn(max = maxHeight),
                shape = RoundedCornerShape(28.dp),
                color = CommonColor.White,
            ) {
                Box {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    ) {
                        NotificationSettingsHeader()
                        Column(
                            modifier =
                                Modifier
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            NotificationSettingsStatus(
                                isLoading = isLoading,
                                hasLoadFailure = hasLoadFailure,
                                hasRevisionMismatch = hasRevisionMismatch,
                                onRetry = onRetry,
                            )
                            if (isLoading) {
                                NotificationSettingsSkeleton()
                            } else if (catalog != null && selection != null) {
                                NotificationSettingsChoices(
                                    catalog = catalog,
                                    selection = selection,
                                    enabled = canEdit,
                                    onTypeToggled = onTypeToggled,
                                    onSelectAll = onSelectAll,
                                    onClear = onClear,
                                )
                            }
                        }
                        if (showPermissionGuide) {
                            NotificationPermissionGuide(onOpenSettings = onOpenSettings)
                        }
                        NotificationSettingsActions(
                            canSave = canEdit,
                            isSaving = isSaving,
                            onDismiss = onDismiss,
                            onSave = onSave,
                        )
                    }
                    toastHost(Modifier.matchParentSize())
                }
            }
        }
    }
}

@Preview(name = "매장 알림 로딩 중", widthDp = 390, heightDp = 844)
@Composable
private fun NotificationSettingsDialogLoadingPreview() {
    RamapTheme {
        NotificationSettingsDialog(
            catalog = null,
            selection = null,
            isLoading = true,
            isSaving = false,
            hasLoadFailure = false,
            hasRevisionMismatch = false,
            showPermissionGuide = false,
            onTypeToggled = {},
            onSelectAll = {},
            onClear = {},
            onDismiss = {},
            onRetry = {},
            onSave = {},
            onOpenSettings = {},
        )
    }
}

@Preview(name = "매장 알림 불러오기 실패", widthDp = 390, heightDp = 844)
@Preview(name = "매장 알림 불러오기 실패 작은 화면", widthDp = 320, heightDp = 568)
@Composable
private fun NotificationSettingsDialogLoadFailurePreview() {
    RamapTheme {
        NotificationSettingsDialog(
            catalog = null,
            selection = null,
            isLoading = false,
            isSaving = false,
            hasLoadFailure = true,
            hasRevisionMismatch = false,
            showPermissionGuide = false,
            onTypeToggled = {},
            onSelectAll = {},
            onClear = {},
            onDismiss = {},
            onRetry = {},
            onSave = {},
            onOpenSettings = {},
        )
    }
}

@Preview(name = "매장 알림", widthDp = 390, heightDp = 844)
@Composable
private fun NotificationSettingsDialogPreview(
    @PreviewParameter(NotificationCatalogPreviewParameterProvider::class) catalog: NotificationCatalog,
) {
    var selection by remember(catalog) {
        mutableStateOf(
            catalog.selection(
                catalog.types
                    .take(2)
                    .map { it.id }
                    .toSet(),
            ),
        )
    }
    RamapTheme {
        NotificationSettingsDialog(
            catalog = catalog,
            selection = selection,
            isLoading = false,
            isSaving = false,
            hasLoadFailure = false,
            hasRevisionMismatch = false,
            showPermissionGuide = false,
            onTypeToggled = { selection = selection.toggle(it, catalog) },
            onSelectAll = { selection = catalog.selectAll() },
            onClear = { selection = catalog.clear() },
            onDismiss = {},
            onRetry = {},
            onSave = {},
            onOpenSettings = {},
        )
    }
}
