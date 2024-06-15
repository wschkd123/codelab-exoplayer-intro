package com.example.exoplayer.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Created by ronaldo on 2017/10/26.
 */

public class HttpModel implements Parcelable {

    // 网络请求的url
    private String requestUrl;
    // 网络请求的方法
    private String requestMethod;
    // 网络请求的头部
    private String requestHeaders;
    // 网络请求的内容 post请求
    private String requestBodyContent;
    // 网络请求的时间
    private String requestTime;
    // 网络请求的时间
    private long durationTime;
    // 网络数据返回的code
    private int responseCode;
    // 网络数据返回的msg
    private String responseMsg;
    // 网络数据返回的head
    private String responseHeaders;
    // 网络数据返回的body内容
    private String responseBodyContent;
    // 网络协议
    private String responseProtocol;

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(String requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getRequestBodyContent() {
        return requestBodyContent;
    }

    public void setRequestBodyContent(String requestBodyContent) {
        this.requestBodyContent = requestBodyContent;
    }

    public long getDurationTime() {
        return durationTime;
    }

    public void setDurationTime(long durationTime) {
        this.durationTime = durationTime;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMsg() {
        return responseMsg;
    }

    public void setResponseMsg(String responseMsg) {
        this.responseMsg = responseMsg;
    }

    public String getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(String responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBodyContent() {
        return responseBodyContent;
    }

    public void setResponseBodyContent(String responseBodyContent) {
        this.responseBodyContent = responseBodyContent;
    }

    public String getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(String requestTime) {
        this.requestTime = requestTime;
    }

    public String getResponseProtocol() {
        return responseProtocol;
    }

    public void setResponseProtocol(String responseProtocol) {
        this.responseProtocol = responseProtocol;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("request time**" + requestTime + "\n");
        stringBuilder.append("request url**" + requestUrl + "\n");
        stringBuilder.append("request method**" + requestMethod + "\n");
        stringBuilder.append("request heads**\n" + requestHeaders.toString());
        if (!TextUtils.isEmpty(requestBodyContent)) {
            stringBuilder.append("requestBodyContent**" + requestBodyContent + "\n\n");
        } else {
            stringBuilder.append("requestBodyContent**" + "null" + "\n\n");
        }
        stringBuilder.append("*********duration*********" + durationTime + "ms" + "\n\n");
        stringBuilder.append("response code**" + responseCode + "\n");
        stringBuilder.append("response msg**" + responseMsg + "\n");
        stringBuilder.append("response protocol**" + responseProtocol + "\n");
        stringBuilder.append("response heads**\n" + responseHeaders + "\n");
        if (!TextUtils.isEmpty(responseBodyContent)) {
            stringBuilder.append("responseBodyContent**\n" + responseBodyContent);
        } else {
            stringBuilder.append("responseBodyContent**" + "null");
        }
        return stringBuilder.toString();
    }

    public String toCurl() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("curl --location --request " + requestMethod);
        stringBuilder.append(" '" + requestUrl + "' \\\n");
        String[] headers = requestHeaders.split("\n");
        for (String hd : headers) {
            stringBuilder.append("--header '" + hd + "' \\\n");
        }
        stringBuilder.append("--data-raw '" + requestBodyContent + "'");
        return stringBuilder.toString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(requestUrl);
        dest.writeString(requestMethod);
        dest.writeString(requestHeaders);
        dest.writeString(requestBodyContent);
        dest.writeString(requestTime);
        dest.writeLong(durationTime);
        dest.writeInt(responseCode);
        dest.writeString(responseMsg);
        dest.writeString(responseProtocol);
        dest.writeString(responseHeaders);
        dest.writeString(responseBodyContent);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<HttpModel> CREATOR = new Creator<HttpModel>() {
        @Override
        public HttpModel createFromParcel(Parcel in) {
            return new HttpModel(in);
        }

        @Override
        public HttpModel[] newArray(int size) {
            return new HttpModel[size];
        }
    };

    protected HttpModel(Parcel in) {
        requestUrl = in.readString();
        requestMethod = in.readString();
        requestHeaders = in.readString();
        requestBodyContent = in.readString();
        requestTime = in.readString();
        durationTime = in.readLong();
        responseCode = in.readInt();
        responseMsg = in.readString();
        responseProtocol = in.readString();
        responseHeaders = in.readString();
        responseBodyContent = in.readString();
    }

    public HttpModel() {

    }
}
