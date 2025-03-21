package com.wjq.wjqpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.wjq.wjqpicturebackend.common.BaseResponse;
import com.wjq.wjqpicturebackend.common.ResultUtils;
import com.wjq.wjqpicturebackend.config.CosClientConfig;
import com.wjq.wjqpicturebackend.exception.BusinessException;
import com.wjq.wjqpicturebackend.exception.ErrorCode;
import com.wjq.wjqpicturebackend.exception.ThrowUtils;
import com.wjq.wjqpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 此方法已废弃，改用upload包下的类
 */
@Service
@Slf4j
@Deprecated
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 获得上传图片的详细信息
     *
     * @param multipartFile
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        //校验图片信息
        this.validPicture(multipartFile);
        //上传文件名 (日期 + 随机数.后缀);
        //日期
        String date = DateUtil.formatDate(new Date());
        //随机数
        String randomString = RandomUtil.randomString(10);
        String originalFilename = multipartFile.getOriginalFilename();
        //后缀
        String fileSuffix = FileUtil.getSuffix(originalFilename);
        String uploadFileName = String.format("%s_%s.%s", date, randomString, fileSuffix);
        //如果多个项目共用一个存储桶，建议再加一个前缀以示区分
        String uploadFilePath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);

        File file = null;
        try {
            //创建本地临时文件保存前端上传的文件
            file = File.createTempFile(uploadFilePath, null);
            //将multipartFile中的数据保存到file中
            multipartFile.transferTo(file);
            //最终将文件上传到cos对象存储中
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilePath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue(); //小数点后1位

            //封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadFilePath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);  //宽高比
            uploadPictureResult.setPicFormat(imageInfo.getFormat());  //图片格式

            return uploadPictureResult;
        } catch (IOException e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            this.deleteTempFile(file);
        }
    }

    /**
     * 获得上传图片的详细信息
     *
     * @param fileUrl
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {
        //校验图片信息
        this.validPicture(fileUrl);
        //上传文件名 (日期 + 随机数.后缀);
        //日期
        String date = DateUtil.formatDate(new Date());
        //从url中获取图片名称
        String fileOriginalName = FileUtil.mainName(fileUrl);
        //随机数
        String randomString = RandomUtil.randomString(10);
        //后缀
        String fileSuffix = FileUtil.getSuffix(fileOriginalName);
        String uploadFileName = String.format("%s_%s.%s", date, randomString, fileSuffix);
        //如果多个项目共用一个存储桶，建议再加一个前缀以示区分
        String uploadFilePath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);

        File file = null;
        try {
            //创建本地临时文件保存前端上传的文件
            file = File.createTempFile(uploadFilePath, null);
            //将multipartFile中的数据保存到file中
            HttpUtil.downloadFile(fileUrl, file);
            //最终将文件上传到cos对象存储中
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilePath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue(); //小数点后1位

            //封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadFilePath);
            uploadPictureResult.setPicName(FileUtil.mainName(fileOriginalName));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);  //宽高比
            uploadPictureResult.setPicFormat(imageInfo.getFormat());  //图片格式

            return uploadPictureResult;
        } catch (IOException e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            this.deleteTempFile(file);
        }
    }

    private void validPicture(String fileUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR);

        //验证url格式
        try {
            new URL(fileUrl);  //验证是否为合法的url
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }

        //校验url协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")
                , ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");

        // 发送 HEAD 请求以验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 未正常返回，无需执行其他判断
            /*有些 URL 地址可能不支持通过 HEAD 请求访问，为了提高导入成功率，
            即使 HEAD 请求访问失败，也不会报错，并且不用执行后续的校验。仅对能获取到的信息进行校验。
            */
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            //  校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            //  校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 2 * 1024 * 1024L; // 限制文件大小为 2MB
                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }


    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR);

        //1.校验文件大小
        long fileSize = multipartFile.getSize();
        //定义最大可上传的文件大小
        final long MAX_SIZE = 2 * 1024 * 1024;   //2MB
        ThrowUtils.throwIf(fileSize > MAX_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
        //2.校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        //定义允许上传的文件后缀
        final List<String> ALLOW_FORMAT_SUFFIX_LIST = Arrays.asList("jpg", "png", "jpeg", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_SUFFIX_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "上传文件格式错误");
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
