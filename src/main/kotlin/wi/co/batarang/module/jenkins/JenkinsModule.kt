package wi.co.batarang.module.jenkins

import com.fasterxml.jackson.module.kotlin.readValue
import wi.co.batarang.Action
import wi.co.batarang.Setting
import wi.co.batarang.SettingKey
import wi.co.batarang.module.Module
import wi.co.batarang.module.launcher.LauncherModule.launch
import wi.co.batarang.util.httpClient
import wi.co.batarang.util.mapper
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers.noBody
import java.net.http.HttpResponse.BodyHandlers.ofString
import java.util.Base64

object JenkinsModule : Module {

    private lateinit var settings: List<Setting>
    private val httpUrlKey = SettingKey("Jenkins-URL (e.g. 'https://myhost.com/jenkins')", "http-url")
    private val usernameKey = SettingKey("Jenkins username", "username")
    private val tokenKey = SettingKey("Jenkins API-Token", "token")

    override val requiredSettings: List<SettingKey> = listOf(httpUrlKey, usernameKey, tokenKey)

    override val canBeActivated = true

    private var jobs: List<JobDetails> = emptyList()

    override fun setData(data: String) {
        jobs = mapper.readValue(data)
    }

    override fun updateData(): String {
        val httpBaseUrl = settings.first { it.key == httpUrlKey }.value
        val username = settings.first { it.key == usernameKey }.value
        val token = settings.first { it.key == tokenKey }.value

        val api = JenkinsApi(httpBaseUrl, username, token)
        return mapper.writeValueAsString(api.listWithDescription())
    }

    override fun getActions(): List<Action> {
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

        return jobs.flatMap { job ->
            listOf(
                Action(
                    label = "[jenkins] BROWSE ${job.name}",
                    tags = listOf("jenkins", "browse", job.name, job.description, job.url)
                ) {
                    launch(job.url)
                },
                Action(
                    label = "[jenkins] RUN ${job.name}",
                    tags = listOf("jenkins", "run", job.name, job.description, job.url)
                ) {
                    val request = mkPost(job.url + "/build")
                    httpClient.send(request, ofString())
                }
            )
        }
    }

    override fun updateSettings(settings: List<Setting>) {
        this.settings = settings
    }
}
