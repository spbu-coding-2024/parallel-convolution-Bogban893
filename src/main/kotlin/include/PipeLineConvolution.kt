package org.example.include

import java.awt.image.BufferedImage
import java.io.File

data class ImagePath(
    val input: File,
    val output: File,
)

class ImageTask(
    val path: ImagePath,
    val pixels: IntArray,
    val width: Int,
    val height: Int,
    val kernel: Kernel,

    var readOk: Boolean = false,
    var convolutionOk: Boolean = false,
    var writeOk: Boolean = false
) {
    var res: BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)

}

