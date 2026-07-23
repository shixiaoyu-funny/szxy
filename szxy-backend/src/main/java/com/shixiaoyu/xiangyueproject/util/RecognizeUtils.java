package com.shixiaoyu.xiangyueproject.util;

import com.shixiaoyu.xiangyueproject.config.RecognizeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import java.io.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.net.URLEncoder;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecognizeUtils {
    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder().readTimeout(300, TimeUnit.SECONDS).build();
    private final RecognizeProperties recognizeProperties;

    public String recognize(String path) {
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        try {
            RequestBody body = RequestBody.create(mediaType, "image=" + getFileContentAsBase64(path, true));
            Request request = new Request.Builder()
                    .url("https://aip.baidubce.com/rest/2.0/image-classify/v1/classify/ingredient?access_token=" + getAccessToken())
                    .method("POST", body)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "UsGvDHkrDCl9fY7QDi90KSsI")
                    .build();
            Response response = HTTP_CLIENT.newCall(request).execute();
            String res=response.body().string();
            log.info("response:{}", res);
            return res;
        } catch (IOException e) {
            log.error("识别异常", e);
            return null;
        }
    }

    /**
     * 获取文件base64编码
     *
     * @param path      文件路径
     * @param urlEncode 如果Content-Type是application/x-www-form-urlencoded时,传true
     * @return base64编码信息，不带文件头
     * @throws IOException IO异常
     */
    private String getFileContentAsBase64(String path, boolean urlEncode) throws IOException {
        log.info("读取OSS文件：{}", path);
        Request request = new Request.Builder().url(path).get().build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("OSS文件下载失败");
        }
        byte[] fileBytes = response.body().bytes();
        // Base64编码
        String base64 = Base64.getEncoder().encodeToString(fileBytes);
        if (urlEncode) {
            base64 = URLEncoder.encode(base64, "UTF-8");
        }
        return base64;
    }

    /**
     * 从用户的AK，SK生成鉴权签名（Access Token）
     *
     * @return 鉴权签名（Access Token）
     * @throws IOException IO异常
     */
    private String getAccessToken() throws IOException {
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "grant_type=client_credentials&client_id=" + recognizeProperties.getApiKey()
                + "&client_secret=" + recognizeProperties.getSecretKey());
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/oauth/2.0/token")
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        return new JSONObject(response.body().string()).getString("access_token");
    }
}
