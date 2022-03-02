package com.hamster.pray.genshin.util

import org.apache.http.client.fluent.Request
import java.io.File


class StringUtil {
    companion object {
        fun splitKeyWord(content: String, keyWord: String): String? {
            if (content.isNullOrEmpty()) return null
            val list = content.split(keyWord)
            if (list.count() <= 1) return null
            return list[1]
        }
    }
}