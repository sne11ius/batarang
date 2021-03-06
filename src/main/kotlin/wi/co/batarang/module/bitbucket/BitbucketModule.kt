package wi.co.batarang.module.bitbucket

import com.fasterxml.jackson.module.kotlin.readValue
import wi.co.batarang.Action
import wi.co.batarang.Setting
import wi.co.batarang.SettingKey
import wi.co.batarang.module.Module
import wi.co.batarang.module.launcher.LauncherModule.launch
import wi.co.batarang.util.mapper
import wi.co.batarang.util.runBackground

data class RepositoryData(
    val projectKey: String,
    val slug: String,
    val readme: String
) {
    fun mkCloneUrl(gitBaseUrl: String): String = "ssh://git@$gitBaseUrl/$projectKey/$slug.git"
    fun mkWebUrl(httpBaseUrl: String): String = "$httpBaseUrl/projects/$projectKey/repos/$slug/browse"
}

object BitbucketModule : Module {

    private lateinit var settings: List<Setting>

    private val gitUrlKey = SettingKey("Bitbucket git-Basis-URL (e.g. 'myhost.com:1234')", "git-url")
    private val httpUrlKey = SettingKey("Bitbucket http-Basis-URL (e.g.. 'https://myhost.com')", "http-url")
    private val usernameKey = SettingKey("Bitbucket username", "username")
    private val passwordKey = SettingKey("Bitbucket password", "password")

    override val requiredSettings: List<SettingKey> = listOf(gitUrlKey, httpUrlKey, usernameKey, passwordKey)

    override val canBeActivated = true

    private var repositories: List<RepositoryData> = emptyList()

    override fun setData(data: String) {
        repositories = mapper.readValue(data)
    }

    override fun updateData(): String {
        val httpBaseUrl = settings.first { it.key == httpUrlKey }.value
        val username = settings.first { it.key == usernameKey }.value
        val password = settings.first { it.key == passwordKey }.value
        val repoData: List<RepositoryData> = BitbucketApi(httpBaseUrl, username, password)
            .listRepositories()
            .map { RepositoryData(it.projectKey, it.slug, it.readme ?: "") }
        return mapper.writeValueAsString(repoData)
    }

    override fun getActions(): List<Action> {
        val gitBaseUrl = settings.first { it.key == gitUrlKey }.value
        val httpBaseUrl = settings.first { it.key == httpUrlKey }.value
        return repositories.flatMap {
            listOf(
                Action(
                    label = "[bitbucket] CLONE ${it.projectKey}/${it.slug}",
                    tags = listOf("bitbucket", "clone", it.projectKey, it.slug, it.readme)
                ) {
                    "git clone ${it.mkCloneUrl(gitBaseUrl)}".runBackground()
                    "[bitbucket] cloned ${it.projectKey}/${it.slug}"
                },
                Action(
                    label = "[bitbucket] BROWSE ${it.projectKey}/${it.slug}",
                    tags = listOf("bitbucket", "browse", it.projectKey, it.slug, it.readme)
                ) {
                    launch(it.mkWebUrl(httpBaseUrl))
                    "[bitbucket] opened ${it.projectKey}/${it.slug}"
                }
            )
        }
    }

    override fun updateSettings(settings: List<Setting>) {
        this.settings = settings
    }
}
