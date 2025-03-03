package com.wjq.wjqpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.wjq.wjqpicturebackend.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }


    /**
     * 上传图片(并附带图片信息)
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        //对图片进行处理（获取基本信息也算作是一种处理）
        PicOperations picOperations = new PicOperations();
        // 图片压缩（转成 webp 格式）
        String webKey = FileUtil.mainName(file) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();  //new一个压缩规则对象
        ArrayList<PicOperations.Rule> rules = new ArrayList<>();  //集合，存放压缩规则对象
//        Pic-Operations:
//        {
//            "is_pic_info": 1,
//                "rules": [{
//            "fileid": "exampleobject",
//                    "rule": "imageMogr2/format/<Format>"
//        }]
//        }
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webKey);
        compressRule.setRule("imageMogr2/format/webp");
        rules.add(compressRule);

//        Pic-Operations:
//        {
//            "is_pic_info": 1,
//                "rules": [{
//            "fileid": "exampleobject",
//                    "rule": "imageMogr2/thumbnail/<imageSizeAndOffsetGeometry>"
//        }]
//        }

        //只有在原图大于20KB时，才生成缩略图
        if (file.length() > 20 * 1024) {
            //生成缩略图
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            //判断后缀是否为空
            if (StrUtil.isBlank(FileUtil.getSuffix(file))) {
                //为空，则默认加上.png后缀
                thumbnailRule.setFileId(FileUtil.mainName(file) + "_thumbnail." + "png");
            }
            thumbnailRule.setFileId(FileUtil.mainName(file) + "_thumbnail." + FileUtil.getSuffix(file));
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            rules.add(thumbnailRule);
        }
        //1表示返回原图信息
        picOperations.setIsPicInfo(1);
        //构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

}
