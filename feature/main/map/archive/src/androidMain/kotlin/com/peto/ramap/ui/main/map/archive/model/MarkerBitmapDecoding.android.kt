package com.peto.ramap.ui.main.map.archive.model

import coil3.request.ImageRequest
import coil3.request.allowHardware

internal actual fun ImageRequest.Builder.configureMarkerBitmapDecoding(): ImageRequest.Builder = allowHardware(enable = false)
