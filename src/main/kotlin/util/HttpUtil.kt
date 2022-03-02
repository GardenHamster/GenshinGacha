package com.hamster.pray.genshin.util

import org.apache.http.client.fluent.Request
import java.io.File


class HttpUtil {
    companion object {
        fun DownloadPicture(url: String, savePath: String): File {
            val file = File(savePath)
            Request.Get(url)
                //建立连接的超时时间
                .connectTimeout(30 * 1000)
                //客户端和服务进行数据包交互的间隔超时时间
                .socketTimeout(30 * 1000)
                //执行
                .execute()
                //存储文件
                .saveContent(file)
            return file
        }
    }
}