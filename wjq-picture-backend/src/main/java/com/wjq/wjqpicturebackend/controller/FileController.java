package com.wjq.wjqpicturebackend.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.wjq.wjqpicturebackend.annotation.AuthCheck;
import com.wjq.wjqpicturebackend.common.BaseResponse;
import com.wjq.wjqpicturebackend.common.ResultUtils;
import com.wjq.wjqpicturebackend.constant.UserConstant;
import com.wjq.wjqpicturebackend.exception.BusinessException;
import com.wjq.wjqpicturebackend.exception.ErrorCode;
import com.wjq.wjqpicturebackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传
     * @param multipartFile
     * @return
     */
    @PostMapping("/test/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> fileUpload(@RequestPart("file") MultipartFile multipartFile) {
        String originalFilename = multipartFile.getOriginalFilename();
        System.out.println(originalFilename);
        String filePath = String.format("/test/%s", originalFilename);

        File file = null;
        try {
            //创建本地临时文件保存前端上传的文件
            file = File.createTempFile(filePath, null);
            //将multipartFile中的数据保存到file中
            multipartFile.transferTo(file);
            //最终将文件上传到cos对象存储中
            cosManager.putObject(filePath, file);
            return ResultUtils.success(filePath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传图片失败");
        } finally {
            if (file != null) {
                //删除本地临时文件，避免文件堆积占用空间
                boolean isDelete = file.delete();
                if(!isDelete){
                    log.error("file delete failed,file path:{}",filePath);
                }
            }
        }
    }

    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }

}
