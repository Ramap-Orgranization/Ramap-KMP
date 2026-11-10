package com.peto.ramap.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.LocalAppTypography
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.search_bar_clear_action
import ramap.shared.generated.resources.search_bar_placeholder
import ramap.shared.generated.resources.search_bar_search_icon

private val SearchBarActionSize = 48.dp
private val SearchBarIconSize = 24.dp
private val SearchBarIconTextSpacing = 4.dp

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
    val shape = RoundedCornerShape(28.dp)

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = shape,
                    clip = false,
                ).clip(shape)
                .background(Color.White)
                .onFocusChanged { onFocusChanged(it.isFocused) },
        singleLine = true,
        textStyle = LocalAppTypography.current.b1.copy(color = GrayColor.C500),
        cursorBrush = SolidColor(GrayColor.C400),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions =
            KeyboardActions(
                onDone = {
                    onSearch()
                    focusManager.clearFocus()
                },
            ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchBarLeadingIcon(
                    isSearchMode = isSearchMode,
                    onBack = { focusManager.clearFocus() },
                )

                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(
                                top = 16.dp,
                                end = if (query.isEmpty()) 16.dp else 2.dp,
                                bottom = 16.dp,
                            ),
                    contentAlignment = Alignment.CenterStart,
                    propagateMinConstraints = true,
                ) {
                    if (query.isEmpty()) {
                        AppText(
                            text = placeholder,
                            style = AppTextStyle.B2,
                            color = GrayColor.C400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(SearchBarActionSize),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_close),
                            contentDescription = stringResource(Res.string.search_bar_clear_action),
                            tint = GrayColor.C400,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SearchBarLeadingIcon(
    isSearchMode: Boolean,
    onBack: () -> Unit,
) {
    val searchIconDescription = stringResource(Res.string.search_bar_search_icon)
    val iconOffset = (SearchBarActionSize - SearchBarIconSize) / 2 - SearchBarIconTextSpacing

    if (isSearchMode) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(SearchBarActionSize),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow3_left),
                contentDescription = searchIconDescription,
                modifier = Modifier.offset(x = iconOffset).size(SearchBarIconSize),
                tint = GrayColor.C500,
            )
        }
    } else {
        Box(
            modifier = Modifier.size(SearchBarActionSize),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = "⌕",
                style = AppTextStyle.H2,
                color = GrayColor.C500,
                modifier =
                    Modifier
                        .offset(x = iconOffset)
                        .width(SearchBarIconSize)
                        .semantics { contentDescription = searchIconDescription },
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
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
