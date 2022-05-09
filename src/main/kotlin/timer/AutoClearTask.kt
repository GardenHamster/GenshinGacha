package com.hamster.pray.genshin.timer

import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.info
import java.io.File
import java.util.*

class AutoClearTask(val logger: MiraiLogger, val clearPath: String) : TimerTask() {
    override fun run() {
        try {
            delDir(clearPath)
            logger.info { "历史下载图片清理完毕..." }
        } catch (e: Exception) {
            logger.error("历史下载图片清理失败，${e.message}")
        }
    }

    fun delDir(dirpath: String) {
        val dir = File(dirpath)
        deleteDirWihtFile(dir)
    }

    fun deleteDirWihtFile(dir: File?) {
        if (dir!!.checkFile()) return
        for (file in dir.listFiles()) {
            if (file.isFile)
                file.delete() // 删除所有文件
            else if (file.isDirectory)
                deleteDirWihtFile(file) // 递规的方式删除文件夹
        }
        dir.delete()// 删除目录本身
    }

    private fun File.checkFile(): Boolean {
        return this == null || !this.exists() || !this.isDirectory
    }


}