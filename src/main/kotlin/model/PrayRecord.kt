package com.hamster.pray.genshin.model

import com.hamster.pray.genshin.data.PondIndex
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class PrayRecord() {
    var memberCode: String = ""
    var createTime: Long = Date().time

    constructor(memberCode: String) : this() {
        this.memberCode = memberCode
        this.createTime = Date().time
    }

}