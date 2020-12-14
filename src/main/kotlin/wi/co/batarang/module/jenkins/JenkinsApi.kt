package wi.co.batarang.module.jenkins

import com.fasterxml.jackson.module.kotlin.readValue
import wi.co.batarang.util.httpClient
import wi.co.batarang.util.mapper
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.ofString
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

    private fun listAll(): ListAllRespnse {
        val getRequest = mkGet("$baseUrl/api/json")
        val response = httpClient.send(getRequest, ofString())
        return mapper.readValue(response.body())
    }

    fun listWithDescription(): List<JobDetails> {
        return listAll().jobs.map { job ->
            val detailsUrl = job.url + "/api/json"
            val request = mkGet(detailsUrl)
            val response = httpClient.send(request, ofString())
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
