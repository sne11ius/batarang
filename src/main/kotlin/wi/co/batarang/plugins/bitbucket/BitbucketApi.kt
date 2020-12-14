package wi.co.batarang.plugins.bitbucket

import com.fasterxml.jackson.module.kotlin.readValue
import wi.co.batarang.util.httpClient
import wi.co.batarang.util.mapper
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.ofString
import java.util.Base64

data class Href(
    val href: String
)

data class ProjectLinksResponse(
    val self: List<Href>
)

data class ProjectResponse(
    val key: String,
    val id: Int,
    val name: String,
    val links: ProjectLinksResponse
) {
    fun repositoriesLink(baseUrl: String): String {
        return "$baseUrl/rest/api/1.0/projects/$key/repos?limit=1000"
    }
}

data class ProjectsListResponse(
    val values: List<ProjectResponse>
)

data class RepositoryResponse(
    val slug: String,
    val id: Int,
    val name: String
) {
    fun mkRepository(project: ProjectResponse): Repository {
        return Repository(
            projectKey = project.key,
            slug = slug,
            name = name
        )
    }
}

data class RepositoriesListResponse(
    val values: List<RepositoryResponse>
)

data class Repository(
    val projectKey: String,
    val slug: String,
    val name: String
) {
    fun readmeLink(baseUrl: String): String {
        return "$baseUrl/rest/api/1.0/projects/$projectKey/repos/$slug/browse/README.md"
    }
    fun withReadme(readme: String?): RepositoryWithReadme {
        return RepositoryWithReadme(
            projectKey = projectKey,
            slug = slug,
            name = name,
            readme = readme
        )
    }
}

data class RepositoryWithReadme(
    val projectKey: String,
    val slug: String,
    val name: String,
    val readme: String?
)

data class Line(
    val text: String
)

data class ReadmeResponse(
    val lines: List<Line>
) {
    val text = lines.joinToString("\n") { it.text }
}

class BitbucketApi(
    private val baseUrl: String,
    private val username: String,
    private val password: String
) {
    private fun listProjects(): ProjectsListResponse {
        val getRequest = mkGet("$baseUrl/rest/api/1.0/projects?limit=1000")
        val response = httpClient.send(getRequest, ofString())
        return mapper.readValue(response.body())
    }

    @SuppressWarnings("TooGenericExceptionCaught")
    fun listRepositories(): List<RepositoryWithReadme> {
        return listProjects()
            .values
            .flatMap { project ->
                val reposUrl = project.repositoriesLink(baseUrl)

                val getRequest = mkGet(reposUrl)
                val response = httpClient.send(getRequest, ofString())
                val reposResponse: RepositoriesListResponse = mapper.readValue(response.body())
                reposResponse.values.map { it.mkRepository(project) }
            }
            .map { repository ->
                val readMeText: String? = try {
                    val readmeUrl = repository.readmeLink(baseUrl)
                    val getRequest = mkGet(readmeUrl)
                    val response = httpClient.send(getRequest, ofString())
                    val readmeResponse: ReadmeResponse = mapper.readValue(response.body())
                    readmeResponse.text
                } catch (e: Exception) {
                    null
                }
                repository.withReadme(readMeText)
            }
    }

    private fun mkGet(url: String): HttpRequest {
        return HttpRequest.newBuilder(URI(url))
            .GET()
            .header("Authorization", basicAuth(username, password))
            .build()
    }

    // https://stackoverflow.com/a/54208946
    private fun basicAuth(username: String, password: String): String {
        return "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray())
    }
}
