package com.hamster.pray.genshin

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.hamster.pray.genshin.data.*
import com.hamster.pray.genshin.timer.AutoClearTask
import com.hamster.pray.genshin.util.DateUtil
import com.hamster.pray.genshin.util.HttpUtil
import com.hamster.pray.genshin.util.RxUtils
import com.hamster.pray.genshin.util.StringUtil
import com.sun.org.apache.xalan.internal.lib.ExsltDatetime.date
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.info
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


/**
 * 使用 kotlin 版请把
 * `src/main/resources/META-INF.services/net.mamoe.mirai.console.plugin.jvm.JvmPlugin`
 * 文件内容改成 `org.example.mirai.plugin.PluginMain` 也就是当前主类全类名
 *
 * 使用 kotlin 可以把 java 源集删除不会对项目有影响
 *
 * 在 `settings.gradle.kts` 里改构建的插件名称、依赖库和插件版本
 *
 * 在该示例下的 [JvmPluginDescription] 修改插件名称，id和版本，etc
 *
 * 可以使用 `src/test/kotlin/RunMirai.kt` 在 ide 里直接调试，
 * 不用复制到 mirai-console-loader 或其他启动器中调试
 */


object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "com.hamster.pray.genshin",
        name = "原神模拟抽卡",
        version = "1.2.0"
    ) {
        author("花园仓鼠")
        info(
            """
            一个调用https://github.com/GardenHamster/GenshinPray接口进行原神模拟抽卡的mirai插件
        """.trimIndent()
        )
    }
) {
    override fun onEnable() {
        logger.info("加载配置....")
        Config.reload()
        logger.info("加载数据....")
        PrayRecordData.reload()
        logger.info { "Plugin loaded" }
        val clearTask = AutoClearTask(logger, "${dataFolderPath}/download/")
        val clearStartDate = DateUtil.getHourStart(4)
        Timer().schedule(clearTask, clearStartDate, 24 * 60 * 60 * 1000)
        logger.info { "启动定时清理任务，每天凌晨4点自动清理历史下载图片..." }
        //配置文件目录 "${dataFolder.absolutePath}/"
        val eventChannel = GlobalEventChannel.parentScope(this)
        eventChannel.subscribeAlways<GroupMessageEvent> {

            val client = OkHttpClient().newBuilder()
            .hostnameVerifier(RxUtils.TrustAllHostnameVerifier())
            .sslSocketFactory(RxUtils().createSSLSocketFactory(), RxUtils.TrustAllCerts())
            .build()
            val JSON: MediaType = "application/json".toMediaType()

            var builder = Request.Builder()
            builder.addHeader("Content-Type", "application/json")
            builder.addHeader("authorzation", Config.authorzation)

            fun pray(memberCode: String, url: String, prayMsg: (prayResult: PrayResult, upItem: String) -> String) {
                if (PrayRecordData.isPrayUseUp(sender.id.toString())) {
                    launch{ group.sendMessage(message.quote() + Config.overLimitMsg) }
                    return
                }

                if(PrayCoolingData.isCooling(sender.id.toString())){
                    launch{ group.sendMessage(message.quote() + Config.coolingMsg.replace("{cdSeconds}",PrayCoolingData.getCoolingSecond(memberCode).toString())) }
                    return
                }

                PrayCoolingData.setCooling(sender.id.toString())

                launch {
                    if(!Config.prayingMsg.isNullOrEmpty()) group.sendMessage(Config.prayingMsg)
                }

                builder.url(url).get()
                client.newCall(builder.build()).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        launch {
                            group.sendMessage("${Config.errorMsg}，${e.message}")
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        launch {
                            try {
                                val type = object : TypeToken<ApiResult<PrayResult>>() {}.type
                                val apiResult = Gson().fromJson<ApiResult<PrayResult>>(response.body!!.string(), type)
                                val apiData = apiResult.data;
                                if (apiResult.code != 0) {
                                    group.sendMessage("${Config.errorMsg}，接口返回code：${apiResult.code}，接口返回message：${apiResult.message}")
                                    return@launch
                                }

                                var upItem = ""
                                for (item in apiData.star5Up) {
                                    if (upItem.isNotEmpty()) upItem += "+"
                                    upItem += item.goodsName
                                }

                                val imgSaveDir = "${dataFolderPath}/download/${SimpleDateFormat("yyyyMMdd").format(Date(System.currentTimeMillis()))}"
                                File(imgSaveDir).mkdirs()
                                val imgSavePath = "${imgSaveDir}/${SimpleDateFormat("HHmmSS").format(Date(System.currentTimeMillis()))}.jpg"
                                val imgMsg = HttpUtil.DownloadPicture(apiData.imgHttpUrl, imgSavePath).uploadAsImage(sender, "jpg")
                                group.sendMessage(message.quote() + prayMsg(apiData,upItem) + imgMsg);
                                PrayRecordData.addPrayRecord(sender.id.toString())
                            }
                            catch (e:Exception){
                                group.sendMessage("${Config.errorMsg}，${e.message}")
                            }
                        }
                    }
                })
            }

            fun assign(url: String) {
                builder.url(url).get()
                client.newCall(builder.build()).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        launch {
                            group.sendMessage("${Config.errorMsg}，接口异常了")
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        launch {
                            val type = object : TypeToken<ApiResult<Any>>() {}.type
                            val apiResult = Gson().fromJson<ApiResult<Any>>(response.body!!.string(), type)
                            if (apiResult.code != 0) {
                                group.sendMessage("${Config.errorMsg}，接口返回code：${apiResult.code}，接口返回message：${apiResult.message}")
                                return@launch
                            }
                            group.sendMessage(message.quote() + "定轨成功!");
                        }
                    }
                })
            }

            fun getPondInfo(url: String) {
                builder.url(url).get()
                client.newCall(builder.build()).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        launch {
                            group.sendMessage("${Config.errorMsg}，接口异常了")
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        launch {
                            val type = object : TypeToken<ApiResult<PondInfo>>() {}.type
                            val apiResult = Gson().fromJson<ApiResult<PondInfo>>(response.body!!.string(), type)
                            val apiData = apiResult.data;
                            if (apiResult.code != 0) {
                                group.sendMessage("${Config.errorMsg}，接口返回code：${apiResult.code}，接口返回message：${apiResult.message}")
                                return@launch
                            }
                            var msgInfo=StringBuilder()
                            var roleInfo=StringBuilder()
                            var armInfo=StringBuilder()

                            msgInfo.appendLine("目前up内容如下：")

                            for (item in apiData.role) {
                                roleInfo.appendLine(" 角色池${item.pondIndex+1}：")
                                for (star5 in item.pondInfo.star5UpList){
                                    roleInfo.appendLine("  5星：${star5.goodsName}")
                                }
                                for (star4 in item.pondInfo.star4UpList){
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

                            group.sendMessage(message.quote() + msgInfo.toString() + roleInfo.toString() + armInfo.toString());
                        }
                    }
                })
            }

            fun getPrayDetail(url: String) {
                builder.url(url).get()
                client.newCall(builder.build()).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        launch {
                            group.sendMessage("${Config.errorMsg}，接口异常了")
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        launch {
                            val type = object : TypeToken<ApiResult<PrayDetail>>() {}.type
                            val apiResult = Gson().fromJson<ApiResult<PrayDetail>>(response.body!!.string(), type)
                            val apiData = apiResult.data;
                            if (apiResult.code != 0) {
                                group.sendMessage("${Config.errorMsg}，接口返回code：${apiResult.code}，接口返回message：${apiResult.message}")
                                return@launch
                            }
                            var msgInfo=StringBuilder()
                            msgInfo.appendLine("你的祈愿详情如下：")
                            msgInfo.appendLine("角色池大保底剩余抽数：${apiData.role180Surplus}");
                            msgInfo.appendLine("角色池小保底剩余抽数：${apiData.role90Surplus}");
                            msgInfo.appendLine("武器池命定值：${apiData.armAssignValue}");
                            msgInfo.appendLine("武器池保底剩余抽数：${apiData.arm80Surplus}");
                            msgInfo.appendLine("常驻池保底剩余抽数：${apiData.perm90Surplus}");
                            msgInfo.appendLine("角色池累计抽取次数：${apiData.rolePrayTimes}");
                            msgInfo.appendLine("武器池累计抽取次数：${apiData.armPrayTimes}");
                            msgInfo.appendLine("常驻池累计抽取次数：${apiData.permPrayTimes}");
                            msgInfo.appendLine("所有池累计抽取次数：${apiData.totalPrayTimes}");
                            msgInfo.appendLine("累计获得5星数量${apiData.star5Count}");
                            msgInfo.appendLine("累计获得4星数量：${apiData.star4Count}");
                            msgInfo.appendLine("5星出率：${apiData.star5Rate}%");
                            msgInfo.appendLine("4星出率：${apiData.star4Rate}%");
                            group.sendMessage(message.quote() + msgInfo.toString());
                        }
                    }
                })
            }

            fun getPrayRecord(url: String) {
                builder.url(url).get()
                client.newCall(builder.build()).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        launch {
                            group.sendMessage("${Config.errorMsg}，接口异常了")
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        launch {
                            val type = object : TypeToken<ApiResult<PrayRecord>>() {}.type
                            val apiResult = Gson().fromJson<ApiResult<PrayRecord>>(response.body!!.string(), type)
                            val apiData = apiResult.data;
                            if (apiResult.code != 0) {
                                group.sendMessage("${Config.errorMsg}，接口返回code：${apiResult.code}，接口返回message：${apiResult.message}")
                                return@launch
                            }

                            var msgInfo=StringBuilder()
                            var star5Info=StringBuilder()

                            msgInfo.appendLine("祈愿记录如下：")

                            star5Info.appendLine("5星列表：")
                            star5Info.appendLine("物品[消耗抽数]获取时间")
                            for (item in apiData.star5.all) {
                                star5Info.appendLine("${item.goodsName}[${item.cost}]${item.createDate}")
                            }

                            group.sendMessage(message.quote() + msgInfo.toString() + star5Info.toString());
                        }
                    }
                })
            }

            fun getLuckRanking(url: String) {
                builder.url(url).get()
                client.newCall(builder.build()).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        launch {
                            group.sendMessage("${Config.errorMsg}，接口异常了")
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        launch {
                            val type = object : TypeToken<ApiResult<LuckRanking>>() {}.type
                            val apiResult = Gson().fromJson<ApiResult<LuckRanking>>(response.body!!.string(), type)
                            val apiData = apiResult.data;
                            if (apiResult.code != 0) {
                                group.sendMessage("${Config.errorMsg}，接口返回code：${apiResult.code}，接口返回message：${apiResult.message}")
                                return@launch
                            }

                            var msgInfo=StringBuilder()
                            var star5Info=StringBuilder()

                            msgInfo.appendLine("出货率最高的前${apiData.top}名成员如下，统计开始日期：${apiData.startDate}，排行结果每5分钟缓存一次")
                            star5Info.appendLine("名称(id)[5星数量/累计抽数=5星出率]")
                            for (item in apiData.star5Ranking) {
                                star5Info.appendLine("  ${item.memberName}(${item.memberCode})[${item.count}/${item.totalPrayTimes}=${item.rate}%]")
                            }

                            group.sendMessage(message.quote() + msgInfo.toString() + star5Info.toString());
                        }
                    }
                })
            }

            fun setRolePond(url: String, pondIndex: Int, upItems: Array<String>) {
                var params: MutableMap<String, Any> = mutableMapOf<String, Any>()
                params.set("pondIndex", pondIndex)
                params.set("upItems",upItems)

                val jsonStr = GsonBuilder().create().toJson(params)
                val contentType: MediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonStr.toRequestBody(contentType)

                builder.url(url).post(requestBody)
                client.newCall(builder.build()).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        launch {
                            group.sendMessage("${Config.errorMsg}，接口异常了")
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        launch {
                            val type = object : TypeToken<ApiResult<Any>>() {}.type
                            val apiResult = Gson().fromJson<ApiResult<Any>>(response.body!!.string(), type)
                            if (apiResult.code != 0) {
                                group.sendMessage("${Config.errorMsg}，接口返回code：${apiResult.code}，接口返回message：${apiResult.message}")
                                return@launch
                            }
                            group.sendMessage(message.quote() + "配置成功!");
                        }
                    }
                })
            }

            fun setArmPond(url: String, upItems: Array<String>) {
                var params: MutableMap<String, Any> = mutableMapOf<String, Any>()
                params.set("upItems",upItems)

                val jsonStr = GsonBuilder().create().toJson(params)
                val contentType: MediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonStr.toRequestBody(contentType)

                builder.url(url).post(requestBody)
                client.newCall(builder.build()).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        launch {
                            group.sendMessage("${Config.errorMsg}，接口异常了")
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        launch {
                            val type = object : TypeToken<ApiResult<Any>>() {}.type
                            val apiResult = Gson().fromJson<ApiResult<Any>>(response.body!!.string(), type)
                            if (apiResult.code != 0) {
                                group.sendMessage("${Config.errorMsg}，接口返回code：${apiResult.code}，接口返回message：${apiResult.message}")
                                return@launch
                            }
                            group.sendMessage(message.quote() + "配置成功!");
                        }
                    }
                })
            }

            fun resetPond(url: String) {
                var params: MutableMap<String, Any> = mutableMapOf<String, Any>()
                val jsonStr = GsonBuilder().create().toJson(params)
                val contentType: MediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonStr.toRequestBody(contentType)

                builder.url(url).post(requestBody)
                client.newCall(builder.build()).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        launch {
                            group.sendMessage("${Config.errorMsg}，接口异常了")
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        launch {
                            val type = object : TypeToken<ApiResult<Any>>() {}.type
                            val apiResult = Gson().fromJson<ApiResult<Any>>(response.body!!.string(), type)
                            if (apiResult.code != 0) {
                                group.sendMessage("${Config.errorMsg}，接口返回code：${apiResult.code}，接口返回message：${apiResult.message}")
                                return@launch
                            }
                            group.sendMessage(message.quote() + "重置成功!");
                        }
                    }
                })
            }

            fun setSkinRate(url: String) {
                var params: MutableMap<String, Any> = mutableMapOf<String, Any>()
                val jsonStr = GsonBuilder().create().toJson(params)
                val contentType: MediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonStr.toRequestBody(contentType)

                builder.url(url).post(requestBody)
                client.newCall(builder.build()).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        launch {
                            group.sendMessage("${Config.errorMsg}，接口异常了")
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        launch {
                            val type = object : TypeToken<ApiResult<Any>>() {}.type
                            val apiResult = Gson().fromJson<ApiResult<Any>>(response.body!!.string(), type)
                            if (apiResult.code != 0) {
                                group.sendMessage("${Config.errorMsg}，接口返回code：${apiResult.code}，接口返回message：${apiResult.message}")
                                return@launch
                            }
                            group.sendMessage(message.quote() + "设置成功!");
                        }
                    }
                })
            }

            fun sendMenuMsg() {
                launch {
                    group.sendMessage(message.quote() + Config.menuMsg);
                }
            }

            try {
                if (group.id !in Config.enabled_group) return@subscribeAlways

                val atStr = "@${bot.id}"
                val contentStr = message.contentToString()

                var msgContent = ""
                if (contentStr.startsWith(atStr)) {
                    msgContent = contentStr.removePrefix(atStr).trim()
                } else if (contentStr.startsWith(Config.prefix)) {
                    msgContent = contentStr.removePrefix(Config.prefix).trim()
                }

                if (msgContent.trim().isEmpty()) return@subscribeAlways

                if (msgContent.startsWith(Config.setRolePond)) {
                    if (sender.id !in Config.super_manager){
                        group.sendMessage(message.quote() + "该指令需要管理员执行")
                        return@subscribeAlways
                    }
                    val paramStr = StringUtil.splitKeyWord(msgContent, Config.setRolePond)
                    var paramArr = paramStr?.trim()?.split("[,， ]+".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
                    if (paramArr == null) {
                        group.sendMessage(message.quote() + "格式错误，请参考格式：#设定角色池[编号(1~10,或者可以不指定)] 雷电将军，五郎，云堇，香菱")
                        return@subscribeAlways
                    }
                    var pondIndex = paramArr[0]?.toIntOrNull()
                    if (pondIndex != null) paramArr = paramArr.copyOfRange(1, paramArr.count())
                    if (paramArr.count() != 4) {
                        group.sendMessage(message.quote() + "必须指定1个五星和3个四星角色")
                        return@subscribeAlways
                    }
                    if (pondIndex == null) pondIndex = 0
                    if (pondIndex < 0 || pondIndex > 10) {
                        group.sendMessage(message.quote() + "蛋池编号只能设定在1~10之间")
                        return@subscribeAlways
                    }
                    pondIndex = if (pondIndex - 1 < 0) 0 else pondIndex - 1
                    val url = "${Config.apiUrl}/api/PrayInfo/SetRolePond"
                    setRolePond(url, pondIndex, paramArr)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.setArmPond)) {
                    if (sender.id !in Config.super_manager){
                        group.sendMessage(message.quote() + "该指令需要管理员执行")
                        return@subscribeAlways
                    }
                    val paramStr = StringUtil.splitKeyWord(msgContent, Config.setArmPond)
                    var paramArr = paramStr?.trim()?.split("[,， ]+".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
                    if (paramArr == null) {
                        group.sendMessage(message.quote() + "格式错误，请参考格式：#设定武器池 薙草之稻光 不灭月华 恶王丸 曚云之月 匣里龙吟 西风长枪 祭礼残章")
                        return@subscribeAlways
                    }
                    if (paramArr.count() != 7) {
                        group.sendMessage(message.quote() + "必须指定2件五星和5件四星武器")
                        return@subscribeAlways
                    }
                    val url = "${Config.apiUrl}/api/PrayInfo/SetArmPond"
                    setArmPond(url, paramArr)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.resetRolePond)) {
                    if (sender.id !in Config.super_manager){
                        group.sendMessage(message.quote() + "该指令需要管理员执行")
                        return@subscribeAlways
                    }
                    val url = "${Config.apiUrl}/api/PrayInfo/ResetRolePond"
                    resetPond(url)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.resetArmPond)) {
                    if (sender.id !in Config.super_manager){
                        group.sendMessage(message.quote() + "该指令需要管理员执行")
                        return@subscribeAlways
                    }
                    val url = "${Config.apiUrl}/api/PrayInfo/ResetArmPond";
                    resetPond(url)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.setSkinRate)) {
                    if (sender.id !in Config.super_manager){
                        group.sendMessage(message.quote() + "该指令需要管理员执行")
                        return@subscribeAlways
                    }
                    val skinRateStr = StringUtil.splitKeyWord(msgContent, Config.setSkinRate)
                    if (skinRateStr?.toIntOrNull() == null) {
                        group.sendMessage(message.quote() + "概率必须在0~100之间")
                        return@subscribeAlways
                    }
                    val skinRate = skinRateStr.toInt()
                    if (skinRate < 0 || skinRate > 100) {
                        group.sendMessage(message.quote() + "概率必须在0~100之间")
                        return@subscribeAlways
                    }
                    val url = "${Config.apiUrl}/api/PrayInfo/SetSkinRate?rare=${skinRate}"
                    setSkinRate(url)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.rolePrayOne)) {
                    val pondIndexstr = StringUtil.splitKeyWord(msgContent, Config.rolePrayOne)
                    if (pondIndexstr.isNullOrEmpty() == false && pondIndexstr?.toIntOrNull() == null) {
                        group.sendMessage(message.quote() + "指定的蛋池编号无效")
                        return@subscribeAlways
                    }
                    var pondIndex = if (pondIndexstr.isNullOrEmpty()) 0 else pondIndexstr.toInt() - 1
                    if (pondIndex < 0) pondIndex = 0
                    val url = "${Config.apiUrl}/api/RolePray/PrayOne?memberCode=${sender.id}&memberName=${sender.nick}&pondIndex=${pondIndex}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        val warnMsgBuilder = StringBuilder()
                        if (apiData.star5Cost > 0) warnMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
                        warnMsgBuilder.append("当前卡池为：${upItem}，")
                        warnMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
                        warnMsgBuilder.append("距离下次小保底还剩${apiData.role90Surplus}抽，")
                        warnMsgBuilder.append("大保底还剩${apiData.role180Surplus}抽")
                        var surplusTimes = PrayRecordData.getSurplusTimes(sender.id.toString()) - 1
                        surplusTimes = if (surplusTimes < 0) 0 else surplusTimes
                        if (Config.dailyLimit>0) warnMsgBuilder.append("，今日剩余可用抽卡次数${surplusTimes}次")
                        if (Config.prayCDSeconds>0)warnMsgBuilder.append("，CD${Config.prayCDSeconds}秒")
                        return warnMsgBuilder.toString().trimIndent()
                    }
                    pray(sender.id.toString(), url, dlg)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.rolePrayTen)) {
                    val pondIndexstr = StringUtil.splitKeyWord(msgContent, Config.rolePrayTen);
                    if (pondIndexstr.isNullOrEmpty() == false && pondIndexstr?.toIntOrNull() == null) {
                        group.sendMessage(message.quote() + "指定的蛋池编号无效")
                        return@subscribeAlways
                    }
                    var pondIndex = if (pondIndexstr.isNullOrEmpty()) 0 else pondIndexstr.toInt() - 1
                    if (pondIndex < 0) pondIndex = 0
                    val url = "${Config.apiUrl}/api/RolePray/PrayTen?memberCode=${sender.id}&memberName=${sender.nick}&pondIndex=${pondIndex}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        val warnMsgBuilder = StringBuilder()
                        if (apiData.star5Cost > 0) warnMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
                        warnMsgBuilder.append("当前卡池为：${upItem}，")
                        warnMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
                        warnMsgBuilder.append("距离下次小保底还剩${apiData.role90Surplus}抽，")
                        warnMsgBuilder.append("大保底还剩${apiData.role180Surplus}抽")
                        var surplusTimes = PrayRecordData.getSurplusTimes(sender.id.toString()) - 1
                        surplusTimes = if (surplusTimes < 0) 0 else surplusTimes
                        if (Config.dailyLimit>0) warnMsgBuilder.append("，今日剩余可用抽卡次数${surplusTimes}次")
                        if (Config.prayCDSeconds>0)warnMsgBuilder.append("，CD${Config.prayCDSeconds}秒")
                        return warnMsgBuilder.toString().trimIndent()
                    }
                    pray(sender.id.toString(), url, dlg)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.armPrayOne)) {
                    val url = "${Config.apiUrl}/api/ArmPray/PrayOne?memberCode=${sender.id}&memberName=${sender.nick}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        val warnMsgBuilder = StringBuilder()
                        if (apiData.star5Cost > 0) warnMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
                        warnMsgBuilder.append("当前卡池为：${upItem}，")
                        warnMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
                        warnMsgBuilder.append("距离下次保底还剩${apiData.arm80Surplus}抽，")
                        warnMsgBuilder.append("当前命定值为：${apiData.armAssignValue}")
                        var surplusTimes = PrayRecordData.getSurplusTimes(sender.id.toString()) - 1
                        surplusTimes = if (surplusTimes < 0) 0 else surplusTimes
                        if (Config.dailyLimit>0) warnMsgBuilder.append("，今日剩余可用抽卡次数${surplusTimes}次")
                        if (Config.prayCDSeconds>0)warnMsgBuilder.append("，CD${Config.prayCDSeconds}秒")
                        return warnMsgBuilder.toString().trimIndent()
                    }
                    pray(sender.id.toString(), url, dlg)
                    return@subscribeAlways
                }
                if (msgContent.startsWith(Config.armPrayTen)) {
                    val url = "${Config.apiUrl}/api/ArmPray/PrayTen?memberCode=${sender.id}&memberName=${sender.nick}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        val warnMsgBuilder = StringBuilder()
                        if (apiData.star5Cost > 0) warnMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
                        warnMsgBuilder.append("当前卡池为：${upItem}，")
                        warnMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
                        warnMsgBuilder.append("距离下次保底还剩${apiData.arm80Surplus}抽，")
                        warnMsgBuilder.append("当前命定值为：${apiData.armAssignValue}")
                        var surplusTimes = PrayRecordData.getSurplusTimes(sender.id.toString()) - 1
                        surplusTimes = if (surplusTimes < 0) 0 else surplusTimes
                        if (Config.dailyLimit>0) warnMsgBuilder.append("，今日剩余可用抽卡次数${surplusTimes}次")
                        if (Config.prayCDSeconds>0)warnMsgBuilder.append("，CD${Config.prayCDSeconds}秒")
                        return warnMsgBuilder.toString().trimIndent()
                    }
                    pray(sender.id.toString(), url, dlg)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.permPrayOne)) {
                    val url = "${Config.apiUrl}/api/PermPray/PrayOne?memberCode=${sender.id}&memberName=${sender.nick}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        val warnMsgBuilder = StringBuilder()
                        if (apiData.star5Cost > 0) warnMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
                        warnMsgBuilder.append("当前卡池为：${upItem}，")
                        warnMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个相遇之缘，")
                        warnMsgBuilder.append("距离下次保底还剩${apiData.perm90Surplus}抽，")
                        var surplusTimes = PrayRecordData.getSurplusTimes(sender.id.toString()) - 1
                        surplusTimes = if (surplusTimes < 0) 0 else surplusTimes
                        if (Config.dailyLimit>0) warnMsgBuilder.append("，今日剩余可用抽卡次数${surplusTimes}次")
                        if (Config.prayCDSeconds>0)warnMsgBuilder.append("，CD${Config.prayCDSeconds}秒")
                        return warnMsgBuilder.toString().trimIndent()
                    }
                    pray(sender.id.toString(), url, dlg)
                    return@subscribeAlways
                }
                if (msgContent.startsWith(Config.permPrayTen)) {
                    val url = "${Config.apiUrl}/api/PermPray/PrayTen?memberCode=${sender.id}&memberName=${sender.nick}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        val warnMsgBuilder = StringBuilder()
                        if (apiData.star5Cost > 0) warnMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
                        warnMsgBuilder.append("当前卡池为：${upItem}，")
                        warnMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个相遇之缘，")
                        warnMsgBuilder.append("距离下次保底还剩${apiData.perm90Surplus}抽，")
                        var surplusTimes = PrayRecordData.getSurplusTimes(sender.id.toString()) - 1
                        surplusTimes = if (surplusTimes < 0) 0 else surplusTimes
                        if (Config.dailyLimit>0) warnMsgBuilder.append("，今日剩余可用抽卡次数${surplusTimes}次")
                        if (Config.prayCDSeconds>0)warnMsgBuilder.append("，CD${Config.prayCDSeconds}秒")
                        return warnMsgBuilder.toString().trimIndent()
                    }
                    pray(sender.id.toString(), url, dlg)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.fullRolePrayOne)) {
                    val url = "${Config.apiUrl}/api/FullRolePray/PrayOne?memberCode=${sender.id}&memberName=${sender.nick}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        val warnMsgBuilder = StringBuilder()
                        if (apiData.star5Cost > 0) warnMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
                        warnMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
                        warnMsgBuilder.append("距离下次保底还剩${apiData.fullRole90Surplus}抽")
                        var surplusTimes = PrayRecordData.getSurplusTimes(sender.id.toString()) - 1
                        surplusTimes = if (surplusTimes < 0) 0 else surplusTimes
                        if (Config.dailyLimit>0) warnMsgBuilder.append("，今日剩余可用抽卡次数${surplusTimes}次")
                        if (Config.prayCDSeconds>0)warnMsgBuilder.append("，CD${Config.prayCDSeconds}秒")
                        return warnMsgBuilder.toString().trimIndent()
                    }
                    pray(sender.id.toString(), url, dlg)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.fullRolePrayTen)) {
                    val url = "${Config.apiUrl}/api/FullRolePray/PrayTen?memberCode=${sender.id}&memberName=${sender.nick}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        val warnMsgBuilder = StringBuilder()
                        if (apiData.star5Cost > 0) warnMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
                        warnMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
                        warnMsgBuilder.append("距离下次保底还剩${apiData.fullRole90Surplus}抽")
                        var surplusTimes = PrayRecordData.getSurplusTimes(sender.id.toString()) - 1
                        surplusTimes = if (surplusTimes < 0) 0 else surplusTimes
                        if (Config.dailyLimit>0) warnMsgBuilder.append("，今日剩余可用抽卡次数${surplusTimes}次")
                        if (Config.prayCDSeconds>0)warnMsgBuilder.append("，CD${Config.prayCDSeconds}秒")
                        return warnMsgBuilder.toString().trimIndent()
                    }
                    pray(sender.id.toString(), url, dlg)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.fullArmPrayOne)) {
                    val url = "${Config.apiUrl}/api/FullArmPray/PrayOne?memberCode=${sender.id}&memberName=${sender.nick}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        val warnMsgBuilder = StringBuilder()
                        if (apiData.star5Cost > 0) warnMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
                        warnMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
                        warnMsgBuilder.append("距离下次保底还剩${apiData.fullArm80Surplus}抽")
                        var surplusTimes = PrayRecordData.getSurplusTimes(sender.id.toString()) - 1
                        surplusTimes = if (surplusTimes < 0) 0 else surplusTimes
                        if (Config.dailyLimit>0) warnMsgBuilder.append("，今日剩余可用抽卡次数${surplusTimes}次")
                        if (Config.prayCDSeconds>0)warnMsgBuilder.append("，CD${Config.prayCDSeconds}秒")
                        return warnMsgBuilder.toString().trimIndent()
                    }
                    pray(sender.id.toString(), url, dlg)
                    return@subscribeAlways
                }
                if (msgContent.startsWith(Config.fullArmPrayTen)) {
                    val url = "${Config.apiUrl}/api/FullArmPray/PrayTen?memberCode=${sender.id}&memberName=${sender.nick}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        val warnMsgBuilder = StringBuilder()
                        if (apiData.star5Cost > 0) warnMsgBuilder.append("本次5星累计消耗${apiData.star5Cost}抽，")
                        warnMsgBuilder.append("本次祈愿消耗${apiData.prayCount}个纠缠之缘，")
                        warnMsgBuilder.append("距离下次保底还剩${apiData.fullArm80Surplus}抽")
                        var surplusTimes = PrayRecordData.getSurplusTimes(sender.id.toString()) - 1
                        surplusTimes = if (surplusTimes < 0) 0 else surplusTimes
                        if (Config.dailyLimit>0) warnMsgBuilder.append("，今日剩余可用抽卡次数${surplusTimes}次")
                        if (Config.prayCDSeconds>0)warnMsgBuilder.append("，CD${Config.prayCDSeconds}秒")
                        return warnMsgBuilder.toString().trimIndent()
                    }
                    pray(sender.id.toString(), url, dlg)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.assign)) {
                    val goodsName = StringUtil.splitKeyWord(msgContent, Config.assign);
                    if (goodsName.isNullOrEmpty() || goodsName.isNullOrBlank()) {
                        group.sendMessage(message.quote() + "格式错误，请参考格式：#定轨薙草之稻光")
                        return@subscribeAlways
                    }
                    val url = "${Config.apiUrl}/api/PrayInfo/SetMemberAssign?memberCode=${sender.id}&memberName=${sender.nick}&goodsName=${goodsName}";
                    assign(url)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.getPondInfo)) {
                    val url = "${Config.apiUrl}/api/PrayInfo/GetPondInfo";
                    getPondInfo(url)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.getPrayDetail)) {
                    val url = "${Config.apiUrl}/api/PrayInfo/GetMemberPrayDetail?memberCode=${sender.id}";
                    getPrayDetail(url)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.getPrayRecords)) {
                    val url = "${Config.apiUrl}/api/PrayInfo/GetMemberPrayRecords?memberCode=${sender.id}";
                    getPrayRecord(url)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.getLuckRanking)) {
                    val url = "${Config.apiUrl}/api/PrayInfo/GetLuckRanking";
                    getLuckRanking(url)
                    return@subscribeAlways
                }

                if (Config.menu != null && Config.menu.count() > 0) {
                    for (item in Config.menu) {
                        if (msgContent.contains(item)) {
                            sendMenuMsg()
                            return@subscribeAlways
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                group.sendMessage(Config.errorMsg);
            } catch (e: Throwable) {
                e.printStackTrace()
                group.sendMessage(Config.errorMsg);
            }
        }
        eventChannel.subscribeAlways<FriendMessageEvent> {

        }
        eventChannel.subscribeAlways<NewFriendRequestEvent> {

        }
        eventChannel.subscribeAlways<BotInvitedJoinGroupRequestEvent> {

        }
    }
}
