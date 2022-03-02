package com.hamster.pray.genshin

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object Config: AutoSavePluginConfig("config") {
    val enabled_group:MutableList<Long> by value(mutableListOf())
    val apiUrl by value<String>("http://103.233.255.230:8080")
    val authorzation by value<String>("theresa3rd")
    val prefix by value<String>("#")
    val rolePrayOne by value<String>("角色单抽")
    val rolePrayTen by value<String>("角色十连")
    val armPrayOne by value<String>("武器单抽")
    val armPrayTen by value<String>("武器十连")
    val permPrayOne by value<String>("常驻单抽")
    val permPrayTen by value<String>("常驻十连")
    val assign by value<String>("定轨")
    val getPondInfo by value<String>("蛋池")
    val getPrayDetail by value<String>("祈愿详情")
    val getPrayRecords by value<String>("祈愿记录")
    val getLuckRanking by value<String>("欧气排行")
}