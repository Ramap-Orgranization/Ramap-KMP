package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminShopNameField(
    shopName: String,
    shopNames: List<String>,
    onShopNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showShopSuggestions by remember { mutableStateOf(false) }
    val suggestions = suggestedShopNames(shopNames, shopName)

    ExposedDropdownMenuBox(
        expanded = showShopSuggestions && suggestions.isNotEmpty(),
        onExpandedChange = { showShopSuggestions = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = shopName,
            onValueChange = {
                onShopNameChanged(it)
                showShopSuggestions = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
            placeholder = {
                AppText(
                    text = stringResource(R.string.admin_registration_shop_placeholder),
                    style = AppTextStyle.B2,
                    color = GrayColor.C200,
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GrayColor.C200,
                    unfocusedBorderColor = GrayColor.C200,
                    cursorColor = GrayColor.C500,
                ),
            singleLine = true,
        )

        ExposedDropdownMenu(
            expanded = showShopSuggestions && suggestions.isNotEmpty(),
            onDismissRequest = { showShopSuggestions = false },
            modifier = Modifier.fillMaxWidth(),
        ) {
            suggestions.forEach { name ->
                DropdownMenuItem(
                    text = {
                        AppText(
                            text = name,
                            style = AppTextStyle.B2,
                            color = GrayColor.C500,
                        )
                    },
                    onClick = {
                        onShopNameChanged(name)
                        showShopSuggestions = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

private fun suggestedShopNames(
    shopNames: List<String>,
    query: String,
): List<String> {
    val normalizedQuery = query.trim()
    return shopNames
        .asSequence()
        .filter { normalizedQuery.isBlank() || it.contains(normalizedQuery, ignoreCase = true) }
        .sortedWith(
            compareByDescending<String> { it.startsWith(normalizedQuery, ignoreCase = true) }
                .thenBy { it.lowercase() },
        ).take(MAX_SHOP_SUGGESTIONS)
        .toList()
}

private const val MAX_SHOP_SUGGESTIONS = 8

@Preview(showBackground = true)
@Composable
private fun AdminShopNameFieldPreview() {
    RamapTheme {
        AdminShopNameField(
            shopName = "멘야",
            shopNames = listOf("멘야준", "멘야코토", "멘야하나비"),
            onShopNameChanged = {},
        )
    }
}
