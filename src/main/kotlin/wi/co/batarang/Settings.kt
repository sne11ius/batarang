package wi.co.batarang

import wi.co.batarang.module.Module
import wi.co.batarang.module.activation.ActivationModule.isModuleActive
import wi.co.batarang.module.modules
import java.lang.System.getProperty
import java.lang.System.getenv
import java.nio.file.Files
import java.nio.file.Files.createDirectories
import java.nio.file.Files.exists
import java.nio.file.Files.readAllLines
import java.nio.file.Files.writeString
import java.nio.file.Path
import java.nio.file.Paths

data class SettingKey(
    val description: String,
    val key: String
)

data class Setting(
    val key: SettingKey,
    val value: String
)

object Settings {

    private const val CONFIG_DIR_ENV = "BATARANG_CONF_DIR"

    private val userHome = getProperty("user.home")
    private val configDir: Path = getenv(CONFIG_DIR_ENV)
        ?.let { Paths.get(it) }
        ?: Paths.get(userHome, ".config", "batarang")
    private val configFile: Path = configDir.resolve("config.txt")

    fun update() {
        if (!exists(configDir)) {
            createDirectories(configDir)
        }
        if (!exists(configFile)) {
            Files.createFile(configFile)
        }
        val settings = readSettings()
        var settingsChanged = false
        val newSettings: List<String> = modules.flatMap { module ->
            if (isModuleActive(module)) {
                val settingsForModule = module.requiredSettings.map { requiredSetting ->
                    val key = requiredSetting.key
                    val absoluteKey = module.javaClass.simpleName + "." + key
                    if (settings.none { it.startsWith(absoluteKey) }) {
                        settingsChanged = true
                        print("Please enter ${requiredSetting.description}: ")
                        val enteredValue = readLine()
                        "$absoluteKey=$enteredValue"
                    } else {
                        settings.first { it.startsWith(absoluteKey) }
                    }
                }
                module.updateSettings(
                    settingsForModule.map { settingString ->
                        val relativeSettingString = settingString.substringAfter(module.javaClass.simpleName + ".")
                        Setting(
                            key = module.requiredSettings.first { it.key == relativeSettingString.substringBefore("=") },
                            value = relativeSettingString.substringAfter("=")
                        )
                    }
                )
                settingsForModule
            } else emptyList()
        }
        if (settingsChanged) {
            writeSettings(newSettings)
        }
    }

    private fun readSettings(): List<String> {
        return readAllLines(configFile)
    }

    private fun writeSettings(settings: List<String>) {
        writeString(configFile, settings.joinToString("\n"))
    }

    fun configFileForModule(module: Module): Path {
        return configDir.resolve(module.javaClass.simpleName + ".txt")
    }
}
