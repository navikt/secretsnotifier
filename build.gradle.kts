plugins {
    kotlin("multiplatform") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
}

repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        val ktorVersion = "2.3.3"

        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
            }
        }
    }

    val hostOs = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")
    when {
        hostOs == "Mac OS X" && arch == "x86_64" -> macosX64("native")
        hostOs == "Mac OS X" && arch == "aarch64" -> macosArm64("native")
        hostOs == "Linux" -> linuxX64("native")
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native.")
    }.apply {
        binaries {
            executable()
        }
    }

}

tasks.withType<Wrapper> {
    gradleVersion = "8.3"
    distributionType = Wrapper.DistributionType.BIN
}
