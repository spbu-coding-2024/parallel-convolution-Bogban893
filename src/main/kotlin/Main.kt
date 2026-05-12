package org.example

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.multiple
import kotlinx.cli.required
import org.example.include.SeparationMethods
import org.example.parallel.runParallel
import org.example.pipeline.runPipeline
import org.example.sequential.runSequential
import java.io.File

enum class Mode {
    SEQUENTIAL {
        override fun toString() = "seq"
    },
    PARALLEL {
        override fun toString() = "par"
    },
    PIPE_LINE {
        override fun toString() = "pipe"
    }
}

object ArgTypeFile : ArgType<File>(true) {
    override val description = "File path"
    override fun convert(value: kotlin.String, name: kotlin.String): File =
        File(value).also {
            require(it.exists()) { "File not found: $it" }
        }


}

fun main(args: Array<String>) {
    val parser = ArgParser("convolution")

    val mode by parser.option(
        ArgType.Choice<Mode>(),
        shortName = "m",
        description = "Processing mode"
    ).required()

    val image by parser.option(
        ArgTypeFile,
        shortName = "i",
        description = "Input image path"
    ).multiple()

    val kernel by parser.option(
        ArgType.String,
        shortName = "k",
        description = "Kernel file path"
    ).required()

    val output by parser.option(
        ArgType.String,
        shortName = "o",
        description = "Output image path",
        fullName = "output"
    ).default("output.png")

    val thread by parser.option(
        ArgType.Int,
        shortName = "t",
        description = "Thread count",
    ).default(2)

    val methods by parser.option(
        ArgType.Choice<SeparationMethods>(),
        shortName = "s",
        description = "Input separation methods"
    ).default(SeparationMethods.ROW_BY_ROW)

    parser.parse(args)

//    image = image.map { }

    val inputFiles: List<File> = when {
        image.isEmpty() -> error("No input image file")
        image.size == 1 && image.first().isDirectory ->
            image.first()
                .listFiles { f -> f.extension.lowercase() in listOf("png", "jpg", "jpeg") }!!
                .sorted()

        else -> image.map { path ->
            path.also { if (!it.isFile) error("File not found: $path") }
        }
    }
    when (mode) {
        Mode.SEQUENTIAL, Mode.PARALLEL -> {
            if (inputFiles.size > 1)
                println("WARNING: directory given in non-pipeline mode, using only first file: ${inputFiles.first().name}")
            val output = if (File(output).isDirectory) File(
                output,
                "${image.first().nameWithoutExtension}_out.${image.first().extension}"
            ).toString() else output
            if (mode == Mode.SEQUENTIAL) {
                runSequential(image.first(), kernel, output)
            } else {
                runParallel(image.first(), kernel, output, thread, methods)
            }
        }

        Mode.PIPE_LINE -> {
            val output = inputFiles.map { file ->
                File(file.parentFile, file.nameWithoutExtension + "_out." + file.extension)
            }
            runPipeline(inputFiles, kernel, output, thread, methods)
        }
    }
}
