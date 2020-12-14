package wi.co.batarang.module

import wi.co.batarang.Action
import wi.co.batarang.Setting
import wi.co.batarang.SettingKey

interface Module {
    fun setData(data: String)
    fun updateData(settings: List<Setting>): String
    fun getActions(settings: List<Setting>): List<Action>
    val requiredSettings: List<SettingKey>
}
