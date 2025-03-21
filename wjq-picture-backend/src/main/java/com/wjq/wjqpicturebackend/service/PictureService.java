package com.wjq.wjqpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wjq.wjqpicturebackend.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.wjq.wjqpicturebackend.model.domain.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wjq.wjqpicturebackend.model.domain.User;
import com.wjq.wjqpicturebackend.model.dto.picture.*;
import com.wjq.wjqpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author wjq23
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-02-26 14:41:18
 */
public interface PictureService extends IService<Picture> {


    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 将请求对象转换为QueryWrapper对象
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);


    /**
     * 获取单个图片的封装
     *
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装
     *
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 图片校验
     *
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void pictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 批量抓取和创建图片
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    void deletePicture(long pictureId, User loginUser);

//    void checkPictureAuth(User loginUser, Picture picture);

    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     *创建扩图任务
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);
}
