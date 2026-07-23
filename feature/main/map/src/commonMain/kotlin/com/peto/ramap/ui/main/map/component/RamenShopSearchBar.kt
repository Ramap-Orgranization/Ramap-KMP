package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.search_bar_clear_action
import ramap.shared.generated.resources.search_bar_placeholder
import ramap.shared.generated.resources.search_bar_search_icon

@Composable
internal fun RamenShopSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val searchIconDescription = stringResource(Res.string.search_bar_search_icon)
    val clearActionDescription = stringResource(Res.string.search_bar_clear_action)
    var internalValue by rememberSaveable(query) { mutableStateOf(query) }
    var lastCommitted by remember(query) { mutableStateOf(query.trim()) }

    fun commitIfChanged() {
        val trimmed = internalValue.trim()

        if (trimmed.length in 2..15) {
            lastCommitted = trimmed
            onQueryChange(trimmed)
        }
    }

    TextField(
        value = internalValue,
        onValueChange = { value ->
            internalValue = value

            if (value.isBlank() && lastCommitted.isNotBlank()) {
                lastCommitted = ""
                onQueryChange("")
            }
        },
        modifier =
            modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(28.dp),
                    clip = false,
                ),
        shape = RoundedCornerShape(28.dp),
        singleLine = true,
        leadingIcon = {
            Text(
                text = "⌕",
                modifier =
                    Modifier.semantics {
                        contentDescription = searchIconDescription
                    },
                fontSize = 28.sp,
                color = GrayColor.C500,
            )
        },
        placeholder = {
            AppText(
                text = stringResource(Res.string.search_bar_placeholder),
                color = GrayColor.C400,
                style = AppTextStyle.B2,
            )
        },
        trailingIcon = {
            if (internalValue.isNotEmpty()) {
                IconButton(
                    onClick = {
                        internalValue = ""
                        lastCommitted = ""
                        onQueryChange("")
                    },
                ) {
                    Text(
                        text = "×",
                        modifier =
                            Modifier.semantics {
                                contentDescription = clearActionDescription
                            },
                        fontSize = 25.sp,
                        color = GrayColor.C400,
                    )
                }
            }
        },
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = GrayColor.C400,
            ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions =
            KeyboardActions(
                onDone = {
                    commitIfChanged()
                    focusManager.clearFocus()
                },
            ),
    )
}

@Composable
@Preview
private fun RamenShopSearchBarPreview() {
    RamenShopSearchBar(
        query = "",
        onQueryChange = {},
    )
}
