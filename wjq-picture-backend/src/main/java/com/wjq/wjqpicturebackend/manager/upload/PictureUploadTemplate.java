package com.wjq.wjqpicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.wjq.wjqpicturebackend.config.CosClientConfig;
import com.wjq.wjqpicturebackend.exception.BusinessException;
import com.wjq.wjqpicturebackend.exception.ErrorCode;
import com.wjq.wjqpicturebackend.manager.CosManager;
import com.wjq.wjqpicturebackend.model.dto.file.UploadPictureResult;
import com.wjq.wjqpicturebackend.utils.color.HexColorFixer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 获得上传图片的详细信息
     *
     * @param inputSource
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        //校验图片信息
        validPicture(inputSource);
        //上传文件名 (日期 + 随机数.后缀);
        //日期
        String date = DateUtil.formatDate(new Date());
        //随机数
        String randomString = RandomUtil.randomString(10);
        //获取文件名
        String originalFilename = getOriginFilename(inputSource);
        //后缀
        String fileSuffix = FileUtil.getSuffix(originalFilename);
        String uploadFileName = String.format("%s_%s.%s", date, randomString, fileSuffix);
        //如果多个项目共用一个存储桶，建议再加一个前缀以示区分
        String uploadFilePath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);

        File file = null;
        try {
            //创建本地临时文件保存前端上传的文件
            file = File.createTempFile(uploadFilePath, null);
            // 处理文件来源（本地或 URL）
            processFile(inputSource, file);
            //最终将文件上传到cos对象存储中
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilePath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                CIObject compressedCiObject = objectList.get(0);
                // 缩略图默认等于压缩图
                CIObject thumbnailCiObject = compressedCiObject;
                if(objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
                // 封装压缩图返回结果
                return buildResult(originalFilename, compressedCiObject,thumbnailCiObject,imageInfo);
            }
            //封装原图返回结果
            return buildResult(originalFilename,file,uploadFilePath,imageInfo);
        } catch (IOException e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTempFile(file);
        }
    }


    /**
     * 获取文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 图片处理，将前端上传的文件图片或url中图片的信息保存到本地图片中
     */
    protected abstract void processFile(Object inputSource, File file);

    /**
     * 图片校验
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 封装返回结果  缩略图
     */
    private UploadPictureResult buildResult(String originalFilename, CIObject compressedCiObject, CIObject thumbnailCiObject, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        uploadPictureResult.setPicColor(HexColorFixer.fixHexColor(imageInfo.getAve()));  //获得图片主色调
        return uploadPictureResult;
    }


    /**
     * 封装返回结果
     */
    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicColor(HexColorFixer.fixHexColor(imageInfo.getAve()));  //获得图片主色调
        return uploadPictureResult;
    }


    private void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }


}
