package com.hamster.pray.genshin

import com.hamster.pray.genshin.model.PrayRecord
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import java.time.LocalDate
import java.util.*

object PrayCoolingData : AutoSavePluginData("PrayCooling") {
    val prayCoolingMap: MutableMap<String, Date> by value()

    fun setCooling(memberCode:String){
        prayCoolingMap.put(memberCode,Date())
    }

    fun isCooling(memberCode: String): Boolean {
        return getCoolingSecond(memberCode) > 0
    }

    fun getCoolingSecond(memberCode: String): Int {
        val coolDate = prayCoolingMap[memberCode] ?: return 0
        val seconds = (Date().time - coolDate.time).toInt() / 1000 + 1
        val surplusSeconds = Config.prayCDSeconds - seconds
        return if (surplusSeconds > 0) surplusSeconds else 0
    }


}