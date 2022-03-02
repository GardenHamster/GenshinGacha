package com.hamster.pray.genshin.data

import kotlinx.serialization.Serializable

@Serializable
data class PondInfo(
    val arm: List<PondIndex>,
    val role: List<PondIndex>,
    val perm: List<PondIndex>
)

@Serializable
data class PondIndex(
    val pondIndex: Int,
    val pondInfo: PondDetail
)

@Serializable
data class PondDetail(
    val star5UpList: List<GoodsInfo>,
    val star4UpList: List<GoodsInfo>,
)