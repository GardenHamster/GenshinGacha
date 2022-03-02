package com.hamster.pray.genshin.data

import kotlinx.serialization.Serializable

@Serializable
data class ApiResult<T> (
    val code :Int=0,
    val message:String="",
    val data:T
)