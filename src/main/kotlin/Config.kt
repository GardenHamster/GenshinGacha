package com.hamster.pray.genshin

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object Config: AutoSavePluginConfig("config") {
    val enabled_group:MutableList<Long> by value(mutableListOf())
    val super_manager:MutableList<Long> by value(mutableListOf())
    val apiUrl by value<String>("https://www.theresa3rd.cn:8080")
    val authorzation by value<String>("theresa3rd")
    val dailyLimit by value<Int>(0)
    val overLimitMsg by value<String>("今日的抽卡次数已经用完了，明天再来吧~")
    private val prayCDSeconds by value<Int>(30)
    val coolingMsg by value<String>("抽卡功能冷却中，{cdSeconds}秒后再来吧~")
    val prefix by value<String>("#")
    val errorMsg by value<String>("出了点小问题，问题不大，请艾特管理员...")
    val prayingMsg by value<String>("正在拉取结果...")
    val goldMsg by value<String>("{userName}通过{prayType}获得了{goodsName}，累计消耗{star5Cost}抽")
    val menu: MutableList<String> by value(mutableListOf("菜单", "功能", "祈愿", "抽卡", "扭蛋", "十连", "单抽", "武器", "角色"))
    val menuMsg by value<String>("目前可用的抽卡指令有：\r\n" +
        "角色单抽[编号1~10]，角色十连[编号1~10]，武器单抽，武器十连，常驻单抽，常驻十连，全角单抽，全角十连，全武单抽，全武十连，定轨\r\n" +
        "目前可用的查询指令有：\r\n" +
        "蛋池，祈愿详情，祈愿记录，欧气排行\r\n" +
        "目前可用的管理员指令有：\r\n" +
        "设定角色池，设定武器池，重置角色池，重置武器池，服装概率")
    val rolePrayOne by value<String>("角色单抽")
    val rolePrayTen by value<String>("角色十连")
    val armPrayOne by value<String>("武器单抽")
    val armPrayTen by value<String>("武器十连")
    val permPrayOne by value<String>("常驻单抽")
    val permPrayTen by value<String>("常驻十连")
    val fullRolePrayOne by value<String>("全角单抽")
    val fullRolePrayTen by value<String>("全角十连")
    val fullArmPrayOne by value<String>("全武单抽")
    val fullArmPrayTen by value<String>("全武十连")
    val assign by value<String>("定轨")
    val getPondInfo by value<String>("蛋池")
    val getPrayDetail by value<String>("祈愿详情")
    val getPrayRecords by value<String>("祈愿记录")
    val getLuckRanking by value<String>("欧气排行")
    val setRolePond by value<String>("设定角色池")
    val setArmPond by value<String>("设定武器池")
    val resetRolePond by value<String>("重置角色池")
    val resetArmPond by value<String>("重置武器池")
    val setSkinRate by value<String>("服装概率")

    fun getPrayCD():Int{
        return if (prayCDSeconds < 10) 10 else prayCDSeconds
    }

}