package com.hamster.pray.genshin

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hamster.pray.genshin.data.*
import com.hamster.pray.genshin.util.HttpUtil
import com.hamster.pray.genshin.util.StringUtil
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.info
import okhttp3.*
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
        version = "1.0.1"
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
        logger.info("加载数据....")
        Config.reload()
        logger.info { "Plugin loaded" }
        //配置文件目录 "${dataFolder.absolutePath}/"
        val eventChannel = GlobalEventChannel.parentScope(this)
        eventChannel.subscribeAlways<GroupMessageEvent> {

            val client = OkHttpClient().newBuilder()
            .hostnameVerifier(RxUtils.TrustAllHostnameVerifier())
            .sslSocketFactory(RxUtils().createSSLSocketFactory(), RxUtils.TrustAllCerts())
            .build()

            var builder = Request.Builder()
            builder.addHeader("Content-Type", "application/json")
            builder.addHeader("authorzation", Config.authorzation)

            fun pray(url: String, prayMsg: (prayResult: PrayResult,upItem:String) -> String) {
                launch {
                    group.sendMessage(Config.prayingMsg)
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

            try {
                if (group.id !in Config.enabled_group) return@subscribeAlways
                if (!message.contentToString().startsWith(Config.prefix)) return@subscribeAlways
                val msgContent = message.contentToString().removePrefix(Config.prefix)

                if (msgContent.startsWith(Config.rolePrayOne)) {
                    val pondIndexstr = StringUtil.splitKeyWord(msgContent, Config.rolePrayOne);
                    var pondIndex = if (pondIndexstr.isNullOrEmpty()) 0 else pondIndexstr.toInt() - 1
                    if (pondIndex < 0) pondIndex = 0
                    val url = "${Config.apiUrl}/api/RolePray/PrayOne?memberCode=${sender.id}&memberName=${sender.nick}&pondIndex=${pondIndex}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        return "${if (apiData.star5Cost > 0) "本次5星累计消耗${apiData.star5Cost}抽，" else ""}当前卡池为：${upItem}，本次祈愿消耗${apiData.prayCount}个纠缠之缘，距离下次小保底还剩${apiData.role90Surplus}抽，大保底还剩${apiData.role180Surplus}抽".trimIndent()
                    }
                    pray(url, dlg)
                }
                if (msgContent.startsWith(Config.rolePrayTen)) {
                    val pondIndexstr = StringUtil.splitKeyWord(msgContent, Config.rolePrayTen);
                    var pondIndex = if (pondIndexstr.isNullOrEmpty()) 0 else pondIndexstr.toInt() - 1
                    if (pondIndex < 0) pondIndex = 0
                    val url = "${Config.apiUrl}/api/RolePray/PrayTen?memberCode=${sender.id}&memberName=${sender.nick}&pondIndex=${pondIndex}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        return "${if (apiData.star5Cost > 0) "本次5星累计消耗${apiData.star5Cost}抽，" else ""}当前卡池为：${upItem}，本次祈愿消耗${apiData.prayCount}个纠缠之缘，距离下次小保底还剩${apiData.role90Surplus}抽，大保底还剩${apiData.role180Surplus}抽".trimIndent()
                    }
                    pray(url, dlg)
                }

                if (msgContent.startsWith(Config.armPrayOne)) {
                    val url = "${Config.apiUrl}/api/ArmPray/PrayOne?memberCode=${sender.id}&memberName=${sender.nick}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        return "${if (apiData.star5Cost > 0) "本次5星累计消耗${apiData.star5Cost}抽，" else ""}当前卡池为：${upItem}，本次祈愿消耗${apiData.prayCount}个纠缠之缘，距离下次保底还剩${apiData.arm80Surplus}抽，当前命定值为：${apiData.armAssignValue}".trimIndent()
                    }
                    pray(url, dlg)
                }
                if (msgContent.startsWith(Config.armPrayTen)) {
                    val url = "${Config.apiUrl}/api/ArmPray/PrayTen?memberCode=${sender.id}&memberName=${sender.nick}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        return "${if (apiData.star5Cost > 0) "本次5星累计消耗${apiData.star5Cost}抽，" else ""}当前卡池为：${upItem}，本次祈愿消耗${apiData.prayCount}个纠缠之缘，距离下次保底还剩${apiData.arm80Surplus}抽，当前命定值为：${apiData.armAssignValue}".trimIndent()
                    }
                    pray(url, dlg)
                }

                if (msgContent.startsWith(Config.permPrayOne)) {
                    val url = "${Config.apiUrl}/api/PermPray/PrayOne?memberCode=${sender.id}&memberName=${sender.nick}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        return "${if (apiData.star5Cost > 0) "本次5星累计消耗${apiData.star5Cost}抽，" else ""}当前卡池为：${upItem}，本次祈愿消耗${apiData.prayCount}个相遇之缘，距离下次保底还剩${apiData.perm90Surplus}抽".trimIndent()
                    }
                    pray(url, dlg)
                }
                if (msgContent.startsWith(Config.permPrayTen)) {
                    val url = "${Config.apiUrl}/api/PermPray/PrayTen?memberCode=${sender.id}&memberName=${sender.nick}";
                    val dlg = fun(apiData: PrayResult, upItem: String): String {
                        return "${if (apiData.star5Cost > 0) "本次5星累计消耗${apiData.star5Cost}抽，" else ""}当前卡池为：${upItem}，本次祈愿消耗${apiData.prayCount}个相遇之缘，距离下次保底还剩${apiData.perm90Surplus}抽".trimIndent()
                    }
                    pray(url, dlg)
                }

                if (msgContent.startsWith(Config.assign)) {
                    val goodsName = StringUtil.splitKeyWord(msgContent, Config.assign);
                    val url = "${Config.apiUrl}/api/PermPray/PrayTen?memberCode=${sender.id}&memberName=${sender.nick}&goodsName=${goodsName}";
                    assign(url)
                }

                if (msgContent.startsWith(Config.getPondInfo)) {
                    val url = "${Config.apiUrl}/api/PrayInfo/GetPondInfo";
                    getPondInfo(url)
                }

                if (msgContent.startsWith(Config.getPrayDetail)) {
                    val url = "${Config.apiUrl}/api/PrayInfo/GetMemberPrayDetail?memberCode=${sender.id}";
                    getPrayDetail(url)
                }

                if (msgContent.startsWith(Config.getPrayRecords)) {
                    val url = "${Config.apiUrl}/api/PrayInfo/GetMemberPrayRecords?memberCode=${sender.id}";
                    getPrayRecord(url)
                }

                if (msgContent.startsWith(Config.getLuckRanking)) {
                    val url = "${Config.apiUrl}/api/PrayInfo/GetLuckRanking";
                    getLuckRanking(url)
                }
            } catch (e: Exception) {
                group.sendMessage(Config.errorMsg);
            } catch (e: Throwable) {
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
