import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.cyclonedx.gradle.CycloneDxTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

val ktorVersion = "2.3.9"

group = "no.nav"
version = "generatedlater"

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.cyclonedx.bom") version "1.8.2"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

kotlin {
    jvmToolchain(17)
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
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }

    withType<ShadowJar>{
        archiveFileName.set("app-all.jar")
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            showExceptions = true
        }
        testLogging {
            exceptionFormat = FULL
        }
    }

    withType<Wrapper> {
        gradleVersion = "8.3"
    }

    withType<CycloneDxTask> {
        setOutputFormat("json")
        setIncludeLicenseText(false)
    }
}
