package com.hamster.pray.genshin

import com.hamster.pray.genshin.model.PrayRecord
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import java.time.LocalDate
import java.util.*

class PrayCoolingData {
    companion object {
        val prayCoolingMap: MutableMap<String, Long> = HashMap()

        fun setCooling(memberCode: String) {
            prayCoolingMap.put(memberCode, Date().time)
        }

        fun isCooling(memberCode: String): Boolean {
            return getCoolingSecond(memberCode) > 0
        }

        fun getCoolingSecond(memberCode: String): Int {
            val coolTime = prayCoolingMap[memberCode] ?: return 0
            val seconds = (Date().time - coolTime).toInt() / 1000 + 1
            val surplusSeconds = Config.getPrayCD() - seconds
            return if (surplusSeconds > 0) surplusSeconds else 0
        }
    }


}