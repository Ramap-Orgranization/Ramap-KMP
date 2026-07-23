package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.main.map.model.RamenShopUiModel
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_detail_link_report
import ramap.shared.generated.resources.shop_information_report_action
import ramap.shared.generated.resources.shop_information_report_description
import ramap.shared.generated.resources.shop_information_report_dismiss
import ramap.shared.generated.resources.shop_information_report_placeholder

@Composable
internal fun ShopInformationReportDialog(
    shopUiModel: RamenShopUiModel,
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onSubmit: (Set<ShopInformationField>, String) -> Unit,
) {
    val shop = shopUiModel.shop
    var selectedFields by remember(shop.id) { mutableStateOf(emptySet<ShopInformationField>()) }
    var description by remember(shop.id) { mutableStateOf("") }
    val fieldOptions = shopUiModel.reportFieldOptions
    val canSubmit = selectedFields.isNotEmpty() || description.isNotBlank()

    CommonDialog(
        visible = visible,
        confirmText = stringResource(Res.string.shop_information_report_action),
        dismissText = stringResource(Res.string.shop_information_report_dismiss),
        confirmEnabled = canSubmit,
        onDismissRequest = onDismissRequest,
        content = {
            AppText(
                text = stringResource(Res.string.shop_detail_link_report),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
            AppText(
                text = stringResource(Res.string.shop_information_report_description, shop.name),
                modifier = Modifier.padding(top = 8.dp),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
                textAlign = TextAlign.Center,
            )
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                fieldOptions.forEach { option ->
                    val isSelected = option.field in selectedFields
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = isSelected,
                                    role = Role.Checkbox,
                                    onValueChange = { checked ->
                                        selectedFields =
                                            if (checked) selectedFields + option.field else selectedFields - option.field
                                    },
                                ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                            colors =
                                CheckboxDefaults.colors(
                                    checkedColor = GrayColor.C500,
                                    uncheckedColor = GrayColor.C300,
                                    checkmarkColor = CommonColor.White,
                                ),
                        )
                        AppText(
                            text = stringResource(option.label),
                            style = AppTextStyle.B2,
                            color = GrayColor.C500,
                        )
                    }
                }
            }
            TextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                placeholder = {
                    AppText(
                        text = stringResource(Res.string.shop_information_report_placeholder),
                        style = AppTextStyle.B2,
                        color = GrayColor.C300,
                    )
                },
            )
        },
        onConfirm = {
            if (canSubmit) onSubmit(selectedFields, description)
        },
        onDismiss = onDismissRequest,
    )
}
