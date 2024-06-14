package com.example.exoplayer.util

import android.util.Log
import com.google.gson.Gson
import org.json.JSONObject
import java.lang.reflect.Type


/**
 * Created by p_dmweidu on 2022/6/14
 * Desc: Json 工具类
 */
object JsonUtilKt {

    private const val TAG: String = "JsonUtilKt"
    private val gson: Gson = Gson()

    @JvmStatic
    fun toJson(obj: Any?): String {
        return gson.toJson(obj)
    }

    @JvmStatic
    fun <T> toObject(json: String?, classOfT: Class<T>): T? {
        return try {
            gson.fromJson(json, classOfT)
        } catch (e: Exception) {
            Log.w(TAG, "toObject: error ${e.message}")
            null
        }
    }

    @JvmStatic
    fun <T> toObject(json: String?, typeOfT: Type): T? {
        return try {
            gson.fromJson(json, typeOfT)
        } catch (e: Exception) {
            Log.w(TAG, "toObject: error ${e.message}")
            null
        }
    }

    @JvmStatic
    fun <T> toList(json: String?, type: Type): List<T>? {
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.w(TAG, "toObject: error ${e.message}")
            null
        }
    }

    @JvmStatic
    fun <T> toNetRespData(json: String?, classOfT: Class<T>): T? {
        if (json.isNullOrEmpty()) {
            return null
        }
        try {
            val jsonObject = JSONObject(json)
            val code = jsonObject.optInt("code", -1)
            if (code == 0) {
                val dataString = jsonObject.optString("data")
                return gson.fromJson(dataString, classOfT)
            }
            return null
        } catch (e: Exception) {
            return null
        }
    }

    @JvmStatic
    fun getRespCode(json: String?): Int {
        if (json.isNullOrEmpty()) {
            return -1
        }
        return try {
            val jsonObject = JSONObject(json)
            jsonObject.optInt("code", -1)
        } catch (e: Exception) {
            -1
        }
    }

    @JvmStatic
    fun getRespMsg(json: String?): String? {
        if (json.isNullOrEmpty()) {
            return null
        }
        return try {
            val jsonObject = JSONObject(json)
            jsonObject.optString("msg")
        } catch (e: Exception) {
            null
        }
    }

}
