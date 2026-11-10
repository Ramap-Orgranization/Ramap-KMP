package com.peto.ramap.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
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
    val interactionSource = remember { MutableInteractionSource() }

    BasicTextField(
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
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions =
            KeyboardActions(
                onDone = {
                    onSearch()
                    focusManager.clearFocus()
                },
            ),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = query,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                leadingIcon = {
                    if (isSearchMode) {
                        IconButton(onClick = { focusManager.clearFocus() }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_arrow3_left),
                                contentDescription = searchIconDescription,
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                shape = RoundedCornerShape(28.dp),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = GrayColor.C400,
                    ),
            )
        },
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
