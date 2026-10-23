package com.peto.ramap.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.search_bar_clear_action
import ramap.shared.generated.resources.search_bar_placeholder
import ramap.shared.generated.resources.search_bar_search_icon

@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit = {},
    isSearchMode: Boolean = false,
    placeholder: String = stringResource(Res.string.search_bar_placeholder),
    onSearch: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val searchIconDescription = stringResource(Res.string.search_bar_search_icon)
    val clearActionDescription = stringResource(Res.string.search_bar_clear_action)

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(28.dp),
                    clip = false,
                ).onFocusChanged { onFocusChanged(it.isFocused) },
        shape = RoundedCornerShape(28.dp),
        singleLine = true,
        leadingIcon = {
            if (isSearchMode) {
                IconButton(onClick = { focusManager.clearFocus() }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow3_left),
                        contentDescription = stringResource(Res.string.search_bar_search_icon),
                        tint = GrayColor.C500,
                    )
                }
            } else {
                AppText(
                    text = "⌕",
                    modifier = Modifier.semantics { contentDescription = searchIconDescription },
                    style = AppTextStyle.H2,
                    color = GrayColor.C500,
                )
            }
        },
        placeholder = {
            AppText(
                text = placeholder,
                color = GrayColor.C400,
                style = AppTextStyle.B2,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_close),
                        contentDescription = clearActionDescription,
                        tint = GrayColor.C400,
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
                    onSearch()
                    focusManager.clearFocus()
                },
            ),
    )
}

@Composable
@Preview
private fun AppSearchBarPreview() {
    RamapTheme {
        AppSearchBar(
            query = "",
            onQueryChange = {},
            onFocusChanged = {},
        )
    }
}
