plugins {
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.12"
    kotlin("plugin.allopen") version "2.1.20"
    application
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

application {
    mainClass = "org.example.MainKt"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.12")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}


benchmark {
    configurations {
        named("main") {
            warmups = 5
            iterations = 10
            iterationTime = 2
            mode = "AverageTime"
            outputTimeUnit = "MILLISECONDS"
        }
    }
    targets {
        register("main") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = "1.37"
        }
    }
}

