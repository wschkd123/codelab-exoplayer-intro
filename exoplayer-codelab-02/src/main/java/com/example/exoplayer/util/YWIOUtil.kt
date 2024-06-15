package com.example.exoplayer.util

import java.io.BufferedReader
import java.io.Closeable
import java.io.Flushable
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * IO 处理工具类
 * Created by fengkeke on 2023/5/19
 */
object YWIOUtil {
    private val TAG = "YWIOUtil"
    @JvmStatic
    fun getString(input: InputStream?): String {
        if (input == null) {
            return ""
        }
        val reader = BufferedReader(InputStreamReader(input))
        val sb = StringBuilder()
        var line: String? = null
        try {
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return sb.toString()
    }


    /**
     * 关闭输入流
     *
     * @param closeable IO stream
     */
    fun close(closeable: Closeable?) {
        if (closeable == null) {
            return
        }
        try {
            closeable.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Flush输入流
     *
     * @param flushable IO stream
     */
    fun flush(flushable: Flushable?) {
        if (flushable == null) {
            return
        }
        try {
            flushable.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}