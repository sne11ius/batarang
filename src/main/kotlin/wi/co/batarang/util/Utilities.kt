package wi.co.batarang.util

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 * Wir müssen hier verschiedene Programme ausführen (bspw. git, npm). Die Java-
 * API dafür ist leider sehr umständlich, deswegegen, haben wir einen Wrapper
 * mit dem wir Strings als commands ausführen können.
 */
fun String.runCommand(workingDirectory: File = File("."), additionalEnv: Map<String, String> = mapOf()): String {
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

    val completed = proc.waitFor(10, TimeUnit.MINUTES)
    if (!completed || proc.exitValue() != 0) {
        println("Fehler beim Aufruf von '${parts.first()}'")
        println("Hier der output: $out")
        throw RuntimeException("Command nicht erfolgreich '${parts.first()}'")
    } else {
        println("Command '${parts.first()}' wurde erfolgreich ausgeführt")
    }
    return out
}

fun String.runBackground(workingDir: File = File("."), debug: Boolean = false): Process {
    val parts = this
        .split("\\s".toRegex())
        .map { it.replace("\\__", " ") }
    val process = ProcessBuilder(parts)
        .directory(workingDir)
        .redirectErrorStream(true)
        .start()
    val bufferedReader = process.inputStream.bufferedReader()
    thread {
        val prefix = "[background task ${abs(this.hashCode())}]  > "
        if (debug) {
            print(prefix)
        }
        while (true) {
            val c = bufferedReader.read()
            if (-1 == c)
                break
            if (debug) {
                val char = c.toChar()
                print(char)
                if ('\n' == char) {
                    print(prefix)
                }
            }
        }
    }
    return process
}
