package com.hamster.pray.genshin.cache

import com.hamster.pray.genshin.config.Config
import com.hamster.pray.genshin.model.PrayRecord
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import java.util.*

object PrayRecordCache : AutoSavePluginData("PrayRecord") {
    var prayDate: Int by value(Date().date)
    var prayRecords: MutableList<PrayRecord> by value()

    fun addPrayRecord(memberCode: String) {
        prayRecords.add(PrayRecord(memberCode))
    }

    fun getSurplusTimes(memberCode: String): Int {
        if (Config.dailyLimit == 0) return 0
        val prayCount = prayRecords.count { it.memberCode == memberCode }
        val surplusTimes = Config.dailyLimit - prayCount
        return if (surplusTimes > 0) surplusTimes else 0
    }

    fun isPrayUseUp(memberCode: String): Boolean {
        val currentDate = Date().date
        if (prayDate != currentDate) {
            prayDate = currentDate
            clearRecord()
        }
        return Config.dailyLimit > 0 && getSurplusTimes(memberCode) == 0
    }

    fun clearRecord() {
        prayRecords.clear()
    }


}