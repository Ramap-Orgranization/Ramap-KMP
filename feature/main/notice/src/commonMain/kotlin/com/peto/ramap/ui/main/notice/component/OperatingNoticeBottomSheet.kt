package com.peto.ramap.ui.main.notice.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheetConfig
import com.peto.ramap.domain.model.notice.OperatingNotice

@Composable
internal fun OperatingNoticeBottomSheet(
    notice: OperatingNotice,
    isSourceUrlSupported: (String) -> Boolean,
    onSourceClick: (String) -> Unit,
    onShopClick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    CommonBottomSheet(
        visible = true,
        onDismissRequest = onDismiss,
        config = CommonBottomSheetConfig(isDraggable = true),
    ) { modifier ->
        Column(
            modifier =
                modifier
                    .padding(horizontal = 5.dp)
                    .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OperatingNoticeCard(
                notice = notice,
                isSourceUrlSupported = isSourceUrlSupported,
                onSourceClick = onSourceClick,
                onShopClick = onShopClick,
            )
        }
    }
}
