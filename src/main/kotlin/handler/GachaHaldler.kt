package com.hamster.pray.genshin.handler

import com.hamster.pray.genshin.PluginMain
import com.hamster.pray.genshin.cache.PrayCoolingCache
import com.hamster.pray.genshin.cache.PrayRecordCache
import com.hamster.pray.genshin.config.Config
import com.hamster.pray.genshin.data.*
import com.hamster.pray.genshin.util.DateUtil
import com.hamster.pray.genshin.util.GachaUtil
import com.hamster.pray.genshin.util.HttpUtil
import com.hamster.pray.genshin.util.StringUtil
import kotlinx.coroutines.delay
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import java.io.File

class GachaHaldler(val group: Group, val sender: Member, val message: MessageChain) {

    private val memberCode: String = sender.id.toString()
    private val memberName: String = sender.nick

    private suspend fun checkPondIndex(instruction: String, command: String): Int {
        val pondIndexstr = StringUtil.splitKeyWord(instruction, command)
        if (pondIndexstr.isNullOrEmpty() || pondIndexstr.toIntOrNull() == null) {
            group.sendMessage(message.quote() + "指定的蛋池编号无效")
            return -1
        }
        var pondIndex = pondIndexstr.toInt() - 1
        if (pondIndex < 0) pondIndex = 0
        return pondIndex
    }

    private suspend fun checkPrayUseUp(memberCode: String): Boolean{
        if (!PrayRecordCache.isPrayUseUp(memberCode)) return false
        if (Config.overLimitMsg.isNotBlank()) group.sendMessage(message.quote() + Config.overLimitMsg)
        return true
    }

    private suspend fun checkMemberCooling(memberCode: String): Boolean {
        if (!PrayCoolingCache.isCooling(memberCode)) return false
        if (Config.coolingMsg.isBlank()) return true
        val coolingSecond = PrayCoolingCache.getCoolingSecond(memberCode).toString()
        val coolingMsg = Config.coolingMsg.replace("{cdSeconds}", coolingSecond)
        group.sendMessage(message.quote() + coolingMsg)
        return true
    }

    private suspend fun checkSuperManager(): Boolean{
        if (sender.id in Config.super_manager)return true
        group.sendMessage(message.quote() + "该指令需要管理员执行")
        return false
    }

    private suspend fun<T> checkApiResult(apiResult: ApiResult<T>): Boolean {
        if (apiResult.code == 0) return true
        group.sendMessage("${Config.errorMsg}，接口返回code：${apiResult.code}，接口返回message：${apiResult.message}")
        return false
    }

    private suspend fun checkDownImg(apiData: PrayResult, imgFile: File?): Boolean {
        if (imgFile != null) return true
        group.sendMessage("${Config.errorMsg}，图片下载失败了，url=${apiData.imgHttpUrl}")
        return false
    }

    private fun getUpItems(apiData: PrayResult): String {
        var upItem = ""
        for (item in apiData.star5Up) {
            if (upItem.isNotEmpty()) upItem += "+"
            upItem += item.goodsName
        }
        for (item in apiData.star4Up) {
            if (upItem.isNotEmpty()) upItem += "+"
            upItem += item.goodsName
        }
        return upItem
    }

    private fun downImg(apiData: PrayResult):File?{
        val imgSaveDir = "${PluginMain.dataFolderPath}/download/${DateUtil.getDateStr()}"
        if (!File(imgSaveDir).exists()) File(imgSaveDir).mkdirs()
        val imgSavePath = "${imgSaveDir}/${System.currentTimeMillis()}.jpg"
        return HttpUtil.downloadPicture(apiData.imgHttpUrl, imgSavePath)
    }

    private fun getSurplusMsg(memberCode: String): String {
        val surplusMagBuilder = StringBuilder()
        var surplusTimes = PrayRecordCache.getSurplusTimes(memberCode) - 1
        if (surplusTimes < 0) surplusTimes = 0
        if (Config.dailyLimit > 0) surplusMagBuilder.append("，今日剩余可用抽卡次数${surplusTimes}次")
        if (Config.getPrayCD() > 0) surplusMagBuilder.append("，CD${Config.getPrayCD()}秒")
        return surplusMagBuilder.toString().trimIndent()
    }

    private suspend fun sendGoldMsg(apiData: PrayResult, pondName: String) {
        if (apiData.star5Goods.isEmpty()) return
        if (Config.goldMsg.isBlank()) return
        var star5Item = ""
        for (item in apiData.star5Goods) {
            if (star5Item.isNotEmpty()) star5Item += "+"
            star5Item += item.goodsName
        }
        var goldMsg = Config.goldMsg.trim()
        goldMsg = goldMsg.replace("{userName}", sender.nameCardOrNick)
        goldMsg = goldMsg.replace("{prayType}", pondName)
        goldMsg = goldMsg.replace("{goodsName}", star5Item)
        goldMsg = goldMsg.replace("{star5Cost}", apiData.star5Cost.toString())
        delay(1000)
        group.sendMessage(goldMsg)
    }

    private suspend fun doPray(pondName: String, getPrayResult: () -> ApiResult<PrayResult>, prayMsg: (prayResult: PrayResult) -> StringBuilder) {
        if (checkPrayUseUp(memberCode)) return
        if (checkMemberCooling(memberCode)) return
        PrayCoolingCache.setCooling(memberCode)
        if (Config.prayingMsg.isNotBlank()) group.sendMessage(Config.prayingMsg)

        val apiResult = getPrayResult()
        if (!checkApiResult(apiResult)) return
        val apiData = apiResult.data

        val imgFile = downImg(apiData)
        if (!checkDownImg(apiData, imgFile)) return

        val imgMsg = imgFile?.uploadAsImage(sender, "jpg")?.toString() ?: ""
        group.sendMessage(message.quote() + prayMsg(apiData).toString() + imgMsg)
        sendGoldMsg(apiData, pondName)
        PrayRecordCache.addPrayRecord(memberCode)
    }

    suspend fun rolePrayOne(msgContent: String, command: String) {
        val pondName = "角色单抽"
        val pondIndex = checkPondIndex(msgContent, command)
        if (pondIndex < 0) return
        val apiResult = fun(): ApiResult<PrayResult> {
            return GachaUtil.rolePrayOne(pondIndex, memberCode, memberName)
        }
        val prayMsg = fun(apiData: PrayResult): StringBuilder {
            val resultMsgBuilder = StringBuilder()
            if (apiData.star5Cost > 0) resultMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
            resultMsgBuilder.append("当前卡池为：${getUpItems(apiData)}，")
            resultMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
            resultMsgBuilder.append("距离下次小保底还剩${apiData.role90Surplus}抽，")
            resultMsgBuilder.append("大保底还剩${apiData.role180Surplus}抽")
            resultMsgBuilder.append(getSurplusMsg(memberCode))
            return resultMsgBuilder
        }
        doPray(pondName, apiResult, prayMsg)
    }

    suspend fun rolePrayTen(msgContent: String, command: String) {
        val pondName = "角色十连"
        val pondIndex = checkPondIndex(msgContent, command)
        if (pondIndex < 0) return
        val apiResult = fun(): ApiResult<PrayResult> {
            return GachaUtil.rolePrayTen(pondIndex, memberCode, memberName)
        }
        val prayMsg = fun(apiData: PrayResult): StringBuilder {
            val resultMsgBuilder = StringBuilder()
            if (apiData.star5Cost > 0) resultMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
            resultMsgBuilder.append("当前卡池为：${getUpItems(apiData)}，")
            resultMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
            resultMsgBuilder.append("距离下次小保底还剩${apiData.role90Surplus}抽，")
            resultMsgBuilder.append("大保底还剩${apiData.role180Surplus}抽")
            resultMsgBuilder.append(getSurplusMsg(memberCode))
            return resultMsgBuilder
        }
        doPray(pondName, apiResult, prayMsg)
    }

    suspend fun armPrayOne() {
        val pondName = "武器单抽"
        val apiResult = fun(): ApiResult<PrayResult> {
            return GachaUtil.armPrayOne(memberCode, memberName)
        }
        val prayMsg = fun(apiData: PrayResult): StringBuilder {
            val resultMsgBuilder = StringBuilder()
            if (apiData.star5Cost > 0) resultMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
            resultMsgBuilder.append("当前卡池为：${getUpItems(apiData)}，")
            resultMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
            resultMsgBuilder.append("距离下次保底还剩${apiData.arm80Surplus}抽，")
            resultMsgBuilder.append("当前命定值为：${apiData.armAssignValue}")
            resultMsgBuilder.append(getSurplusMsg(memberCode))
            return resultMsgBuilder
        }
        doPray(pondName, apiResult, prayMsg)
    }

    suspend fun armPrayTen() {
        val pondName = "武器十连"
        val apiResult = fun(): ApiResult<PrayResult> {
            return GachaUtil.armPrayTen(memberCode, memberName)
        }
        val prayMsg = fun(apiData: PrayResult): StringBuilder {
            val resultMsgBuilder = StringBuilder()
            if (apiData.star5Cost > 0) resultMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
            resultMsgBuilder.append("当前卡池为：${getUpItems(apiData)}，")
            resultMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
            resultMsgBuilder.append("距离下次保底还剩${apiData.arm80Surplus}抽，")
            resultMsgBuilder.append("当前命定值为：${apiData.armAssignValue}")
            resultMsgBuilder.append(getSurplusMsg(memberCode))
            return resultMsgBuilder
        }
        doPray(pondName, apiResult, prayMsg)
    }

    suspend fun permPrayOne() {
        val pondName = "常驻单抽"
        val apiResult = fun(): ApiResult<PrayResult> {
            return GachaUtil.permPrayOne(memberCode, memberName)
        }
        val prayMsg = fun(apiData: PrayResult): StringBuilder {
            val resultMsgBuilder = StringBuilder()
            if (apiData.star5Cost > 0) resultMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
            resultMsgBuilder.append("当前卡池为：${getUpItems(apiData)}，")
            resultMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个相遇之缘，")
            resultMsgBuilder.append("距离下次保底还剩${apiData.perm90Surplus}抽，")
            resultMsgBuilder.append(getSurplusMsg(memberCode))
            return resultMsgBuilder
        }
        doPray(pondName, apiResult, prayMsg)
    }

    suspend fun permPrayTen() {
        val pondName = "常驻十连"
        val apiResult = fun(): ApiResult<PrayResult> {
            return GachaUtil.permPrayTen(memberCode, memberName)
        }
        val prayMsg = fun(apiData: PrayResult): StringBuilder {
            val resultMsgBuilder = StringBuilder()
            if (apiData.star5Cost > 0) resultMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
            resultMsgBuilder.append("当前卡池为：${getUpItems(apiData)}，")
            resultMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个相遇之缘，")
            resultMsgBuilder.append("距离下次保底还剩${apiData.perm90Surplus}抽，")
            resultMsgBuilder.append(getSurplusMsg(memberCode))
            return resultMsgBuilder
        }
        doPray(pondName, apiResult, prayMsg)
    }

    suspend fun fullRolePrayOne() {
        val pondName = "全角单抽"
        val apiResult = fun(): ApiResult<PrayResult> {
            return GachaUtil.fullRolePrayOne(memberCode, memberName)
        }
        val prayMsg = fun(apiData: PrayResult): StringBuilder {
            val resultMsgBuilder = StringBuilder()
            if (apiData.star5Cost > 0) resultMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
            resultMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
            resultMsgBuilder.append("距离下次保底还剩${apiData.fullRole90Surplus}抽，")
            resultMsgBuilder.append(getSurplusMsg(memberCode))
            return resultMsgBuilder
        }
        doPray(pondName, apiResult, prayMsg)
    }

    suspend fun fullRolePrayTen() {
        val pondName = "全角十连"
        val apiResult = fun(): ApiResult<PrayResult> {
            return GachaUtil.fullRolePrayTen(memberCode, memberName)
        }
        val prayMsg = fun(apiData: PrayResult): StringBuilder {
            val resultMsgBuilder = StringBuilder()
            if (apiData.star5Cost > 0) resultMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
            resultMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
            resultMsgBuilder.append("距离下次保底还剩${apiData.fullRole90Surplus}抽，")
            resultMsgBuilder.append(getSurplusMsg(memberCode))
            return resultMsgBuilder
        }
        doPray(pondName, apiResult, prayMsg)
    }

    suspend fun fullArmPrayOne() {
        val pondName = "全武单抽"
        val apiResult = fun(): ApiResult<PrayResult> {
            return GachaUtil.fullArmPrayOne(memberCode, memberName)
        }
        val prayMsg = fun(apiData: PrayResult): StringBuilder {
            val resultMsgBuilder = StringBuilder()
            if (apiData.star5Cost > 0) resultMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
            resultMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
            resultMsgBuilder.append("距离下次保底还剩${apiData.fullArm80Surplus}抽，")
            resultMsgBuilder.append(getSurplusMsg(memberCode))
            return resultMsgBuilder
        }
        doPray(pondName, apiResult, prayMsg)
    }

    suspend fun fullArmPrayTen() {
        val pondName = "全武十连"
        val apiResult = fun(): ApiResult<PrayResult> {
            return GachaUtil.fullArmPrayTen(memberCode, memberName)
        }
        val prayMsg = fun(apiData: PrayResult): StringBuilder {
            val resultMsgBuilder = StringBuilder()
            if (apiData.star5Cost > 0) resultMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
            resultMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
            resultMsgBuilder.append("距离下次保底还剩${apiData.fullArm80Surplus}抽，")
            resultMsgBuilder.append(getSurplusMsg(memberCode))
            return resultMsgBuilder
        }
        doPray(pondName, apiResult, prayMsg)
    }


    suspend fun assign(msgContent: String, command: String) {
        val goodsName = StringUtil.splitKeyWord(msgContent, command)
        if (goodsName.isNullOrEmpty() || goodsName.isNullOrBlank()) {
            group.sendMessage(message.quote() + "格式错误，请参考格式：#定轨薙草之稻光")
            return
        }
        val apiResult = GachaUtil.assign(memberCode, memberName, goodsName)
        if (!checkApiResult(apiResult)) return
        group.sendMessage(message.quote() + "武器${goodsName}定轨成功!")
    }

    suspend fun getPondInfo() {
        val apiResult = GachaUtil.getPondInfo()
        if (!checkApiResult(apiResult)) return
        val apiData = apiResult.data
        val msgInfo = StringBuilder()
        val roleInfo = StringBuilder()
        val armInfo = StringBuilder()
        msgInfo.appendLine("目前up内容如下：")
        for (item in apiData.role) {
            roleInfo.appendLine(" 角色池${item.pondIndex + 1}：")
            for (star5 in item.pondInfo.star5UpList) {
                roleInfo.appendLine("  5星：${star5.goodsName}")
            }
            for (star4 in item.pondInfo.star4UpList) {
                roleInfo.appendLine("  4星：${star4.goodsName}")
            }
        }
        armInfo.appendLine(" 武器池：")
        for (star5 in apiData.arm[0].pondInfo.star5UpList) {
            armInfo.appendLine("  5星：${star5.goodsName}")
        }
        for (star4 in apiData.arm[0].pondInfo.star4UpList) {
            armInfo.appendLine("  4星：${star4.goodsName}")
        }
        group.sendMessage(message.quote() + msgInfo.toString() + roleInfo.toString() + armInfo.toString())
    }

    suspend fun getPrayDetail() {
        val apiResult = GachaUtil.getPrayDetail(memberCode)
        if (!checkApiResult(apiResult)) return
        val apiData = apiResult.data

        val msgInfo = StringBuilder()
        msgInfo.appendLine("你的祈愿详情如下：")
        msgInfo.appendLine("角色池大保底剩余抽数：${apiData.role180Surplus}")
        msgInfo.appendLine("角色池小保底剩余抽数：${apiData.role90Surplus}")
        msgInfo.appendLine("武器池命定值：${apiData.armAssignValue}")
        msgInfo.appendLine("武器池保底剩余抽数：${apiData.arm80Surplus}")
        msgInfo.appendLine("常驻池保底剩余抽数：${apiData.perm90Surplus}")
        msgInfo.appendLine("角色池累计抽取次数：${apiData.rolePrayTimes}")
        msgInfo.appendLine("武器池累计抽取次数：${apiData.armPrayTimes}")
        msgInfo.appendLine("常驻池累计抽取次数：${apiData.permPrayTimes}")
        msgInfo.appendLine("所有池累计抽取次数：${apiData.totalPrayTimes}")
        msgInfo.appendLine("累计获得5星数量${apiData.star5Count}")
        msgInfo.appendLine("累计获得4星数量：${apiData.star4Count}")
        msgInfo.appendLine("5星出率：${apiData.star5Rate}%")
        msgInfo.appendLine("4星出率：${apiData.star4Rate}%")
        group.sendMessage(message.quote() + msgInfo.toString())
    }

    suspend fun getPrayRecord() {
        val apiResult = GachaUtil.getPrayRecords(memberCode)
        if (!checkApiResult(apiResult)) return
        val apiData = apiResult.data

        val msgInfo = StringBuilder()
        val star5Info = StringBuilder()

        msgInfo.appendLine("祈愿记录如下：")

        star5Info.appendLine("5星列表：")
        star5Info.appendLine("物品[消耗抽数]获取时间")
        for (item in apiData.star5.all) {
            star5Info.appendLine("${item.goodsName}[${item.cost}]${item.createDate}")
        }
        group.sendMessage(message.quote() + msgInfo.toString() + star5Info.toString())
    }

    suspend fun getLuckRanking() {
        val apiResult = GachaUtil.getLuckRanking()
        if (!checkApiResult(apiResult)) return
        val apiData = apiResult.data

        val msgInfo = StringBuilder()
        val star5Info = StringBuilder()

        msgInfo.appendLine("出货率最高的前${apiData.top}名成员如下，统计开始日期：${apiData.startDate}，排行结果每5分钟缓存一次")
        star5Info.appendLine("名称(id)[5星数量/累计抽数=5星出率]")
        for (item in apiData.star5Ranking) {
            star5Info.appendLine("  ${item.memberName}(${item.memberCode})[${item.count}/${item.totalPrayTimes}=${item.rate}%]")
        }
        group.sendMessage(message.quote() + msgInfo.toString() + star5Info.toString())
    }

    suspend fun setRolePond(msgContent: String, command: String) {
        if (!checkSuperManager()) return
        val paramStr = StringUtil.splitKeyWord(msgContent, command)
        var paramArr = paramStr?.trim()?.split("[,， ]+".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
        if (paramArr.isNullOrEmpty()) {
            group.sendMessage(message.quote() + "格式错误，请参考格式：#设定角色池[编号(1~10,或者可以不指定)] 雷电将军，五郎，云堇，香菱")
            return
        }
        var pondIndex = paramArr[0].toIntOrNull()
        if (pondIndex != null) paramArr = paramArr.copyOfRange(1, paramArr.count())
        if (paramArr.count() != 4) {
            group.sendMessage(message.quote() + "必须指定1个五星和3个四星角色")
            return
        }
        if (pondIndex == null) pondIndex = 0
        if (pondIndex < 0 || pondIndex > 10) {
            group.sendMessage(message.quote() + "蛋池编号只能设定在1~10之间")
            return
        }
        pondIndex = if (pondIndex - 1 < 0) 0 else pondIndex - 1
        val apiResult = GachaUtil.setRolePond(pondIndex, paramArr)
        if (!checkApiResult(apiResult)) return
        group.sendMessage(message.quote() + "配置成功!")
    }

    suspend fun setArmPond(msgContent: String, command: String) {
        if (!checkSuperManager()) return
        val paramStr = StringUtil.splitKeyWord(msgContent, command)
        val paramArr = paramStr?.trim()?.split("[,， ]+".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
        if (paramArr.isNullOrEmpty()) {
            group.sendMessage(message.quote() + "格式错误，请参考格式：#设定武器池 薙草之稻光 不灭月华 恶王丸 曚云之月 匣里龙吟 西风长枪 祭礼残章")
            return
        }
        if (paramArr.count() != 7) {
            group.sendMessage(message.quote() + "必须指定2件五星和5件四星武器")
            return
        }
        val apiResult = GachaUtil.setArmPond(paramArr)
        if (!checkApiResult(apiResult)) return
        group.sendMessage(message.quote() + "配置成功!")
    }

    suspend fun resetRolePond() {
        if (!checkSuperManager()) return
        val apiResult = GachaUtil.resetRolePond()
        if (!checkApiResult(apiResult)) return
        group.sendMessage(message.quote() + "重置成功!")
    }

    suspend fun resetArmPond() {
        if (!checkSuperManager()) return
        val apiResult = GachaUtil.resetArmPond()
        if (!checkApiResult(apiResult)) return
        group.sendMessage(message.quote() + "重置成功!")
    }

    suspend fun setSkinRate(msgContent: String, command: String) {
        if (!checkSuperManager()) return
        val skinRateStr = StringUtil.splitKeyWord(msgContent, command)
        if (skinRateStr?.toIntOrNull() == null) {
            group.sendMessage(message.quote() + "概率必须在0~100之间")
            return
        }
        val skinRate = skinRateStr.toInt()
        if (skinRate < 0 || skinRate > 100) {
            group.sendMessage(message.quote() + "概率必须在0~100之间")
            return
        }
        val apiResult = GachaUtil.setSkinRate(skinRate)
        if (!checkApiResult(apiResult)) return
        group.sendMessage(message.quote() + "设置成功!")
    }



}