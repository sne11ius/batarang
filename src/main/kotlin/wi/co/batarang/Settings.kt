package wi.co.batarang

import wi.co.batarang.module.Module
import wi.co.batarang.module.modules
import java.lang.System.getProperty
import java.lang.System.getenv
import java.nio.file.Files
import java.nio.file.Files.createDirectories
import java.nio.file.Files.exists
import java.nio.file.Files.readAllLines
import java.nio.file.Files.readString
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

object SettingsService {

    private const val CONFIG_DIR_ENV = "BATARANG_CONF_DIR"

    private val userHome = getProperty("user.home")
    private val configDir: Path = getenv(CONFIG_DIR_ENV)
        ?.let { Paths.get(it) }
        ?: Paths.get(userHome, ".config", "batarang")
    private val configFile: Path = configDir.resolve("config.txt")

    init {
        if (!exists(configDir)) {
            createDirectories(configDir)
        }
        if (!exists(configFile)) {
            Files.createFile(configFile)
        }
        val settings = readSettings()
        var settingsChanged = false
        val newSettings: List<String> = modules.flatMap { module ->
            module.requiredSettings.map { requiredSetting ->
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
        }
        if (settingsChanged) {
            writeSettings(newSettings)
        }
    }

    fun settingsForModule(module: Module): List<Setting> {
        return module.requiredSettings.map { settingKey ->
            val absoluteKey = module.javaClass.simpleName + "." + settingKey.key
            Setting(
                key = settingKey,
                value = readSettings().first { it.startsWith(absoluteKey) }.substringAfter("=")
            )
        }
    }

    private fun readSettings(): List<String> {
        return readAllLines(configFile)
    }

    private fun writeSettings(settings: List<String>) {
        writeString(configFile, settings.joinToString("\n"))
    }

    fun readModuleData(module: Module): String {
        return readString(configDir.resolve(module.javaClass.simpleName + ".txt"))
    }

    fun writeModuleData(module: Module, moduleData: String) {
        writeString(configDir.resolve(module.javaClass.simpleName + ".txt"), moduleData)
    }
}
