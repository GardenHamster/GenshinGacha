package com.hamster.pray.genshin.util

import com.google.gson.GsonBuilder
import com.hamster.pray.genshin.config.Config
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.http.client.fluent.Request
import java.io.File


class HttpUtil {
    companion object {
        fun downloadPicture(url: String, savePath: String): File? {
            try {
                val file = File(savePath)
                Request.Get(url).connectTimeout(30 * 1000).socketTimeout(30 * 1000).execute().saveContent(file)
                return file
            } catch (e: Exception) {
                return null
            }
        }

        fun httpGet(url: String):String {
            val client = OkHttpClient().newBuilder()
                .hostnameVerifier(RxUtils.TrustAllHostnameVerifier())
                .sslSocketFactory(RxUtils().createSSLSocketFactory(), RxUtils.TrustAllCerts())
                .build()
            val builder = okhttp3.Request.Builder()
            builder.addHeader("Content-Type", "application/json")
            builder.addHeader("authorzation", Config.authorzation)
            builder.url(url).get()
            return client.newCall(builder.build()).execute().body!!.string()
        }

        fun httpPost(url: String,params:MutableMap<String, Any>):String{
            val client = OkHttpClient().newBuilder()
                .hostnameVerifier(RxUtils.TrustAllHostnameVerifier())
                .sslSocketFactory(RxUtils().createSSLSocketFactory(), RxUtils.TrustAllCerts())
                .build()
            val jsonStr = GsonBuilder().create().toJson(params)
            val requestBody = jsonStr?.let {
                val contentType: MediaType = "application/json; charset=utf-8".toMediaType()
                jsonStr.toRequestBody(contentType)
            } ?: run {
                FormBody.Builder().build()
            }
            val builder = okhttp3.Request.Builder()
            builder.addHeader("Content-Type", "application/json")
            builder.addHeader("authorzation", Config.authorzation)
            builder.url(url).post(requestBody)
            return client.newCall(builder.build()).execute().body!!.string()
        }

    }
}