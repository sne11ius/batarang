package wi.co.batarang.module.activation

import wi.co.batarang.Action
import wi.co.batarang.Setting
import wi.co.batarang.SettingKey
import wi.co.batarang.module.Module
import wi.co.batarang.module.modules

object ActivationModule : Module {

    private lateinit var settings: List<Setting>

    override val canBeActivated = false
    override val requiredSettings: List<SettingKey>
        get() = modules.filter { it.canBeActivated }.map { module ->
            SettingKey("Activate module ${module.javaClass.simpleName} (Y/n)?", module.javaClass.simpleName)
        }

    fun isModuleActive(module: Module): Boolean {
        if (!module.canBeActivated) return true
        val settingForModule = settings.first { it.key.key == module.javaClass.simpleName }
        return settingForModule.value.toLowerCase() in listOf("", "y", "yes", "true")
    }

    override fun setData(data: String) {
        // This intentionally blank
    }

    override fun updateData(): String {
        return ""
    }

    override fun getActions(): List<Action> {
        return emptyList()
    }

    override fun updateSettings(settings: List<Setting>) {
        this.settings = settings
    }
}
