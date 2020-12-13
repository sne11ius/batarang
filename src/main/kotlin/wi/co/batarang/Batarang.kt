package wi.co.batarang

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.ActionListBox
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.GridLayout.Alignment.BEGINNING
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.MultiWindowTextGUI
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextBox
import com.googlecode.lanterna.gui2.Window.Hint.CENTERED
import com.googlecode.lanterna.gui2.Window.Hint.NO_DECORATIONS
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.ansi.UnixTerminal
import wi.co.batarang.plugins.plugins
import wi.co.batarang.service.SettingsService
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if ("-h" in args) {
        printHelpAndExit()
    }
    if ("-u" in args) {
        updatePluginData()
        exitProcess(0)
    }

    if ("---generate-Native-Image-Config" in args) {
        // Use as much stuff as possible to generate data for graalvm
        // val terminal = UnixTerminal()
        // val screen = TerminalScreen(terminal)
        // screen.startScreen()
        // screen.stopScreen(false)
        // updatePluginData()
    }
    runGui(args)
}

fun printHelpAndExit() {
    println(
        """
            batarang -_-
            Parameters:
              Any parameters can be used to pre-filter the task list
            Options:
              -u Update data and exit. Consider running with this option as cron task
              -h Print help and exit
        """.trimIndent()
    )
    exitProcess(0)
}

private fun runGui(args: Array<String>) {
    val allActions = plugins.flatMap { plugin ->
        plugin.setData(SettingsService.readPluginData(plugin))
        val settingsForPlugin = SettingsService.settingsForPlugin(plugin)
        plugin.getActions(settingsForPlugin)
    }
    val immediateActions = allActions.filter { it.matches(args.toList()) }
    if (immediateActions.size == 1) {
        val action = immediateActions.first()
        println("Run ${action.label}? (Y/n)")
        val input = readLine()
        if (input.isNullOrBlank() || input.toLowerCase().trim() == "y") {
            action.action.run()
            exitProcess(0)
        }
    }
    buildLayout(args, allActions)
}

@SuppressWarnings("MagicNumber")
fun buildLayout(args: Array<String>, allActions: List<Action>) {
    val terminal = UnixTerminal()
    val screen = TerminalScreen(terminal)
    screen.startScreen()

    val window = BasicWindow()
    window.setHints(listOf(CENTERED, NO_DECORATIONS))

    val textGUI = MultiWindowTextGUI(screen)
    val contentPanel = Panel(GridLayout(2))

    val gridLayout = contentPanel.layoutManager as GridLayout
    gridLayout.horizontalSpacing = 0
    gridLayout.verticalSpacing = 0
    val label = Label("\uF61A >> ")
    contentPanel.addComponent(label)
    val textBox = TextBox(TerminalSize(60, 1))

    val actionList = ActionListBox(TerminalSize(65, 20))
    actionList.addItem("...", Runnable {})
    actionList.layoutData = GridLayout.createLayoutData(
        BEGINNING,
        BEGINNING,
        true,
        false,
        2,
        1
    )
    textBox.setTextChangeListener { newText, _ ->
        val actions = allActions.filter { it.matches(newText.split(" ")) }
        actionList.clearItems()
        if (actions.isEmpty()) {
            actionList.addItem("...", Runnable {})
        } else {
            actions.forEach { action ->
                actionList.addItem(action.label) {
                    action.action.run()
                    exitProcess(0)
                }
            }
        }
    }
    contentPanel.apply {
        addComponent(textBox)
        addComponent(actionList)
        addComponent(
            Button("Cancel") {
                exitProcess(0)
            }
        )
    }

    textBox.text = args.joinToString(" ")
    window.component = contentPanel

    textGUI.addWindowAndWait(window)
}

private fun updatePluginData() {
    plugins.forEach { plugin ->
        println("Aktualisiere Daten für ${plugin.javaClass.simpleName}")
        val pluginSettings = SettingsService.settingsForPlugin(plugin)
        val pluginData = plugin.updateData(pluginSettings)
        SettingsService.writePluginData(plugin, pluginData)
    }
}
