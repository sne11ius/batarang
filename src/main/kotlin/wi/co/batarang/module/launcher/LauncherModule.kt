package wi.co.batarang.module.launcher

import wi.co.batarang.Action
import wi.co.batarang.Setting
import wi.co.batarang.SettingKey
import wi.co.batarang.module.Module
import wi.co.batarang.util.runBackground

// Yes, a slightly ill-advised use of our shiny module system. Hit me :D
object LauncherModule : Module {

    private val launchCommandlKey = SettingKey(
        "How to launch stuff, esp. Web-URLs. Use `{}` as placeholder (e.g. 'xdg-open {}'.",
        "launcher-command"
    )

    override val requiredSettings = listOf(launchCommandlKey)

    private var launchCommand: String? = null

    override fun setData(data: String) {
        // This method intentionally blank
    }

    override fun updateData(settings: List<Setting>): String {
        return ""
    }

    override fun getActions(settings: List<Setting>): List<Action> {
        launchCommand = settings.first { it.key == launchCommandlKey }.value
        return emptyList()
    }

    @Suppress("TooGenericExceptionThrown")
    fun launch(thing: String) {
        launchCommand?.replace("{}", thing)?.runBackground() ?: throw RuntimeException("Launch command not set")
    }
}
