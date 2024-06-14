package com.example.exoplayer

import android.app.Activity
import android.util.Log
import com.example.exoplayer.util.JsonUtilKt
import com.example.exoplayer.util.YWFileUtil
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.internal.sse.RealEventSource
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 通过sse协议实现文本流式输入语音流式输出
 *
 * https://platform.minimaxi.com/document/guides/T2A-model/stream?id=65701c77024fd5d1dffbb8fe
 *
 * @author wangshichao
 * @date 2024/6/14
 */
class SpeechStreamHelper(
    private val activity: Activity,
    val invoke: (MediaDataSource) -> Unit
) {
    private val TAG = "SpeechStreamHelper"
    private val apiKey =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJHcm91cE5hbWUiOiLkuIrmtbfnrZHmoqblspvkurrlt6Xmmbrog73np5HmioDmnInpmZDlhazlj7giLCJVc2VyTmFtZSI6ImNsaWVudHRlc3QiLCJBY2NvdW50IjoiY2xpZW50dGVzdEAxNzgyNTg4NTA5Njk4MTM0NDU1IiwiU3ViamVjdElEIjoiMTgwMTE5NDU2ODkwNTkyNDYwOSIsIlBob25lIjoiIiwiR3JvdXBJRCI6IjE3ODI1ODg1MDk2OTgxMzQ0NTUiLCJQYWdlTmFtZSI6IiIsIk1haWwiOiIiLCJDcmVhdGVUaW1lIjoiMjAyNC0wNi0xMyAyMToxNzoyMCIsImlzcyI6Im1pbmltYXgifQ.T-09xCHVDtou3vpO_gIxJW8dg9yOw8BQ_gIpDffhWWAzZb5R6Tv2Q6UJdMRxdPdCYWjqRnOBRS8dEf2Wu9rukhFY9CoDoeYQ7hNwB8472aoz67hJnv0420PlOXTV9VH5MB648lC0uYcdmOQ7-VH7MF5NSyvYr-rRvyL2UVJr2zyGlsS40ngzygoaIJK3ZmD7O-v1ko-JRBiFTFFfzb6Kp6lRnc20HKnK35gpJVY2OkmtoxxFCXm8rJvFuj0dlijmoeqKG8hS8f6JDpkybp1pqlwzOSg15f1rDstYOAtL8OYkYuJeNZFkZ9sUCPyqQPVkQhDJLZhJS9VaVzJmkLTpBw"
    private val format = "mp3"
    private val mediaType = MediaType.parse("application/json; charset=utf-8");
    private var chunkIndex = 0

    fun loadData(content: String = "你好") {
        chunkIndex = 0
        val json = "{\n" +
                "    \"timber_weights\": [\n" +
                "      {\n" +
                "        \"voice_id\": \"male-qn-qingse\",\n" +
                "        \"weight\": 1\n" +
                "      },\n" +
                "      {\n" +
                "        \"voice_id\": \"female-shaonv\",\n" +
                "        \"weight\": 1\n" +
                "      }\n" +
                "    ],\n" +
                "    \"text\": \"${content}\",\n" +
                "    \"voice_id\": \"\",\n" +
                "    \"model\": \"speech-01\",\n" +
                "    \"speed\": 1,\n" +
                "    \"vol\": 1,\n" +
                "    \"pitch\": 0,\n" +
                "    \"audio_sample_rate\": 32000,\n" +
                "    \"bitrate\": 128000,\n" +
                "    \"format\": \"$format\"\n" +
                "  }"
        val okHttpClient: OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .build()
        val requestBody = RequestBody.create(mediaType, json)
        val request = Request.Builder()
            .url("https://api.minimax.chat/v1/tts/stream?GroupId=1782588509698134455")
            .addHeader("accept", "application/json, text/plain, */*")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val realEventSource = RealEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                super.onOpen(eventSource, response)
                showMessage("已连接")
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                super.onEvent(eventSource, id, type, data)
                processMessageContent(data)
            }

            override fun onClosed(eventSource: EventSource) {
                super.onClosed(eventSource)
                showMessage("已断开")
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                super.onFailure(eventSource, t, response)
                showMessage("连接失败 ${t?.message}")
            }
        })
        realEventSource.connect(okHttpClient)

    }

    private fun showMessage(msg: String) {
        activity.runOnUiThread {
            Log.i(TAG, "msg:${msg}")
        }
    }

    private fun processMessageContent(msg: String) {
        val chunk = JsonUtilKt.toObject(msg, ChunkData::class.java)
        if (chunk == null) {
            Log.i(TAG, "chunk is null")
            return
        }
        val trace_id = chunk.trace_id
        if (chunk.base_resp.isSuccess().not()) {
            Log.i(TAG, "chunk is fail, msg ${chunk.base_resp.status_msg} trace_id:$trace_id")
            return
        }
        if (chunk.data.audio.isNullOrEmpty()) {
            Log.i(TAG, "audio is null, trace_id:$trace_id")
            return
        }
        activity.runOnUiThread {
            val chunkPath = if (chunk.data.isEnd()) {
                //TODO 缓存
                Log.i(TAG, "sse end, trace_id:$trace_id")
                saveAudioLocal(chunk.data.audio, chunk.trace_id)
            } else {
                Log.i(TAG, "content:${chunk.data.audio.length}, trace_id:$trace_id")
                saveAudioLocal(chunk.data.audio, chunk.trace_id + "_" + chunkIndex++)
            }
            invoke.invoke(MediaDataSource(chunk.trace_id, chunkPath, format, chunk.data.isEnd()))
        }
    }

    private fun saveAudioLocal(data: String, key: String): String {
        val byteArray = decodeHex(data)
        val path = YWFileUtil.getStorageFileDir(activity).path + "/" + key + ".mp3"
        saveByteArrayToFile(byteArray, path)
        return path
    }

    private fun decodeHex(hexString: String): ByteArray {
        val byteArray = ByteArray(hexString.length / 2)
        var i = 0
        while (i < hexString.length) {
            val hex = hexString.substring(i, i + 2)
            byteArray[i / 2] = hex.toInt(16).toByte()
            i += 2
        }
        return byteArray
    }

    private fun saveByteArrayToFile(byteArray: ByteArray?, filePath: String) {
        try {
            val file = File(filePath)
            val fos = FileOutputStream(file)
            fos.write(byteArray)
            fos.close()
            Log.i(TAG, "File saved successfully.")
        } catch (e: IOException) {
            Log.e(TAG, "Error saving file: ${e.message}")
        }
    }
}

/**
 * 音频片段播放数据资源
 */
data class MediaDataSource(
    /**
     * 本次会话的id
     */
    val traceId: String,
    /**
     * 音频片段路径
     */
    val chunkPath: String,
    /**
     * 生成的音频格式。默认mp3，范围[mp3,pcm,flac]
     */
    val format: String,
    /**
     * 是否合成结束
     */
    val isEnd: Boolean
)


/**
 * 音频片段数据
 */
data class ChunkData(
    val base_resp: BaseResp,
    val `data`: Data,
    val extra_info: ExtraInfo? = null,
    /**
     * 本次会话的id
     */
    val trace_id: String
) {
    data class BaseResp(
        /**
         * 1000，未知错误1001，超时1002，触发限流1004，鉴权失败1013，服务内部错误及非法字符超过10%2013，输入格式信息不正常
         */
        val status_code: Int = 0,
        val status_msg: String? = null
    ) {
        fun isSuccess() = status_code == 0
    }

    data class Data(
        /**
         * 合成后的音频片段，采用hex编码，按照输入定义的格式进行生成（mp3/pcm/flac）
         */
        val audio: String? = null,
        val ced: String? = null,
        /**
         * 当前音频流状态，1表示合成中，2表示合成结束
         */
        val status: Int = 0
    ) {
        fun isEnd() = status == 2
    }

    data class ExtraInfo(
        /**
         * 音频时长，精确到毫秒
         */
        val audio_length: Int = 0,
        /**
         * 单位为字节
         */
        val audio_size: Int = 0,
        /**
         * 默认为24000，如客户请求参数进行调整，会根据请求参数生成
         */
        val audio_sample_rate: Int = 0,
        /**
         * 默认为168000，如客户请求参数进行调整，会根据请求参数生成
         */
        val bitrate: Int = 0,
        /**
         * 非法字符不超过10%（包含10%），音频会正常生成并返回非法字符占比；最大不超过0.1（10%），超过进行报错
         */
        val invisible_character_ratio: Int = 0,
        /**
         * 本次语音生成的计费字符数
         */
        val usage_characters: Int = 0,
        /**
         * 已经发音的字数统计（不算标点等其他符号，包含汉字数字字母）
         */
        val word_count: Int = 0
    )
}