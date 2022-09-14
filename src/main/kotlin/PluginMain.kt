package com.hamster.pray.genshin

import com.hamster.pray.genshin.cache.PrayRecordCache
import com.hamster.pray.genshin.config.Config
import com.hamster.pray.genshin.data.*
import com.hamster.pray.genshin.handler.GachaHaldler
import com.hamster.pray.genshin.timer.AutoClearTask
import com.hamster.pray.genshin.util.DateUtil
import com.hamster.pray.genshin.util.StringUtil
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.info
import okhttp3.*
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
        version = "1.3.0"
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
        PrayRecordCache.reload()
        logger.info { "Plugin loaded" }
        val clearTask = AutoClearTask(logger, "${dataFolderPath}/download/")
        val clearStartDate = DateUtil.getHourStart(4)
        Timer().schedule(clearTask, clearStartDate, 24 * 60 * 60 * 1000)
        logger.info { "启动定时清理任务，每天凌晨4点自动清理历史下载图片..." }
        //配置文件目录 "${dataFolder.absolutePath}/"
        val eventChannel = GlobalEventChannel.parentScope(this)
        eventChannel.subscribeAlways<GroupMessageEvent> {
            try {
                if (group.id !in Config.enabled_group) return@subscribeAlways
                val gaucheHandler= GachaHaldler(group,sender,message)
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
                    gaucheHandler.setRolePond(msgContent, Config.setRolePond)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.setArmPond)) {
                    gaucheHandler.setArmPond(msgContent, Config.setArmPond)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.resetRolePond)) {
                    gaucheHandler.resetRolePond()
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.resetArmPond)) {
                    gaucheHandler.resetArmPond()
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.setSkinRate)) {
                    gaucheHandler.setSkinRate(msgContent, Config.setArmPond)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.rolePrayOne)) {
                    gaucheHandler.rolePrayOne(msgContent, Config.rolePrayOne)
                    return@subscribeAlways
                }
                if (msgContent.startsWith(Config.rolePrayTen)) {
                    gaucheHandler.rolePrayTen(msgContent, Config.rolePrayTen)
                    return@subscribeAlways
                }
                if (msgContent.startsWith(Config.armPrayOne)) {
                    gaucheHandler.armPrayOne()
                    return@subscribeAlways
                }
                if (msgContent.startsWith(Config.armPrayTen)) {
                    gaucheHandler.armPrayTen()
                    return@subscribeAlways
                }
                if (msgContent.startsWith(Config.permPrayOne)) {
                    gaucheHandler.permPrayOne()
                    return@subscribeAlways
                }
                if (msgContent.startsWith(Config.permPrayTen)) {
                    gaucheHandler.permPrayTen()
                    return@subscribeAlways
                }
                if (msgContent.startsWith(Config.fullRolePrayOne)) {
                    gaucheHandler.fullRolePrayOne()
                    return@subscribeAlways
                }
                if (msgContent.startsWith(Config.fullRolePrayTen)) {
                    gaucheHandler.fullRolePrayTen()
                    return@subscribeAlways
                }
                if (msgContent.startsWith(Config.fullArmPrayOne)) {
                    gaucheHandler.fullArmPrayOne()
                    return@subscribeAlways
                }
                if (msgContent.startsWith(Config.fullArmPrayTen)) {
                    gaucheHandler.fullArmPrayTen()
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.assign)) {
                    gaucheHandler.assign(msgContent, Config.assign)
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.getPondInfo)) {
                    gaucheHandler.getPondInfo()
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.getPrayDetail)) {
                    gaucheHandler.getPrayDetail()
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.getPrayRecords)) {
                    gaucheHandler.getPrayRecord()
                    return@subscribeAlways
                }

                if (msgContent.startsWith(Config.getLuckRanking)) {
                    gaucheHandler.getLuckRanking()
                    return@subscribeAlways
                }

                if (Config.menu != null && Config.menu.count() > 0) {
                    for (item in Config.menu) {
                        if (msgContent.contains(item)) {
                            group.sendMessage(message.quote() + Config.menuMsg);
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
