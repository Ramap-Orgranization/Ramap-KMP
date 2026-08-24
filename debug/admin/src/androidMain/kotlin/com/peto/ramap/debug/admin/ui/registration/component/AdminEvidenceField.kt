package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.peto.ramap.debug.admin.data.model.AdminEvidence
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_camera_add
import ramap.shared.generated.resources.ic_close

@Composable
internal fun AdminEvidenceField(
    evidence: AdminEvidence?,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (evidence == null) {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GrayColor.C050)
                    .drawBehind {
                        val strokeWidth = 2.dp.toPx()
                        drawRoundRect(
                            color = GrayColor.C200,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            cornerRadius = CornerRadius(16.dp.toPx()),
                            style =
                                Stroke(
                                    width = strokeWidth,
                                    pathEffect =
                                        PathEffect.dashPathEffect(
                                            intervals = floatArrayOf(8.dp.toPx(), 4.dp.toPx()),
                                        ),
                                ),
                        )
                    }.clickable { onAddClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_camera_add),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = GrayColor.C400,
            )
        }
    } else {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f)
                    .clip(RoundedCornerShape(16.dp)),
        ) {
            AsyncImage(
                model = evidence.bytes,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            IconButton(
                onClick = onRemoveClick,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .size(24.dp)
                        .background(CommonColor.White, CircleShape),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_close),
                    contentDescription = null,
                    tint = GrayColor.C500,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminEvidenceFieldEmptyPreview() {
    RamapTheme {
        AdminEvidenceField(
            evidence = null,
            onAddClick = {},
            onRemoveClick = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminEvidenceFieldWithImagePreview() {
    RamapTheme {
        AdminEvidenceField(
            evidence = AdminEvidence(byteArrayOf(), "image/jpeg"),
            onAddClick = {},
            onRemoveClick = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
