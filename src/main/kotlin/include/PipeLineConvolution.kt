package org.example.include

import java.awt.image.BufferedImage
import java.io.File

data class ImagePath(
    val input: File,
    val output: File,
)

data class ImageTask(
    val path: ImagePath,
    val pixels: IntArray,
    val width: Int,
    val height: Int,
    val kernel: Kernel,
)

data class ImageResult(
    val path: ImagePath,
    val image: BufferedImage,
)
