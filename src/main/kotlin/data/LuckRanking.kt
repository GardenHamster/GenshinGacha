package com.hamster.pray.genshin.data

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class LuckRanking(
    val star5Ranking: List<RankingDetail>,
    val star4Ranking: List<RankingDetail>,
    val startDate: String,
    val endDate: String,
    val cacheDate: String,
    val top: Int,
)

@Serializable
data class RankingDetail(
    val memberCode: String,
    val memberName: String,
    val count: Int,
    val totalPrayTimes: Int,
    val rate: Double,
)
