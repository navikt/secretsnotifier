import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

val ktorVersion = "3.2.3"
val junitVersion = "5.13.4"
val logbackVersion = "1.5.18"
val logstashEncoderVersion = "8.1"

group = "no.nav"
version = "yolo"

plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation(platform("org.junit:junit-bom:$junitVersion")) // (5.12)
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    withType<Jar> {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "no.nav.MainKt"
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            showExceptions = true
            exceptionFormat = FULL
        }
    }

    withType<Wrapper> {
        gradleVersion = "9.0.0"
    }
}
