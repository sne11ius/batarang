package wi.co.batarang.util

import java.io.File
import java.net.http.HttpClient
import java.net.http.HttpClient.Version.HTTP_2
import java.time.Duration.ofSeconds
import kotlin.concurrent.thread
import kotlin.math.abs

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

private const val HTTP_TIMEOUT_IN_SECONDS = 5L

val httpClient: HttpClient = HttpClient.newBuilder()
    .connectTimeout(ofSeconds(HTTP_TIMEOUT_IN_SECONDS))
    .version(HTTP_2)
    .build()
