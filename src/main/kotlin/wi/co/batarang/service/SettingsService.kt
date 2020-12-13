package wi.co.batarang.service

import wi.co.batarang.Setting
import wi.co.batarang.plugins.Plugin
import wi.co.batarang.plugins.plugins
import java.lang.System.getProperty
import java.nio.file.Files.createDirectories
import java.nio.file.Files.createFile
import java.nio.file.Files.exists
import java.nio.file.Files.readAllLines
import java.nio.file.Files.readString
import java.nio.file.Files.writeString
import java.nio.file.Path
import java.nio.file.Paths.get

object SettingsService {

    private val userHome = getProperty("user.home")
    private val configDir: Path = get(userHome, ".config", "batarang")
    private val configFile: Path = configDir.resolve("config.txt")

    init {
        if (!exists(configDir)) {
            createDirectories(configDir)
        }
        if (!exists(configFile)) {
            createFile(configFile)
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
