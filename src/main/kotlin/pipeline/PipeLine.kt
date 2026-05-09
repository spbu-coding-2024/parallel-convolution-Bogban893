package pipeline

import include.ImagePath
import include.ImageTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.example.include.SeparationMethods
import org.example.include.convolution
import org.example.include.parallelConvolution
import org.example.include.readKernel
import parallel.runParallel
import sequential.runSequential
import java.awt.Image
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
    val kernel = readKernel(File(kernelPath))
    val toConvolve = Channel<ImageTask>(capacity = thread)
    val toWrite = Channel<ImageTask>(capacity = thread)


    val executor = Executors.newFixedThreadPool(thread)
    val dispatcher = executor.asCoroutineDispatcher()

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

        val workers = (0 until thread).map {
            launch(dispatcher) {
                for (task in toConvolve) {
                    if (thread == 1) {
                        task.res = convolution(task.width, task.height, task.pixels, kernel)
                    } else {
                        task.res = parallelConvolution(task.width, task.height, task.pixels, kernel, thread, method)
                    }
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
        toWrite.close()
        writers.join()
    }

    dispatcher.close()
    executor.shutdown()

}