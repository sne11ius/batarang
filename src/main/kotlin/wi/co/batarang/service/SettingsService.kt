package wi.co.batarang.service

import wi.co.batarang.Setting
import wi.co.batarang.plugins.Plugin
import wi.co.batarang.plugins.plugins
import java.nio.file.Files
import java.nio.file.Files.readAllLines
import java.nio.file.Files.readString
import java.nio.file.Files.writeString
import java.nio.file.Path
import java.nio.file.Paths

object SettingsService {

    private val userHome = System.getProperty("user.home")
    private val configDir: Path = Paths.get(userHome, ".config", "batarang")
    private val configFile: Path = configDir.resolve("config.txt")

    init {
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir)
        }
        if (!Files.exists(configFile)) {
            Files.createFile(configFile)
        }
        val settings = readSettings()
        var settingsChanged = false
        val newSettings: List<String> = plugins.flatMap { plugin ->
            plugin.requiredSettings.map { requiredSetting ->
                val key = requiredSetting.key
                val absoluteKey = plugin.javaClass.simpleName + "." + key
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

    fun settingsForPlugin(plugin: Plugin): List<Setting> {
        return plugin.requiredSettings.map { settingKey ->
            val absoluteKey = plugin.javaClass.simpleName + "." + settingKey.key
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

    fun readPluginData(plugin: Plugin): String {
        return readString(configDir.resolve(plugin.javaClass.simpleName + ".txt"))
    }

    fun writePluginData(plugin: Plugin, pluginData: String) {
        writeString(configDir.resolve(plugin.javaClass.simpleName + ".txt"), pluginData)
    }

}
