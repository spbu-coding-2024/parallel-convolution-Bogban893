package org.example

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import sequential.runSequential

fun main(args: Array<String>) {
    val parser = ArgParser("convolution")

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
    parser.parse(args)

    runSequential(image, kernel, output)
}
