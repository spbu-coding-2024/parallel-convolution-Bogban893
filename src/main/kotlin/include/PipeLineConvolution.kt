package include

import org.example.include.Kernel
import org.example.include.SeparationMethods
import java.awt.image.BufferedImage
import java.io.DataInput
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
    var res: BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY),

    var readOk: Boolean = false,
    var convolutionOk: Boolean = false,
    var writeOk: Boolean = false
)

