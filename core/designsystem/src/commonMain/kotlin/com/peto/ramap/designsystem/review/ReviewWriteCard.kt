package com.peto.ramap.designsystem.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_profile_edit
import ramap.shared.generated.resources.shop_review_prompt_title
import ramap.shared.generated.resources.shop_review_write_action

private val ReviewWriteCardBackgroundColor = ChromaticColor.Cream400
private val ReviewWriteCardWatermarkColor = ChromaticColor.Apricot400.copy(alpha = 0.5f)

@Composable
fun ReviewWriteCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(size = 16.dp))
                .background(
                    color = ReviewWriteCardBackgroundColor,
                    shape = RoundedCornerShape(size = 16.dp),
                ),
    ) {
        Image(
            painter = painterResource(resource = Res.drawable.ic_profile_edit),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color = ReviewWriteCardWatermarkColor),
            modifier =
                Modifier
                    .align(alignment = Alignment.BottomEnd)
                    .offset(x = 12.dp, y = 12.dp)
                    .size(size = 72.dp)
                    .graphicsLayer { rotationZ = 15f },
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 20.dp,
                        top = 20.dp,
                        end = 20.dp,
                        bottom = 16.dp,
                    ),
            verticalArrangement = Arrangement.spacedBy(space = 14.dp),
        ) {
            AppText(
                text = stringResource(resource = Res.string.shop_review_prompt_title),
                style = AppTextStyle.T2,
                color = GrayColor.C500,
            )

            Surface(
                onClick = onClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(height = 48.dp),
                shape = RoundedCornerShape(size = 100.dp),
                color = GrayColor.C500,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Image(
                            painter = painterResource(resource = Res.drawable.ic_profile_edit),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(color = InstagramColor.Orange),
                            modifier = Modifier.size(size = 18.dp),
                        )

                        Spacer(modifier = Modifier.width(width = 6.dp))

                        AppText(
                            text = stringResource(resource = Res.string.shop_review_write_action),
                            style = AppTextStyle.T3,
                            color = CommonColor.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewWriteCardPreview() {
    RamapTheme {
        Box(modifier = Modifier.padding(paddingValues = PaddingValues(all = 16.dp))) {
            ReviewWriteCard(onClick = {})
        }
    }
}
