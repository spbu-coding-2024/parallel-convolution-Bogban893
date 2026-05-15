package org.example.benchmark

import org.example.include.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)

open class ConvolutionBenchmark {
    @Param("512", "1024", "2048")
    var imageSize: Int = 512

    @Param("2", "4", "8", "16")
    var threads: Int = 4

    private lateinit var pixels: IntArray
    private lateinit var kernel: Kernel
    private var width = 0
    private var height = 0

    @Setup(Level.Trial)
    fun setup() {
        width = imageSize
        height = imageSize
        pixels = IntArray(width * height) { (it % 256) }
        val matrix = List(5) { FloatArray(5) { 1f / 25f } }.map { it.toList() }
        kernel = Kernel(matrix, factor = 1f, bias = 0f)
    }

    @Benchmark
    fun sequential(): Int {
        val result = convolution(width, height, pixels, kernel)
        return result.width
    }

    @Benchmark
    fun parallelRowByRow(): Int {
        val result = parallelConvolution(width, height, pixels, kernel, threads, SeparationMethods.ROW_BY_ROW)
        return result.width
    }

    @Benchmark
    fun parallelColumnByColumn(): Int {
        val result = parallelConvolution(width, height, pixels, kernel, threads, SeparationMethods.COLUMN_BY_COLUMN)
        return result.width
    }

    @Benchmark
    fun parallelGrid(): Int {
        val result = parallelConvolution(width, height, pixels, kernel, threads, SeparationMethods.GRID)
        return result.width
    }

    @Benchmark
    fun parallelPixelByPixel(): Int {
        val result = parallelConvolution(width, height, pixels, kernel, threads, SeparationMethods.PIXEL_BY_PIXEL)
        return result.width
    }
}
