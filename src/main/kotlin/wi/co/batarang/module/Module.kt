package wi.co.batarang.module

import wi.co.batarang.Action
import wi.co.batarang.Setting
import wi.co.batarang.SettingKey

interface Module {
    fun setData(data: String)
    fun updateData(): String
    fun getActions(): List<Action>
    fun updateSettings(settings: List<Setting>)
    val requiredSettings: List<SettingKey>
    val canBeActivated: Boolean
}
