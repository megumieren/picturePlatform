package com.wjq.wjqpicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.wjq.wjqpicturebackend.exception.BusinessException;
import com.wjq.wjqpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


@Deprecated
@Slf4j
public class GetImagePageUrlApi {

    /**
     * 获取图片页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl) {
        //1.准备请求参数
        HashMap<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn","pc");
        formData.put("from","pc");
        formData.put("image_source","PC_UPLOAD_FILE");
        long uptime = System.currentTimeMillis();
        //请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;
        String acsToken = "1741062668951_1741087078365_43LGS64gYMPlYyk/gxTFPdNafHvbcq9vvrrSkz1WMYrmYMuOcQS5GSIjeVYiA5Bi0yShC7k7V/4L6fFxvPIQsgIDZkiOpM1m/Df3szc8ykPWSCRyDqA3LeqEbBCrKDND2vbcr58mTepA/pQU1R07yUznNjzV1nWjnHrssq5ZZHejws0sTN79GkOcvhxvCajIAsPgozZYKv2v9kGaz45m4dhOVY3zicqudavK8WtEt8VsnOKMU/1MoL7p0zXcNCShzIE3z/x9NRMMNJKAzh07g4IXTuX4lBZRJjb+XuCQvKtAaV0PFblmtReZ7Rz9DHLavja+zzuVll3y1ZxLpoQ/mZrymsOUgy183q10jtPZGHzM5WTOjBQLvtHktL4Sb0dT/qQw8bG5ltp11a7V+7zeoF+iGkP/8a5pO0HjTx2XD5m+9PssRJSuugrV6rIvU69T9AekPaqriGVveKCT0LbRpGrpBAuliR6PW9LM+ZpEJO8=";
        try {
            //2.发送post请求到百度接口
            HttpResponse response = HttpRequest.post(url)
                    .header("Acs-Token", acsToken)
                    .form(formData)
                    .timeout(5000)
                    .execute();
            //判断响应状态
            if(response.getStatus() != HttpStatus.HTTP_OK){
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            // status	0
            // msg	"Success"
            // data	Object { url: "https://graph.baidu.com/s?card_key=pc", sign: "126a89b586717eae217bf01741089453" }
            //解析响应
            String responseBody = response.body();
            Map<String,Object> result = JSONUtil.toBean(responseBody, Map.class);
            //3.处理响应结果
            if(result == null || !Integer.valueOf(0).equals(result.get("status"))){
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            Map<String,Object> data = (Map<String, Object>) result.get("data");
            String rawUrl = (String) data.get("url");
            // 对 URL 进行解码
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            // 如果 URL 为空
            if (searchResultUrl == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");
            }
            return searchResultUrl;
        } catch (Exception e) {
            log.error("搜索失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

//    public static void main(String[] args) {
//        // 测试以图搜图功能
//        String imageUrl = "https://wjq-picture-1344614417.cos.ap-nanjing.myqcloud.com/blackmyth_wukong_wallpaper_m_034.jpg";
//        String result = getImagePageUrl(imageUrl);
//        System.out.println("搜索成功，结果 URL：" + result);
//    }
}
