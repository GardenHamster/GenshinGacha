package com.hamster.pray.genshin.cache

import com.hamster.pray.genshin.config.Config
import java.util.*

class PrayCoolingCache {
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