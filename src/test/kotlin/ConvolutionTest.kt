package org.example

import org.example.include.*
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.awt.image.BufferedImage
import java.util.stream.Stream
import kotlin.random.Random
import kotlin.test.assertEquals


fun BufferedImage.toPixels(): IntArray {
    val pixels = IntArray(width * height)
    raster.getPixels(0, 0, width, height, pixels)
    return pixels
}

fun randomImage(width: Int, height: Int, rng: Random = Random.Default): IntArray =
    IntArray(width * height) { rng.nextInt(256) }

fun randomKernel(size: Int, rng: Random = Random.Default): Kernel {
    require(size % 2 == 1)
    val matrix = List(size) { List(size) { rng.nextFloat() * 2f - 1f } }
    return Kernel(matrix, factor = 1f, bias = 0f)
}

fun applyConvolution(width: Int, height: Int, pixels: IntArray, kernel: Kernel): IntArray =
    convolution(width, height, pixels, kernel).toPixels()

data class ImageKernelSize(val w: Int, val h: Int, val kSize: Int) {
    override fun toString() = "img=${w}x${h} kernel=${kSize}x${kSize}"
}

class ConvolutionTest {
    private fun generateRandomPixels(width: Int, height: Int): IntArray {
        return IntArray(width * height) { Random.nextInt(0, 256) }
    }

    private fun createIdentityKernel(size: Int): Kernel {
        val matrix = List(size) { x ->
            List(size) { y ->
                if (x == size / 2 && y == size / 2) 1.0f else 0.0f
            }
        }
        return Kernel(matrix, factor = 1.0f, bias = 0.0f)
    }

    private fun createZeroKernel(size: Int = 5): Kernel {
        val matrix = List(size) { List(size) { 0.0f } }
        return Kernel(matrix, factor = 1.0f, bias = 0.0f)
    }

    private fun padKernelWithZeros(kernel: Kernel, pad: Int = 1): Kernel {
        val oldSizeX = kernel.matrix.size
        val oldSizeY = kernel.matrix[0].size
        val newSizeX = oldSizeX + pad * 2
        val newSizeY = oldSizeY + pad * 2

        val newMatrix = List(newSizeX) { x ->
            List(newSizeY) { y ->
                if (x in pad until pad + oldSizeX && y in pad until pad + oldSizeY) {
                    kernel.matrix[x - pad][y - pad]
                } else {
                    0.0f
                }
            }
        }
        return Kernel(newMatrix, kernel.factor, kernel.bias)
    }

    private fun extractPixels(image: BufferedImage, width: Int, height: Int): IntArray {
        val result = IntArray(width * height)
        image.raster.getPixels(0, 0, width, height, result)
        return result
    }

    @Test
    fun `Identity kernel leaves image unchanged across different sizes`() {
        val sizes = listOf(10 to 10, 50 to 50, 100 to 20)
        val kernelSizes = listOf(3, 5, 7)

        for ((width, height) in sizes) {
            val originalPixels = generateRandomPixels(width, height)

            for (kSize in kernelSizes) {
                val kernel = createIdentityKernel(kSize)
                val resultImage = convolution(width, height, originalPixels, kernel)
                val resultPixels = extractPixels(resultImage, width, height)

                assertArrayEquals(
                    originalPixels,
                    resultPixels,
                    "Failed on image ${width}x${height} with kernel ${kSize}x${kSize}"
                )
            }
        }
    }

    @Test
    fun `Zero kernel produces black image`() {
        val width = 50
        val height = 50
        val originalPixels = generateRandomPixels(width, height)

        val kernel = createZeroKernel()
        val resultImage = convolution(width, height, originalPixels, kernel)
        val resultPixels = extractPixels(resultImage, width, height)

        assertTrue(resultPixels.all { it == 0 }, "All pixels should be 0")
    }

    @Test
    fun `Padding filter with zeros does not change result`() {
        val width = 40
        val height = 40
        val originalPixels = generateRandomPixels(width, height)

        val randomMatrix = List(3) { List(3) { Random.nextFloat() } }
        val baseKernel = Kernel(randomMatrix, factor = 1.0f, bias = 0.0f)

        val paddedKernel = padKernelWithZeros(baseKernel)

        val resultBase = extractPixels(convolution(width, height, originalPixels, baseKernel), width, height)
        val resultPadded = extractPixels(convolution(width, height, originalPixels, paddedKernel), width, height)

        assertArrayEquals(resultBase, resultPadded, "Padded kernel should yield identical results")
    }

    @Test
    fun `Composition of opposite shifts yields identity`() {
        val width = 30
        val height = 30
        val originalPixels = generateRandomPixels(width, height)

        val shiftLeftMatrix = List(3) { x -> List(3) { y -> if (x == 2 && y == 1) 1.0f else 0.0f } }
        val shiftLeftKernel = Kernel(shiftLeftMatrix, 1.0f, 0.0f)

        val shiftRightMatrix = List(3) { x -> List(3) { y -> if (x == 0 && y == 1) 1.0f else 0.0f } }
        val shiftRightKernel = Kernel(shiftRightMatrix, 1.0f, 0.0f)

        val intermediateImage = convolution(width, height, originalPixels, shiftLeftKernel)
        val intermediatePixels = extractPixels(intermediateImage, width, height)

        val finalImage = convolution(width, height, intermediatePixels, shiftRightKernel)
        val finalPixels = extractPixels(finalImage, width, height)

        assertArrayEquals(
            originalPixels,
            finalPixels,
            "Composition of Left and Right shift should equal original image"
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("imageSizes")
    fun `zero kernel with bias fills image with bias value`(p: ImageKernelSize) {
        val bias = 128f
        val pixels = randomImage(p.w, p.h)
        val matrix = List(p.kSize) { List(p.kSize) { 0f } }
        val kernel = Kernel(matrix, factor = 1f, bias = bias)

        val result = applyConvolution(p.w, p.h, pixels, kernel)

        val expected = IntArray(p.w * p.h) { bias.toInt() }
        assertArrayEquals(expected, result, "Zero weights + bias=$bias should fill image with $bias")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("imageSizes")
    fun `applying identity pipeline preserves image`(p: ImageKernelSize) {
        val pixels = randomImage(p.w, p.h)
        val matrix = List(p.kSize) { r ->
            List(p.kSize) { c ->
                if (r == p.kSize / 2 && c == p.kSize / 2) 1f else 0f
            }
        }
        val identity = Kernel(matrix, factor = 1f, bias = 0f)

        val after1 = applyConvolution(p.w, p.h, pixels, identity)
        val after2 = applyConvolution(p.w, p.h, after1, identity)
        val after3 = applyConvolution(p.w, p.h, after2, identity)

        assertArrayEquals(pixels, after3, "Chaining identity filters must preserve the image")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("imageSizes")
    fun `output pixels are always in 0-255`(p: ImageKernelSize) {
        val pixels = randomImage(p.w, p.h)

        val kernelHigh = Kernel(
            List(p.kSize) { List(p.kSize) { 1f } },
            factor = 100f, bias = 10000f
        )
        val kernelLow = Kernel(
            List(p.kSize) { List(p.kSize) { 1f } },
            factor = 100f, bias = -10000f
        )

        val resultHigh = applyConvolution(p.w, p.h, pixels, kernelHigh)
        val resultLow = applyConvolution(p.w, p.h, pixels, kernelLow)

        resultHigh.forEach { v -> assert(v in 0..255) { "Expected clamp to 255, got $v" } }
        resultLow.forEach { v -> assert(v in 0..255) { "Expected clamp to 0, got $v" } }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("imageSizes")
    fun `output size matches input size`(p: ImageKernelSize) {
        val pixels = randomImage(p.w, p.h)
        val kernel = randomKernel(p.kSize)

        val result = convolution(p.w, p.h, pixels, kernel)

        assertEquals(p.w, result.width, "Width must be preserved")
        assertEquals(p.h, result.height, "Height must be preserved")
    }

    @RepeatedTest(5)
    fun `parallel result matches sequential`() {
        val imageSizes = listOf(
            Triple(1, 1, 1),
            Triple(16, 16, 3),
            Triple(64, 64, 5),
            Triple(128, 64, 7),
        )
        val threadCounts = listOf(1, 2, 4)

        imageSizes.forEach { (w, h, kSize) ->
            val pixels = randomImage(w, h)
            val kernel = randomKernel(kSize)
            val expected = applyConvolution(w, h, pixels, kernel)

            threadCounts.forEach { threads ->
                SeparationMethods.entries.forEach { method ->
                    val actual = parallelConvolution(w, h, pixels, kernel, threads, method).toPixels()
                    assertArrayEquals(
                        expected, actual,
                        "Method=$method threads=$threads img=${w}x${h} kernel=${kSize}x${kSize}"
                    )
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun imageSizes(): Stream<ImageKernelSize> = Stream.of(
            ImageKernelSize(1, 1, 1),
            ImageKernelSize(3, 3, 1),
            ImageKernelSize(16, 16, 3),
            ImageKernelSize(5, 20, 3),
            ImageKernelSize(7, 7, 7),
            ImageKernelSize(64, 64, 9)
        )
    }
}
