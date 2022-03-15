package com.hamster.pray.genshin

import com.hamster.pray.genshin.model.PrayRecord
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import java.util.*

object PrayRecordData : AutoSavePluginData("PrayRecord") {
    var prayDate: Int by value(Date().date)
    val prayRecords: MutableList<PrayRecord> by value()

    fun addPrayRecord(memberCode: String) {
        val currentDate = Date().date
        if (prayDate != currentDate) {
            prayDate = currentDate
            clearRecord()
        }
        prayRecords.add(PrayRecord(memberCode))
    }

    fun getSurplusTimes(memberCode: String): Int {
        if (Config.dailyLimit == 0) return -1
        val prayCount = prayRecords.count { it.memberCode == memberCode }
        val surplusTimes = Config.dailyLimit - prayCount
        return if (surplusTimes > 0) surplusTimes else 0
    }

    fun isPrayUseUp(memberCode: String): Boolean {
        return getSurplusTimes(memberCode) != 0
    }

    fun clearRecord() {
        prayRecords.clear()
    }


}