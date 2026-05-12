package org.example.pipeline

import org.example.include.ImagePath
import org.example.include.ImageTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.example.include.SeparationMethods
import org.example.include.convolution
import org.example.include.parallelConvolutionSuspend
import org.example.include.readKernel
import java.io.File
import java.util.concurrent.Executors
import javax.imageio.ImageIO

fun runPipeline(
    imagePath: List<File>,
    kernelPath: String,
    outputPath: List<File>,
    thread: Int,
    method: SeparationMethods
) {

    val start = System.nanoTime()
    val kernel = readKernel(File(kernelPath))
    val toConvolve = Channel<ImageTask>(capacity = thread)
    val toWrite = Channel<ImageTask>(capacity = thread)


    runBlocking {
        val reader = launch(Dispatchers.IO) {
            for ((input, output) in imagePath.zip(outputPath)) {
                val image = ImageIO.read(input)
                val pixels = IntArray(image.width * image.height)
                image.raster.getPixels(0, 0, image.width, image.height, pixels)
                val task = ImageTask(ImagePath(input, output), pixels, image.width, image.height, kernel)
                toConvolve.send(task)
            }
            toConvolve.close()
        }

        val executor = Executors.newFixedThreadPool(thread)
        val cpuDispatcher = executor.asCoroutineDispatcher()
        val workers = (0 until thread).map {
            launch(cpuDispatcher) {
                for (task in toConvolve) {
                    task.res = if (thread <= 1)
                        convolution(task.width, task.height, task.pixels, kernel) // Подумать
                    else
                        parallelConvolutionSuspend(
                            task.width,
                            task.height,
                            task.pixels,
                            kernel,
                            thread,
                            method,
                            cpuDispatcher
                        )
                    toWrite.send(task)
                }
            }
        }

        val writers = launch(Dispatchers.IO) {
            for (task in toWrite) {
                ImageIO.write(task.res, task.path.input.extension, task.path.output)
            }
        }

        reader.join()
        workers.joinAll()
        cpuDispatcher.close()
        executor.shutdown()
        toWrite.close()
        writers.join()
    }


    val elapsed = System.nanoTime() - start
    println("Pipeline [${imagePath.size} images, threads: $thread]: ${elapsed / 1_000_000} ms")
}