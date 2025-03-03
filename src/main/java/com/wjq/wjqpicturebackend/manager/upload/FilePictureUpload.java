package com.wjq.wjqpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.wjq.wjqpicturebackend.exception.BusinessException;
import com.wjq.wjqpicturebackend.exception.ErrorCode;
import com.wjq.wjqpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 上传文件图片
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {
    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        try {
            multipartFile.transferTo(file);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        }
    }

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
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
}
