package com.hamster.pray.genshin.util

class StringUtil {
    companion object {
        fun splitKeyWord(content: String, keyWord: String): String? {
            if (content.isBlank()) return null
            val list = content.split(keyWord)
            if (list.count() <= 1) return null
            return list[1].trim()
        }
    }
}