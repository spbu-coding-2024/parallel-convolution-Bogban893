package parallel

import org.example.include.SeparationMethods
import org.example.include.readKernel
import org.example.include.toGrayscale
import java.io.File
import javax.imageio.ImageIO
import org.example.include.parallelConvolution
import java.awt.image.BufferedImage


fun runParallel(imagePath: String, kernelPath: String, outputPath: String, thread: Int) {
    val image = toGrayscale(ImageIO.read(File(imagePath)))
    val kernel = readKernel(File(kernelPath))
    val width = image.width
    val height = image.height
    println("Parallel [${width}x${height}, kernel ${kernel.matrix.size}x${kernel.matrix[0].size}, threads: $thread, factor ${kernel.factor}, bias ${kernel.bias}]")
    println("-".repeat(70))

    val pixels = IntArray(image.width * image.height)
    image.raster.getPixels(0, 0, width, height, pixels)

    var lastResult: BufferedImage? = null
    SeparationMethods.entries.forEach { method ->

        val start = System.nanoTime()
        val result = parallelConvolution(width, height, pixels, kernel, thread, method)
        val elapsed = System.nanoTime() - start

        println("%-20s %6d ms".format(method.name, elapsed / 1_000_000))
        lastResult = result
    }

    println("-".repeat(70))
    ImageIO.write(lastResult!!, "png", File(outputPath))
}
