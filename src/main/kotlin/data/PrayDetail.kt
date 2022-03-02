package com.hamster.pray.genshin.data

import kotlinx.serialization.Serializable

@Serializable
data class PrayDetail(
    val role180Surplus: Int,
    val role90Surplus: Int,
    val role10Surplus: Int,
    val armAssignValue: Int,
    val arm80Surplus: Int,
    val arm10Surplus: Int,
    val perm90Surplus: Int,
    val perm10Surplus: Int,
    val rolePrayTimes: Int,
    val armPrayTimes: Int,
    val permPrayTimes: Int,
    val totalPrayTimes: Int,
    val star4Count: Int,
    val star5Count: Int,
    val roleStar4Count: Int,
    val armStar4Count: Int,
    val permStar4Count: Int,
    val roleStar5Count: Int,
    val armStar5Count: Int,
    val permStar5Count: Int,
    val star4Rate: Double,
    val star5Rate: Double,
    val roleStar4Rate: Double,
    val armStar4Rate: Double,
    val permStar4Rate: Double,
    val roleStar5Rate: Double,
    val armStar5Rate: Double,
    val permStar5Rate: Double
)
