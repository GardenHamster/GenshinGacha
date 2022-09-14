package com.hamster.pray.genshin.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.hamster.pray.genshin.config.Config
import com.hamster.pray.genshin.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*

class GachaUtil {
    companion object {

        fun rolePrayOne(pondIndex: Int, memberCode: String, memberName: String): ApiResult<PrayResult> {
            val url = "${Config.apiUrl}/api/RolePray/PrayOne?memberCode=${memberCode}&memberName=${memberName}&pondIndex=${pondIndex}";
            val json = HttpUtil.httpGet(url);
            return Gson().fromJson(json, object : TypeToken<ApiResult<PrayResult>>() {}.type)
        }

        fun rolePrayTen(pondIndex: Int, memberCode: String, memberName: String): ApiResult<PrayResult> {
            val url = "${Config.apiUrl}/api/RolePray/PrayTen?memberCode=${memberCode}&memberName=${memberName}&pondIndex=${pondIndex}";
            val json = HttpUtil.httpGet(url);
            return Gson().fromJson(json, object : TypeToken<ApiResult<PrayResult>>() {}.type)
        }

        fun armPrayOne(memberCode: String, memberName: String): ApiResult<PrayResult> {
            val url = "${Config.apiUrl}/api/ArmPray/PrayOne?memberCode=${memberCode}&memberName=${memberName}";
            val json = HttpUtil.httpGet(url);
            return Gson().fromJson(json, object : TypeToken<ApiResult<PrayResult>>() {}.type)
        }

        fun armPrayTen(memberCode: String, memberName: String): ApiResult<PrayResult> {
            val url = "${Config.apiUrl}/api/ArmPray/PrayTen?memberCode=${memberCode}&memberName=${memberName}";
            val json = HttpUtil.httpGet(url);
            return Gson().fromJson(json, object : TypeToken<ApiResult<PrayResult>>() {}.type)
        }

        fun permPrayOne(memberCode: String, memberName: String): ApiResult<PrayResult> {
            val url = "${Config.apiUrl}/api/PermPray/PrayOne?memberCode=${memberCode}&memberName=${memberName}";
            val json = HttpUtil.httpGet(url);
            return Gson().fromJson(json, object : TypeToken<ApiResult<PrayResult>>() {}.type)
        }

        fun permPrayTen(memberCode: String, memberName: String): ApiResult<PrayResult> {
            val url = "${Config.apiUrl}/api/PermPray/PrayTen?memberCode=${memberCode}&memberName=${memberName}";
            val json = HttpUtil.httpGet(url);
            return Gson().fromJson(json, object : TypeToken<ApiResult<PrayResult>>() {}.type)
        }

        fun fullRolePrayOne(memberCode: String, memberName: String): ApiResult<PrayResult> {
            val url = "${Config.apiUrl}/api/FullRolePray/PrayOne?memberCode=${memberCode}&memberName=${memberName}";
            val json = HttpUtil.httpGet(url);
            return Gson().fromJson(json, object : TypeToken<ApiResult<PrayResult>>() {}.type)
        }

        fun fullRolePrayTen(memberCode: String, memberName: String): ApiResult<PrayResult> {
            val url = "${Config.apiUrl}/api/FullRolePray/PrayTen?memberCode=${memberCode}&memberName=${memberName}";
            val json = HttpUtil.httpGet(url);
            return Gson().fromJson(json, object : TypeToken<ApiResult<PrayResult>>() {}.type)
        }

        fun fullArmPrayOne(memberCode: String, memberName: String): ApiResult<PrayResult> {
            val url = "${Config.apiUrl}/api/FullArmPray/PrayOne?memberCode=${memberCode}&memberName=${memberName}";
            val json = HttpUtil.httpGet(url);
            return Gson().fromJson(json, object : TypeToken<ApiResult<PrayResult>>() {}.type)
        }

        fun fullArmPrayTen(memberCode: String, memberName: String): ApiResult<PrayResult> {
            val url = "${Config.apiUrl}/api/FullArmPray/PrayTen?memberCode=${memberCode}&memberName=${memberName}";
            val json = HttpUtil.httpGet(url);
            return Gson().fromJson(json, object : TypeToken<ApiResult<PrayResult>>() {}.type)
        }

        fun assign(memberCode: String, memberName: String, goodsName: String): ApiResult<Any> {
            val url = "${Config.apiUrl}/api/PrayInfo/SetMemberAssign?memberCode=${memberCode}&memberName=${memberName}&goodsName=${goodsName}";
            val json = HttpUtil.httpGet(url);
            return Gson().fromJson(json, object : TypeToken<ApiResult<PrayResult>>() {}.type)
        }

        fun getPondInfo(): ApiResult<PondInfo>{
            val url = "${Config.apiUrl}/api/PrayInfo/GetPondInfo";
            val json = HttpUtil.httpGet(url);
            return Gson().fromJson(json, object : TypeToken<ApiResult<PrayResult>>() {}.type)
        }

        fun getPrayDetail(memberCode: String): ApiResult<PrayDetail>{
            val url = "${Config.apiUrl}/api/PrayInfo/GetMemberPrayDetail?memberCode=${memberCode}";
            val json = HttpUtil.httpGet(url);
            return Gson().fromJson(json, object : TypeToken<ApiResult<PrayResult>>() {}.type)
        }

        fun getPrayRecords(memberCode: String): ApiResult<PrayRecord>{
            val url = "${Config.apiUrl}/api/PrayInfo/GetMemberPrayRecords?memberCode=${memberCode}";
            val json = HttpUtil.httpGet(url);
            return Gson().fromJson(json, object : TypeToken<ApiResult<PrayResult>>() {}.type)
        }

        fun getLuckRanking(): ApiResult<LuckRanking>{
            val url = "${Config.apiUrl}/api/PrayInfo/GetLuckRanking";
            val json = HttpUtil.httpGet(url);
            return Gson().fromJson(json, object : TypeToken<ApiResult<PrayResult>>() {}.type)
        }

        fun setRolePond(pondIndex: Int, upItems: Array<String>): ApiResult<Any> {
            val url = "${Config.apiUrl}/api/PrayInfo/SetRolePond"
            var params: MutableMap<String, Any> = mutableMapOf()
            params.set("pondIndex", pondIndex)
            params.set("upItems", upItems)
            val json = HttpUtil.httpPost(url, params);
            return Gson().fromJson(json, object : TypeToken<ApiResult<Any>>() {}.type)
        }

        fun setArmPond(upItems: Array<String>): ApiResult<Any> {
            val url = "${Config.apiUrl}/api/PrayInfo/SetArmPond"
            var params: MutableMap<String, Any> = mutableMapOf()
            params.set("upItems", upItems)
            val json = HttpUtil.httpPost(url, params);
            return Gson().fromJson(json, object : TypeToken<ApiResult<Any>>() {}.type)
        }

        fun setSkinRate(skinRate: Int): ApiResult<Any> {
            val url = "${Config.apiUrl}/api/PrayInfo/SetSkinRate?rare=${skinRate}"
            var params: MutableMap<String, Any> = mutableMapOf()
            val json = HttpUtil.httpPost(url, params);
            return Gson().fromJson(json, object : TypeToken<ApiResult<Any>>() {}.type)
        }

        fun resetRolePond(): ApiResult<Any> {
            val url = "${Config.apiUrl}/api/PrayInfo/ResetRolePond"
            var params: MutableMap<String, Any> = mutableMapOf()
            val json = HttpUtil.httpPost(url, params);
            return Gson().fromJson(json, object : TypeToken<ApiResult<Any>>() {}.type)
        }

        fun resetArmPond(): ApiResult<Any> {
            val url = "${Config.apiUrl}/api/PrayInfo/ResetArmPond"
            var params: MutableMap<String, Any> = mutableMapOf()
            val json = HttpUtil.httpPost(url, params);
            return Gson().fromJson(json, object : TypeToken<ApiResult<Any>>() {}.type)
        }

    }
}