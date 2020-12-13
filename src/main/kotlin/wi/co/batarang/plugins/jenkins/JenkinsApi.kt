package wi.co.batarang.plugins.jenkins

import com.fasterxml.jackson.module.kotlin.readValue
import wi.co.batarang.mapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpClient.Version.HTTP_2
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.ofString
import java.time.Duration.ofSeconds
import java.util.Base64

data class Job(
    val name: String,
    val url: String,
    val color: String? = null
)

data class JobDetails(
    val url: String,
    val name: String,
    val description: String
)

data class ListAllRespnse(
    val jobs: List<Job>
)

class JenkinsApi(
    private val baseUrl: String,
    private val username: String,
    private val token: String
) {

    private val client = HttpClient.newBuilder()
        .connectTimeout(ofSeconds(5))
        .version(HTTP_2)
        .build()

    private fun listAll(): ListAllRespnse {
        val getRequest = mkGet("$baseUrl/api/json")
        val response = client.send(getRequest, ofString())
        return mapper.readValue(response.body())
    }

    fun listWithDescription(): List<JobDetails> {
        return listAll().jobs.map { job ->
            val detailsUrl = job.url + "/api/json"
            val request = mkGet(detailsUrl)
            val response = client.send(request, ofString())
            val details: JobDetails = mapper.readValue(response.body())
            details
        }
    }

    private fun mkGet(url: String): HttpRequest {
        return HttpRequest.newBuilder(URI(url))
            .GET()
            .header("Authorization", basicAuth())
            .build()
    }

    // https://stackoverflow.com/a/54208946
    private fun basicAuth(): String {
        return "Basic " + Base64.getEncoder().encodeToString("$username:$token".toByteArray())
    }
}
