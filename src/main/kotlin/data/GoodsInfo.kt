package com.hamster.pray.genshin.data

import kotlinx.serialization.Serializable

@Serializable
data class GoodsInfo(
    val goodsName: String,
    val goodsType: String,
    val goodsSubType: String,
    val rareType: String
)