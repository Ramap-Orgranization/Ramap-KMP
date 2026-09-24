package com.peto.ramap.designsystem.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.profile_pixel_ramen

@Composable
fun ProfileAvatar(
    model: Any?,
    description: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(30.dp)
    val fallback = painterResource(Res.drawable.profile_pixel_ramen)
    Box(modifier.clip(shape).background(Color(0xFFF1E9DB)).border(1.dp, Color(0xFFE5DACA), shape)) {
        if (model == null) {
            Image(fallback, description, Modifier.fillMaxSize().padding(13.dp))
        } else {
            AsyncImage(model, description, Modifier.fillMaxSize(), placeholder = fallback, error = fallback, contentScale = ContentScale.Crop)
        }
    }
}
