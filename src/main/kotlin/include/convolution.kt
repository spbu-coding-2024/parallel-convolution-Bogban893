package org.example.include

import java.awt.image.BufferedImage

fun convolution(width: Int, height: Int, pixels: IntArray, kernel: Kernel): BufferedImage {
    val resultPixels = IntArray(width * height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            resultPixels[y * width + x] = convolvePixel(pixels, x, y, width, height, kernel)
        }
    }
    val res = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
    res.raster.setPixels(0, 0, width, height, resultPixels)
    return res
}
