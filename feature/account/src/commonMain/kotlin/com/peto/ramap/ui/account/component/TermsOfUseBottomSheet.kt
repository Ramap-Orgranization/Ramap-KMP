package com.peto.ramap.ui.account.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheetConfig
import com.peto.ramap.domain.model.auth.SignupTerms
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
fun TermsOfUseBottomSheet(
    terms: SignupTerms,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    var checked by rememberSaveable(terms.version) { mutableStateOf(false) }
    var openedDocument by rememberSaveable(terms.version) { mutableStateOf<TermsOfUseDocument?>(null) }
    val goBack: () -> Unit = {
        if (openedDocument != null) openedDocument = null else onDismiss()
    }

    val backEventState =
        rememberNavigationEventState<NavigationEventInfo>(
            currentInfo = NavigationEventInfo.None,
        )
    NavigationBackHandler(
        state = backEventState,
        isBackEnabled = openedDocument != null,
        onBackCompleted = { openedDocument = null },
    )

    CommonBottomSheet(
        visible = true,
        onDismissRequest = onDismiss,
        isBackEnabled = openedDocument == null,
        config =
            CommonBottomSheetConfig(
                scrimColor = GrayColor.C500.copy(alpha = 0.4f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                showHandle = true,
                isDraggable = openedDocument == null,
                isContentDraggable = openedDocument == null,
                isNavigationBarPadded = true,
            ),
    ) {
        TermsOfUseContent(
            checked = checked,
            openedDocument = openedDocument,
            onCheck = { checked = it },
            onOpenDocument = { openedDocument = it },
            onOpenFullDocument = { ExternalUriOpener.open(it.publicUrl ?: terms.url) },
            onBack = goBack,
            onContinue = { if (checked) onAccept() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TermsOfUseBottomSheetPreview() {
    RamapTheme {
        TermsOfUseBottomSheet(
            terms = SignupTerms(version = "v1", url = "https://example.test/terms/v1"),
            onAccept = {},
            onDismiss = {},
        )
    }
}
