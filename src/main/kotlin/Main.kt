package org.example

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import org.example.include.SeparationMethods
import parallel.runParallel
import sequential.runSequential

enum class Mode { SEQUENTIAL, PARALLEL }

fun main(args: Array<String>) {
    val parser = ArgParser("convolution")

    val mode by parser.option(
        ArgType.Choice<Mode>(),
        shortName = "m",
        description = "Processing mode"
    ).required()

    val image by parser.option(
        ArgType.String,
        shortName = "i",
        description = "Input image path"
    ).required()

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

    when (mode) {
        Mode.SEQUENTIAL -> runSequential(image, kernel, output)
        Mode.PARALLEL -> runParallel(image, kernel, output, thread, methods)
    }
}
