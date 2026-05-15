package org.example.include

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.util.concurrent.Executors
import kotlin.math.sqrt

private fun processRegion(
    startX: Int, endX: Int,
    startY: Int, endY: Int,
    width: Int,
    height: Int,
    pixels: IntArray,
    kernel: Kernel,
    resultPixels: IntArray
) {
    for (y in startY until endY) {
        for (x in startX until endX) {
            resultPixels[y * width + x] = convolvePixel(pixels, x, y, width, height, kernel)
        }
    }
}

fun parallelConvolution(
    width: Int,
    height: Int,
    pixels: IntArray,
    kernel: Kernel,
    thread: Int,
    methods: SeparationMethods
): BufferedImage {
    val executor = Executors.newFixedThreadPool(thread)
    val dispatcher = executor.asCoroutineDispatcher()

    return try {
        runBlocking {
            parallelConvolutionSuspend(width, height, pixels, kernel, thread, methods, dispatcher)
        }
    } finally {
        dispatcher.close()
        executor.shutdown()
    }
}

suspend fun parallelConvolutionSuspend(
    width: Int, height: Int,
    pixels: IntArray, kernel: Kernel,
    thread: Int, methods: SeparationMethods,
    dispatcher: CoroutineDispatcher
): BufferedImage {
    val resultPixels = IntArray(width * height)

    coroutineScope {
        when (methods) {
            SeparationMethods.ROW_BY_ROW -> {
                val chunk = height / thread

                (0 until thread).map { t ->
                    launch(dispatcher) {
                        val startY = t * chunk
                        val endY = if (t == thread - 1) height else startY + chunk
                        processRegion(0, width, startY, endY, width, height, pixels, kernel, resultPixels)
                    }
                }.joinAll()
            }

            SeparationMethods.COLUMN_BY_COLUMN -> {
                val chunk = width / thread

                (0 until thread).map { t ->
                    launch(dispatcher) {
                        val startX = t * chunk
                        val endX = if (t == thread - 1) width else startX + chunk
                        processRegion(startX, endX, 0, height, width, height, pixels, kernel, resultPixels)
                    }
                }.joinAll()
            }

            SeparationMethods.PIXEL_BY_PIXEL -> {
                val totalPixels = width * height
                val chunk = (totalPixels + thread - 1) / thread

                (0 until thread).map { t ->
                    launch(dispatcher) {
                        val start = t * chunk
                        val end = minOf(start + chunk, totalPixels)
                        for (idx in start until end) {
                            val x = idx % width
                            val y = idx / width
                            resultPixels[idx] = convolvePixel(pixels, x, y, width, height, kernel)
                        }
                    }
                }.joinAll()
            }

            SeparationMethods.GRID -> {
                val tileRows = bestRowCount(thread)
                val tileCols = thread / tileRows
                val tileH = height / tileRows
                val tileW = width / tileCols

                (0 until tileRows).flatMap { row -> (0 until tileCols).map { col -> Pair(row, col) } }
                    .map { (row, col) ->
                        launch(dispatcher) {
                            val startY = row * tileH
                            val endY = if (row == tileRows - 1) height else startY + tileH
                            val startX = col * tileW
                            val endX = if (col == tileCols - 1) width else startX + tileW
                            for (y in startY until endY)
                                for (x in startX until endX)
                                    resultPixels[
                                        y * width + x] =
                                        convolvePixel(pixels, x, y, width, height, kernel)

                        }
                    }.joinAll()
            }

        }
    }
    val res = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
    res.raster.setPixels(0, 0, width, height, resultPixels)
    return res
}

private fun bestRowCount(thread: Int): Int {
    require(thread >= 1) { "Thread count must be >= 1" }
    val sqr = sqrt(thread.toDouble()).toInt()
    for (r in sqr downTo 1) {
        if (thread % r == 0) return r
    }
    return 1
}
