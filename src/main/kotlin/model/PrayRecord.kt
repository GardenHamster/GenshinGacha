package com.hamster.pray.genshin.model

import com.hamster.pray.genshin.data.PondIndex
import kotlinx.serialization.Serializable
import java.util.*


class PrayRecord constructor(memberCode: String, createTime: Long = Date().time) {
    val memberCode = memberCode
    val createTime = createTime
}