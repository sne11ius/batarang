import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.lang.System.getenv

plugins {
    kotlin("jvm") version "1.4.20"
    id("org.mikeneck.graalvm-native-image") version "v0.9.1"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-core:2.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.0")
    //implementation("com.googlecode.lanterna:lanterna:3.0.4")
    implementation("com.googlecode.lanterna:lanterna:3.1.0-SNAPSHOT")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("batarang")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "wi.co.batarang.BatarangKt"))
        }
    }
}

nativeImage {
    graalVmHome = getenv("JAVA_HOME")
    mainClass ="wi.co.batarang.BatarangKt"
    executableName = "batarang"
    outputDirectory = File("$buildDir")
    arguments(
        "--no-fallback",
        "--enable-all-security-services",
        "--report-unsupported-elements-at-runtime",
        "--allow-incomplete-classpath"
    )
}
