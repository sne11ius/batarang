import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.lang.System.getenv
import org.gradle.api.JavaVersion.VERSION_11
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.js.dce.InputResource.Companion.file
import java.util.concurrent.TimeUnit.MINUTES

plugins {
    kotlin("jvm") version "1.4.21"
    id("org.mikeneck.graalvm-native-image") version "v0.9.1"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
    id("io.gitlab.arturbosch.detekt") version "1.14.2"
    id("at.zierler.yamlvalidator") version "1.5.0"
}

group = "wi.co"
version = "0.3d"
java.sourceCompatibility = VERSION_11

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-core:2.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.0")
    // implementation("com.googlecode.lanterna:lanterna:3.0.4")
    // implementation("com.googlecode.lanterna:lanterna:3.1.0-SNAPSHOT")
    // implementation("com.googlecode.lanterna:lanterna:3.2.0-alpha1")
    implementation("com.googlecode.lanterna:lanterna:3.2.0-SNAPSHOT")
    val isWindows = System.getProperty("os.name").toLowerCase().contains("windows")
    // TODO: Newest lanterna (3.2.0-alpha1) seems to support windows (if including the stuff below).
    //       see https://github.com/mabe02/lanterna/issues/422
    //       This version is missing the Listener in the textbox however and we really don't want
    //       to work around that.
    //       Solution: port necessary changes to the relevant version and make mabe release a new
    //       test version for us
    if (isWindows) {
        implementation("net.java.dev.jna:jna-platform:5.6.0")
    }
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
    graalVmHome = getenv("JAVA_HOME") ?: "Please set proper JAVA_HOME"
    mainClass = "wi.co.batarang.BatarangKt"
    executableName = "bat"
    outputDirectory = File("$buildDir")
    arguments(
        "--no-fallback",
        "--enable-all-security-services",
        "--report-unsupported-elements-at-runtime",
        "--allow-incomplete-classpath"
    )
}

detekt {
    failFast = true
    buildUponDefaultConfig = true
    config = files("$projectDir/detekt.yaml")
}

ktlint {
    disabledRules.set(setOf("import-ordering"))
    additionalEditorconfigFile.set(file(".editorconfig"))
}

tasks {
    build {
        dependsOn(
            "ktlintFormat"
        )
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = VERSION_11.toString()
    }
}

yamlValidator {
    searchPaths = listOf(".")
    setAllowDuplicates(false)
    setSearchRecursive(true)
}

task("install-pre-commit-hook") {
    doLast {
        val commitHookScript =
            """
            #!/bin/sh
            set -e
            ./gradlew validate-pre-commit
            """.trimIndent()
        val commitHookFile = File("$rootDir/.git/hooks/pre-commit")
        commitHookFile.writeText(commitHookScript)
        commitHookFile.setExecutable(true)
    }
}

task("validate-pre-commit") {
    dependsOn("validateYaml")
    doLast {
        val isWindows = System.getProperty("os.name").toLowerCase().contains("windows")
        val failWith: ((GradleException) -> Unit) = {
            throw it
        }

        val commandSuffix = if (isWindows) ".bat" else ""
        println("Running ktlintformat and detekt")
        "./gradlew$commandSuffix ktlintFormat detekt".runCommand(failWith = failWith)
        "git add -A".runCommand(failWith = failWith)
    }
}

fun String.runCommand(
    workingDirectory: File = File("."),
    additionalEnv: Map<String, String> = mapOf(),
    failWith: (GradleException) -> Unit
): String {
    val parts = this.split("\\s".toRegex())
        .map { it.replace("\\__", " ") }
    val builder = ProcessBuilder(parts)
        .directory(workingDirectory)
        .redirectErrorStream(true)
    builder.environment() += additionalEnv
    val proc = builder.start()
    var out = ""
    val bufferedReader = proc.inputStream.bufferedReader()
    while (true) {
        val c = bufferedReader.read()
        if (-1 == c)
            break
        val char = c.toChar()
        out += char
    }

    val completed = proc.waitFor(10, MINUTES)
    if (!completed || proc.exitValue() != 0) {
        println("Fehler beim Aufruf von '${parts.first()}'")
        println("Hier der output: $out")
        failWith(GradleException("Command nicht erfolgreich '${parts.first()}'"))
    } else {
        println("Command '${parts.first()}' wurde erfolgreich ausgeführt")
    }
    return out
}
