package com.hamster.pray.genshin.data

import kotlinx.serialization.Serializable

@Serializable
data class PrayResult(
    val prayCount: Int,
    val role180Surplus: Int,
    val role90Surplus: Int,
    val arm80Surplus: Int,
    val armAssignValue: Int,
    val perm90Surplus: Int,
    val fullRole90Surplus: Int,
    val fullArm80Surplus: Int,
    val surplus10: Int,
    val star5Cost: Int,
    val apiDailyCallSurplus: Int,
    val imgHttpUrl: String,
    val imgSize: Int,
    val imgPath: String,
    val imgBase64: String,
    val star3Goods:List<GoodsInfo>,
    val star4Goods:List<GoodsInfo>,
    val star5Goods:List<GoodsInfo>,
    val star4Up:List<GoodsInfo>,
    val star5Up:List<GoodsInfo>,
)