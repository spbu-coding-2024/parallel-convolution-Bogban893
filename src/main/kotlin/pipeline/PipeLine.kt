package org.example.pipeline

import org.example.include.ImagePath
import org.example.include.ImageTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.example.include.ImageResult
import org.example.include.SeparationMethods
import org.example.include.parallelConvolutionSuspend
import org.example.include.readKernel
import org.example.include.toGrayscale
import java.io.File
import java.util.concurrent.Executors
import javax.imageio.ImageIO

fun runPipeline(
    imagePaths: List<File>,
    kernelPath: String,
    outputPaths: List<File>,
    thread: Int,
    method: SeparationMethods
) {
    require(imagePaths.size == outputPaths.size) {
        "Input and output lists must have the same size"
    }

    val writerCount = maxOf(1, thread / 4)
    val kernel = readKernel(File(kernelPath))
    val toConvolve = Channel<ImageTask>(capacity = thread)
    val toWrite    = Channel<ImageResult>(capacity = writerCount)

    val cpuExecutor   = Executors.newFixedThreadPool(thread)
    val cpuDispatcher = cpuExecutor.asCoroutineDispatcher()

    val start = System.nanoTime()

    runBlocking {
        val reader = launch(Dispatchers.IO) {
            for ((input, output) in imagePaths.zip(outputPaths)) {
                val image  = toGrayscale(ImageIO.read(input))   // fixed: always grayscale
                val pixels = IntArray(image.width * image.height)
                image.raster.getPixels(0, 0, image.width, image.height, pixels)

                toConvolve.send(
                    ImageTask(
                        path   = ImagePath(input, output),
                        pixels = pixels,
                        width  = image.width,
                        height = image.height,
                        kernel = kernel
                    )
                )
            }
            toConvolve.close()
        }
        val worker = launch(Dispatchers.Default) {
            for (task in toConvolve) {
                val result = parallelConvolutionSuspend(
                    width      = task.width,
                    height     = task.height,
                    pixels     = task.pixels,
                    kernel     = task.kernel,
                    thread     = thread,
                    methods    = method,
                    dispatcher = cpuDispatcher
                )
                toWrite.send(ImageResult(task.path, result))
            }
        }

        val writers = (0 until writerCount).map {
            launch(Dispatchers.IO) {
                for (result in toWrite) {
                    ImageIO.write(
                        result.image,
                        result.path.input.extension,
                        result.path.output
                    )
                }
            }
        }

        reader.join()
        worker.join()
        toWrite.close()
        cpuDispatcher.close()
        cpuExecutor.shutdown()
        writers.joinAll()
    }

    val elapsed = System.nanoTime() - start
    println(
        "Pipeline [${imagePaths.size} images, threads: $thread, " +
                "writers: $writerCount, method: $method]: ${elapsed / 1_000_000} ms"
    )
}
