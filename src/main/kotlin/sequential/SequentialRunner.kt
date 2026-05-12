package org.example.sequential

import org.example.include.*
import java.io.File
import javax.imageio.ImageIO

fun runSequential(imagePath: File, kernelPath: String, outputPath: String) {
    val image = toGrayscale(ImageIO.read(imagePath))
    val kernel = readKernel(File(kernelPath))
    val width = image.width
    val height = image.height

    val pixels = IntArray(image.width * image.height)
    image.raster.getPixels(0, 0, width, height, pixels)

    println("Sequential [${width}x${height}, kernel ${kernel.matrix.size}x${kernel.matrix[0].size}, factor ${kernel.factor}, bias ${kernel.bias}]")
    println("-".repeat(60))

    val start = System.nanoTime()
    val result = convolution(width, height, pixels, kernel)
    val elapsed = System.nanoTime() - start

    ImageIO.write(result, imagePath.extension, File(outputPath))
    println("%-20s %6d ms".format("Time:", elapsed / 1_000_000))
    println("-".repeat(60))
}
