package com.wjq.wjqpicturebackend.api.imagesearch;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.*;
import cn.hutool.json.JSONUtil;
import com.wjq.wjqpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.wjq.wjqpicturebackend.exception.BusinessException;
import com.wjq.wjqpicturebackend.exception.ErrorCode;
import com.wjq.wjqpicturebackend.exception.ThrowUtils;
import com.wjq.wjqpicturebackend.manager.upload.UrlPictureUpload;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class GetImageByImageThrough360 {

    public static List<ImageSearchResult> getImagePageUrl(String imageUrl) {
        //1.准备请求参数
        HashMap<String, Object> formData = new HashMap<>();
        formData.put("img_url", imageUrl);
        //请求地址
        String url = "https://st.so.com/stu?a=mrecomm&start=0";
        try {
            //2.发送post请求到百度接口
            HttpResponse response = HttpRequest.post(url)
                    .form(formData)
                    .timeout(5000)
                    .execute();
            //判断响应状态
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            // status	0
            // msg	"Success"
            // data	Object { url: "https://graph.baidu.com/s?card_key=pc", sign: "126a89b586717eae217bf01741089453" }
            //解析响应
            String responseBody = response.body();
            Map<String, Object> result = JSONUtil.toBean(responseBody, Map.class);
            //3.处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("errno"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            if (data == null || !Integer.valueOf(0).equals(data.get("error"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            List<Map<String, Object>> resultList = (List<Map<String, Object>>) data.get("result");
            List<ImageSearchResult> urlList = new ArrayList<>();
            for (Map<String, Object> resultMap : resultList) {
                ImageSearchResult imageSearchResult = new ImageSearchResult();
                String rawUrl = (String) resultMap.get("imgurl");
                // 对 URL 进行解码
//                String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
                rawUrl = rawUrl.replace("\\/", "/");
//                boolean validResult = validPicture(searchResultUrl);
                imageSearchResult.setImageUrl(rawUrl);
                urlList.add(imageSearchResult);
            }
            // 如果 URL 为空
            if (urlList.isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");
            }
            return urlList;
        } catch (Exception e) {
            log.error("搜索失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    /**
     * 处理不合规的图片
     */
//    public static boolean validPicture(String fileUrl) {
//        return checkImageSize(fileUrl);
//    }
//
//
//    public static boolean checkImageSize(String imageUrl) {
//        InputStream inputStream = null;
//        HttpURLConnection connection = null;
//
//        try {
//            URL url = new URL(imageUrl);
//            connection = (HttpURLConnection) url.openConnection();
//            connection.setRequestMethod("GET");
//            connection.setConnectTimeout(5000); // 设置连接超时时间为 5 秒
//            connection.setReadTimeout(5000);    // 设置读取超时时间为 5 秒
//            // 检查 HTTP 响应码，确保图片可访问
//            int responseCode = connection.getResponseCode();
//            if (responseCode != HttpURLConnection.HTTP_OK) {
//                return false;
//            }
//            inputStream = connection.getInputStream();
//            // 读取图片内容并计算大小
//            byte[] buffer = new byte[2048]; // 每次读取 2KB
//            int bytesRead;
//            long totalBytes = 0;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                totalBytes += bytesRead;
//                // 如果已经超过 2MB，直接返回 false
//                if (totalBytes > 2 * 1024 * 1024) {
//                    return false;
//                }
//            }
//            return true;
//        } catch (IOException e) {
//            return false; // 如果发生异常，返回 false
//        } finally {
//            if (inputStream != null) {
//                try {
//                    inputStream.close();
//                } catch (IOException e) {
//                    log.error(e.getMessage(), e);
//                }
//            }
//            if (connection != null) {
//                connection.disconnect(); // 关闭连接
//            }
//        }
//    }
}
