package wi.co.batarang.plugins.jenkins

import com.fasterxml.jackson.module.kotlin.readValue
import wi.co.batarang.Action
import wi.co.batarang.Setting
import wi.co.batarang.SettingKey
import wi.co.batarang.mapper
import wi.co.batarang.plugins.Plugin
import wi.co.batarang.util.runBackground
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpClient.Version.HTTP_2
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers.noBody
import java.net.http.HttpResponse.BodyHandlers.ofString
import java.time.Duration.ofSeconds
import java.util.Base64

object JenkinsPlugin : Plugin {

    private val httpUrlKey = SettingKey("Jenkins-URL (bspw. 'https://myhost.com/jenkins')", "http-url")
    private val usernameKey = SettingKey("Jenkins username", "username")
    private val tokenKey = SettingKey("Jenkins API-Token", "token")

    override val requiredSettings: List<SettingKey> = listOf(httpUrlKey, usernameKey, tokenKey)

    private var jobs: List<JobDetails> = emptyList()

    private val client = HttpClient.newBuilder()
        .connectTimeout(ofSeconds(5))
        .version(HTTP_2)
        .build()

    override fun setData(data: String) {
        jobs = mapper.readValue(data)
    }

    override fun updateData(settings: List<Setting>): String {
        val httpBaseUrl = settings.first { it.key == httpUrlKey }.value
        val username = settings.first { it.key == usernameKey }.value
        val token = settings.first { it.key == tokenKey }.value

        val api = JenkinsApi(httpBaseUrl, username, token)
        return mapper.writeValueAsString(api.listWithDescription())
    }

    override fun getActions(settings: List<Setting>): List<Action> {
        val username = settings.first { it.key == usernameKey }.value
        val token = settings.first { it.key == tokenKey }.value

        // https://stackoverflow.com/a/54208946
        fun basicAuth(): String {
            return "Basic " + Base64.getEncoder().encodeToString("$username:$token".toByteArray())
        }

        fun mkPost(url: String): HttpRequest {
            return HttpRequest.newBuilder(URI(url))
                .POST(noBody())
                .header("Authorization", basicAuth())
                .build()
        }

        fun mkGet(url: String): HttpRequest {
            return HttpRequest.newBuilder(URI(url))
                .GET()
                .header("Authorization", basicAuth())
                .build()
        }

        return jobs.flatMap { job ->
            listOf(
                Action(
                    label = "[jenkins] BROWSE ${job.name}",
                    strings = listOf("jenkins", "browse", job.name, job.description, job.url)
                ) {
                    "xdg-open ${job.url}".runBackground()
                },
                Action(
                    label = "[jenkins] RUN ${job.name}",
                    strings = listOf("jenkins", "run", job.name, job.description, job.url)
                ) {
                    data class CrumbResponse(
                        val crumb: String
                    )
                    val request = mkPost(job.url + "/build")
                    client.send(request, ofString())
                }
            )
        }
    }

}
