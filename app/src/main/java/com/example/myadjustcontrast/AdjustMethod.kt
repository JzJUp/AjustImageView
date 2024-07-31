package com.example.myadjustcontrast

import android.graphics.ColorMatrix

internal fun adjustContrast(contrastValue: Int): ColorMatrix {
    val contrast = (1.0 + (contrastValue - 50) / 100.0).toFloat()
    val colorMatrix = ColorMatrix()
    colorMatrix.set(floatArrayOf(
        contrast, 0f, 0f, 0f, 0f,
        0f, contrast, 0f, 0f, 0f,
        0f, 0f, contrast, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))
    return colorMatrix
}

internal fun adjustBrightness(brightnessValue: Int): ColorMatrix {
    val brightness = brightnessValue - 50f // 将亮度值从0-100转换为-50到50
    val colorMatrix = ColorMatrix()
    // 亮度调整，如果亮度值为正，则增加亮度；如果为负，则减少亮度
    colorMatrix.set(floatArrayOf(
        1f, 0f, 0f, 0f, brightness, // Red
        0f, 1f, 0f, 0f, brightness, // Green
        0f, 0f, 1f, 0f, brightness, // Blue
        0f, 0f, 0f, 1f, 0f // Alpha
    ))
    return colorMatrix
}

internal fun adjustSaturability(saturabilityValue: Int): ColorMatrix {
    val saturationFactor = (saturabilityValue - 50).toFloat() / 50f
    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(1f + saturationFactor)
    return colorMatrix
}






