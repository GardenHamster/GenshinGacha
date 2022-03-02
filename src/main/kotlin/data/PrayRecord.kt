package com.hamster.pray.genshin.data

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class PrayRecord(
    val star5: StarInfo,
    val star4: StarInfo
)

@Serializable
data class StarInfo(
    val arm: List<RecordDetail>,
    val role: List<RecordDetail>,
    val perm: List<RecordDetail>,
    val all: List<RecordDetail>
)

@Serializable
data class RecordDetail(
    val goodsName: String,
    val goodsType: String,
    val goodsSubType: String,
    val rareType: String,
    val cost: Int,
    val createDate: String
)