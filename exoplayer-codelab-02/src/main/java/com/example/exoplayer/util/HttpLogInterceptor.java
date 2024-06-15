package com.example.exoplayer.util;


import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;
import okio.Okio;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ronaldo on 2017/9/29.
 * 日志拦截器，打印url和header
 */

public class HttpLogInterceptor implements Interceptor {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private long maxContentLength = 250000L;
    private static final String HEADERS_PARAMS_CONTENT_ENCODING = "Content-Encoding";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String requestBodyContent = "";
        String responseBodyContent = "";

        String url = request.url().toString();
        String method = request.method();
        Headers headers = request.headers();
        RequestBody requestBody = request.body();
        boolean isHasRequestBody = requestBody != null;
        if (isHasRequestBody) {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            Charset charset = UTF8;
            MediaType contentType = requestBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(UTF8);
            }
            boolean isGzip = requestBodyGzipped(headers);
            requestBodyContent = readFromBuffer(buffer.clone(), charset, isGzip);
        }

        HttpModel httpModel = new HttpModel();
        httpModel.setRequestUrl(url);
        httpModel.setRequestMethod(method);
        httpModel.setRequestHeaders(headers.toString());
        httpModel.setRequestBodyContent(requestBodyContent);
        Date nowTime = new Date();
        SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        httpModel.setRequestTime(time.format(nowTime));

        long httpRequestTime = System.currentTimeMillis();
        Response response = chain.proceed(request);
        if (response == null) {
            throw new IOException("interceptor response can not be null");
        }
        long httpResponseTime = System.currentTimeMillis();
        long durationTime = httpResponseTime - httpRequestTime;
        httpModel.setDurationTime(durationTime);
        int code = response.code();
        String msg = response.message();
        String protocol = response.protocol().toString();
        Headers responseHeads = response.headers();
        ResponseBody responseBody = response.body();
        if (HttpHeaders.hasBody(response)) {
            BufferedSource source = getNativeSource(response);
            source.request(Long.MAX_VALUE);
            Buffer buffer = source.buffer();
            Charset charset = UTF8;
            MediaType contentType = responseBody.contentType();
            if (contentType != null) {
                try {
                    charset = contentType.charset(UTF8);
                } catch (UnsupportedCharsetException e) {
                    return response;
                }
            }
            responseBodyContent = readFromBuffer(buffer.clone(), charset, false);
        }

        httpModel.setResponseCode(code);
        httpModel.setResponseMsg(msg);
        httpModel.setResponseProtocol(protocol);
        httpModel.setResponseHeaders(responseHeads.toString());
        httpModel.setResponseBodyContent(buildBeautyJson(responseBodyContent));
//        HttpModelController.getInstance().putHttpModel(httpModel);
        Log.i("HttpLog", httpModel.toString());
        Log.i("HttpLog", httpModel.toCurl());
        return response;
    }

    private BufferedSource getNativeSource(BufferedSource input, boolean isGzipped) {
        if (isGzipped) {
            GzipSource source = new GzipSource(input);
            return Okio.buffer(source);
        } else {
            return input;
        }
    }

    private boolean requestBodyGzipped(Headers headers) {
        String contentEncoding = headers.get(HEADERS_PARAMS_CONTENT_ENCODING);
        return "gzip".equalsIgnoreCase(contentEncoding);
    }

    private boolean responseBodyGzipped(Headers headers) {
        String contentEncoding = headers.get(HEADERS_PARAMS_CONTENT_ENCODING);
        return "gzip".equalsIgnoreCase(contentEncoding);
    }

    private String readFromBuffer(Buffer buffer, Charset charset, boolean isGzip) {
        long bufferSize = buffer.size();
        long maxBytes = Math.min(bufferSize, maxContentLength);
        String body = "";
        if (!isGzip) {
            try {
                body = buffer.readString(maxBytes, charset);
            } catch (EOFException e) {
                body += "\\n\\n--- Unexpected end of content ---";
            }
            if (bufferSize > maxContentLength) {
                body += "\\n\\n--- Content truncated ---";
            }
        } else {
            InputStream is = null;
            try {
                is = new GZIPInputStream(buffer.inputStream());
                body = YWIOUtil.getString(is);
            } catch (Exception e) {

            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return body;
    }

    private BufferedSource getNativeSource(Response response) throws IOException {
        if (responseBodyGzipped(response.headers())) {
            BufferedSource source = response.peekBody(maxContentLength).source();
            if (source.buffer().size() < maxContentLength) {
                return getNativeSource(source, true);
            }
        }
        return response.body().source();
    }

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small
     * sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    private boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            return false; // Truncated UTF-8 sequence.
        }
    }

    public String buildBeautyJson(@Nullable String json) {
        String message = "";
        if (TextUtils.isEmpty(json)) {
            return message;
        }
        try {
            json = json.trim();
            if (json.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(json);
                message = jsonObject.toString(2);
            }
            if (json.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(json);
                message = jsonArray.toString(2);
            }
        } catch (JSONException e) {
        }
        return message;
    }
}
