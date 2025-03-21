package com.wjq.wjqpicturebackend.api.imagesearch;

import com.wjq.wjqpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.wjq.wjqpicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.wjq.wjqpicturebackend.api.imagesearch.sub.GetImageListApi;
import com.wjq.wjqpicturebackend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Deprecated
@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        return GetImageListApi.getImageList(imageFirstUrl);
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://wjq-picture-1344614417.cos.ap-nanjing.myqcloud.com/space/1896902515514535937/2025-03-04_3rcf4eykh2.png16907742964274229069.webp";
        List<ImageSearchResult> resultList = searchImage(imageUrl);
        System.out.println("结果列表" + resultList);
    }
}
