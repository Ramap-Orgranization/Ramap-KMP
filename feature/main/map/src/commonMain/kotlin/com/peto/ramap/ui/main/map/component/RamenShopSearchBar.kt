package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.onFocusChanged
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
internal fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    isSearchMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val searchIconDescription = stringResource(Res.string.search_bar_search_icon)
    val clearActionDescription = stringResource(Res.string.search_bar_clear_action)
    var internalValue by rememberSaveable(query) { mutableStateOf(query) }
    var lastCommitted by remember(query) { mutableStateOf(query.trim()) }

    fun commitIfChanged() {
        val trimmed = internalValue.trim()

        if (trimmed.length in MIN_QUERY_LENGTH..MAX_QUERY_LENGTH) {
            lastCommitted = trimmed
            onQueryChange(trimmed)
        }
    }

    TextField(
        value = internalValue,
        onValueChange = { value ->
            val limitedValue = value.take(MAX_QUERY_LENGTH)
            internalValue = limitedValue

            if (limitedValue.trim().length < MIN_QUERY_LENGTH) {
                lastCommitted = limitedValue
                onQueryChange(limitedValue)
            }
        },
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
                Text(
                    text = "⌕",
                    modifier = Modifier.semantics { contentDescription = searchIconDescription },
                    fontSize = 28.sp,
                    color = GrayColor.C500,
                )
            }
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
                    commitIfChanged()
                    focusManager.clearFocus()
                },
            ),
    )
}

private const val MIN_QUERY_LENGTH = 2
private const val MAX_QUERY_LENGTH = 15

@Composable
@Preview
private fun SearchBarPreview() {
    RamapTheme {
        SearchBar(
            query = "",
            onQueryChange = {},
            onFocusChanged = {},
        )
    }
}
