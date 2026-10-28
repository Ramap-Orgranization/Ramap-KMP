@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.peto.ramap.ui.main.map.archive

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIScreen

/** Convert the common Compose marker raster to the native map SDK image. */
internal fun markerUIImage(bitmap: ImageBitmap): UIImage? {
    val bytes = Image.makeFromBitmap(bitmap.asSkiaBitmap()).encodeToData()?.bytes ?: return null
    return bytes.usePinned { pinned ->
        UIImage.imageWithData(NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong()), scale = UIScreen.mainScreen.scale)
    }
}
