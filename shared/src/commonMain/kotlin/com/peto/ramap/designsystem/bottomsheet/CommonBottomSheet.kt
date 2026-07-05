package com.peto.ramap.designsystem.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    config: CommonBottomSheetConfig = CommonBottomSheetConfig(),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = false,
        )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = config.shape,
        containerColor = CommonColor.White,
        tonalElevation = 0.dp,
        scrimColor = config.scrimColor,
        dragHandle = {
            if (config.showHandle) {
                BottomSheetDefaults.DragHandle(
                    modifier =
                        Modifier.padding(
                            top = config.handleTopPadding,
                            bottom = config.handleBottomPadding,
                        ),
                    color = GrayColor.C100,
                )
            }
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        config.maxHeight?.let { maxHeight ->
                            Modifier.heightIn(max = maxHeight)
                        } ?: Modifier,
                    )
                    .verticalScroll(rememberScrollState()),
            content = content,
        )
    }
}
