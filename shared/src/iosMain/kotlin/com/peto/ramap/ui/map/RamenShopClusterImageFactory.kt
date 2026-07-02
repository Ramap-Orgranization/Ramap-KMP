@file:OptIn(ExperimentalForeignApi::class)

package com.peto.ramap.ui.map

import com.peto.ramap.core.config.MarkerClusterConfig
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGContextSetShadowWithColor
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIBezierPath
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UILabel
import platform.UIKit.UIScreen

/**
 * iOS KakaoMap 클러스터 POI에 사용할 count UIImage를 생성하고 재사용한다.
 */
internal class RamenShopClusterImageFactory {
    private val cache = mutableMapOf<String, UIImage>()

    fun create(count: Int): UIImage {
        val text = MarkerClusterConfig.countText(count)

        return cache.getOrPut(text) {
            drawClusterImage(text)
        }
    }

    private fun drawClusterImage(text: String): UIImage {
        val scale = UIScreen.mainScreen.scale
        val bitmapSize = (MarkerConfig.Cluster.SIZE + SHADOW_PADDING * 2) / scale
        UIGraphicsBeginImageContextWithOptions(
            CGSizeMake(bitmapSize, bitmapSize),
            false,
            0.0,
        )

        val rect =
            CGRectMake(
                SHADOW_PADDING / scale,
                SHADOW_PADDING / scale,
                MarkerConfig.Cluster.SIZE / scale,
                MarkerConfig.Cluster.SIZE / scale,
            )

        drawShadow(rect, scale)
        drawBackground(rect, scale)
        drawText(text, rect)
        val image = UIGraphicsGetImageFromCurrentImageContext() ?: UIImage()
        UIGraphicsEndImageContext()

        return image
    }

    private fun drawShadow(
        rect: CValue<CGRect>,
        scale: Double,
    ) {
        val context = UIGraphicsGetCurrentContext() ?: return
        CGContextSetShadowWithColor(
            context,
            CGSizeMake(SHADOW_OFFSET / scale, SHADOW_OFFSET / scale),
            SHADOW_BLUR / scale,
            CLUSTER_SHADOW_COLOR.CGColor,
        )
        drawBackground(rect, scale)
    }

    private fun drawBackground(
        rect: CValue<CGRect>,
        scale: Double,
    ) {
        CLUSTER_BACKGROUND_COLOR.setFill()
        UIBezierPath
            .bezierPathWithRoundedRect(
                rect = rect,
                cornerRadius = CLUSTER_CORNER_RADIUS / scale,
            ).fill()
    }

    private fun drawText(
        text: String,
        rect: CValue<CGRect>,
    ) {
        UILabel(frame = rect).apply {
            this.text = text
            textColor = UIColor.whiteColor
            font = UIFont.boldSystemFontOfSize(IOS_CLUSTER_TEXT_SIZE)
            textAlignment = NSTextAlignmentCenter
            backgroundColor = UIColor.clearColor
            drawTextInRect(rect)
        }
    }

    private companion object {
        private const val SHADOW_PADDING = 3
        private const val SHADOW_OFFSET = 2.0
        private const val SHADOW_BLUR = 3.0
        private const val CLUSTER_CORNER_RADIUS = 15.0
        private const val IOS_CLUSTER_TEXT_SIZE = 13.0
        private val CLUSTER_BACKGROUND_COLOR = UIColor.colorWithRed(1.0, 0.337, 0.302, 1.0)
        private val CLUSTER_SHADOW_COLOR = UIColor.colorWithRed(0.0, 0.0, 0.0, 0.2)
    }
}
