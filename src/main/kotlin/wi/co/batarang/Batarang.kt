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
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import wi.co.batarang.Settings.configFileForModule
import wi.co.batarang.module.Module
import wi.co.batarang.module.activation.ActivationModule.isModuleActive
import wi.co.batarang.module.modules
import java.nio.file.Files.exists
import java.nio.file.Files.readString
import java.nio.file.Files.writeString
import kotlin.system.exitProcess

@Suppress("TooGenericExceptionCaught")
fun main(args: Array<String>) {
    if ("-h" in args) {
        printHelpAndExit()
    }
    Settings.update()
    if ("-u" in args) {
        updateAllModuleData()
        exitProcess(0)
    }

    if ("---generate-Native-Image-Config" in args) {
        // Use as much stuff as possible to generate data for graalvm
        // Hence no `exitProcess`
        updateAllModuleData()
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
    val allActions = modules.filter { isModuleActive(it) }.flatMap { module ->
        module.setData(readModuleData(module))
        module.getActions()
    }
    val immediateActions = allActions.filter { it.matches(args.toList()) }
    if (immediateActions.size == 1) {
        val action = immediateActions.first()
        println("Run ${action.label}? (Y/n)")
        val input = readLine()
        if (input.isNullOrBlank() || input.toLowerCase().trim() == "y") {
            val message = action.action.invoke()
            println(message)
            exitProcess(0)
        }
    }
    buildLayout(args, allActions)
}

@SuppressWarnings("MagicNumber")
fun buildLayout(args: Array<String>, allActions: List<Action>) {
    val terminal = DefaultTerminalFactory().createHeadlessTerminal()
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
    actionList.addItem("...") {}
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
            actionList.addItem("...") {}
        } else {
            actions.forEach { action ->
                actionList.addItem(action.label) {
                    terminal.close()
                    val output = action.action.invoke()
                    println(output)
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

private fun updateAllModuleData() {
    modules.filter { isModuleActive(it) }.forEach { updateModuleData(it) }
}

fun updateModuleData(module: Module) {
    println("Updating ${module.javaClass.simpleName}")
    val moduleData = module.updateData()
    writeModuleData(module, moduleData)
}

fun readModuleData(module: Module): String {
    val configFile = configFileForModule(module)
    if (!exists(configFile)) {
        updateModuleData(module)
    }
    return readString(configFile)
}

fun writeModuleData(module: Module, moduleData: String) {
    writeString(configFileForModule(module), moduleData)
}
