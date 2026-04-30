package org.example.include

import java.awt.image.BufferedImage
import java.io.File


data class Kernel(
    val matrix: List<List<Float>>,
    val factor: Float,
    val bias: Float
)

enum class SeparationMethods {
    PIXEL_BY_PIXEL,
    ROW_BY_ROW,
    COLUMN_BY_COLUMN,
    GRID
}

fun readKernel(file: File): Kernel {
    val lines = file.readLines().filter { it.isNotBlank() }
    val (rows, cols) = lines[0].split("\\s+".toRegex()).map { it.toInt() }
    require(rows % 2 != 0 && cols % 2 != 0) { "Kernel dimensions must be odd, got ${rows}x${cols}" }
    require(lines.size >= rows + 1) { "File has fewer rows than declared" }

    val matrix = Array(rows) { i ->
        lines[i + 1].trim().split("\\s+".toRegex()).map { it.toFloat() }
    }.map { it.toList() }

    val (factor, bias) = if (lines.size > rows + 1)
        lines[rows + 1].trim().split("\\s+".toRegex()).map { it.toFloat() }
    else listOf(1.0f, 0.0f)

    return Kernel(matrix, factor, bias)
}

fun toGrayscale(image: BufferedImage): BufferedImage {
    if (image.type == BufferedImage.TYPE_BYTE_GRAY) return image
    val gray = BufferedImage(image.width, image.height, BufferedImage.TYPE_BYTE_GRAY)
    val g = gray.createGraphics()
    g.drawImage(image, 0, 0, null)
    g.dispose()
    return gray
}

fun convolvePixel(pixels: IntArray, x: Int, y: Int, width: Int, height: Int, kernel: Kernel): Int {
    val kCenterX = kernel.matrix.size / 2
    val kCenterY = kernel.matrix[0].size / 2
    var sum = 0f
    for (filterX in kernel.matrix.indices) {
        for (filterY in kernel.matrix[filterX].indices) {
            val imageX = (x - kCenterX + filterX + width) % width
            val imageY = (y - kCenterY + filterY + height) % height
            sum += pixels[imageY * width + imageX] * kernel.matrix[filterX][filterY]
        }
    }
    return (sum * kernel.factor + kernel.bias).coerceIn(0f, 255f).toInt()
}
