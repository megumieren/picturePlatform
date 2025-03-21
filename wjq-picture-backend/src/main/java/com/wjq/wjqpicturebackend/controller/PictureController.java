package com.wjq.wjqpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wjq.wjqpicturebackend.annotation.AuthCheck;
import com.wjq.wjqpicturebackend.api.aliyun.AliYunAiApi;
import com.wjq.wjqpicturebackend.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.wjq.wjqpicturebackend.api.aliyun.model.GetOutPaintingTaskResponse;
import com.wjq.wjqpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.wjq.wjqpicturebackend.api.imagesearch.GetImageByImageThrough360;
import com.wjq.wjqpicturebackend.common.BaseResponse;
import com.wjq.wjqpicturebackend.common.DeleteRequest;
import com.wjq.wjqpicturebackend.common.ResultUtils;
import com.wjq.wjqpicturebackend.constant.SpaceUserPermissionConstant;
import com.wjq.wjqpicturebackend.constant.UserConstant;
import com.wjq.wjqpicturebackend.exception.BusinessException;
import com.wjq.wjqpicturebackend.exception.ErrorCode;
import com.wjq.wjqpicturebackend.exception.ThrowUtils;
import com.wjq.wjqpicturebackend.manager.auth.SpaceUserAuthManager;
import com.wjq.wjqpicturebackend.manager.auth.StpKit;
import com.wjq.wjqpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.wjq.wjqpicturebackend.model.domain.Picture;
import com.wjq.wjqpicturebackend.model.domain.Space;
import com.wjq.wjqpicturebackend.model.domain.User;
import com.wjq.wjqpicturebackend.model.dto.picture.*;
import com.wjq.wjqpicturebackend.model.enums.PictureReviewStatusEnum;
import com.wjq.wjqpicturebackend.model.vo.PictureTagCategory;
import com.wjq.wjqpicturebackend.model.vo.PictureVO;
import com.wjq.wjqpicturebackend.service.PictureService;
import com.wjq.wjqpicturebackend.service.SpaceService;
import com.wjq.wjqpicturebackend.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SpaceService spaceService;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    //构造本地缓存（在jvm中）
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

    /**
     * 上传图片
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 通过 URL 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }


    /**
     * 删除图片
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        Long pictureId = deleteRequest.getId();
        pictureService.deletePicture(pictureId,user);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员可用）
     *
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        //检验合法性
        pictureService.validPicture(picture);
        //判断数据库中是否存在对应id图片
        Long pictureId = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        //补充审核参数
        User loginUser = userService.getLoginUser(httpServletRequest);
        pictureService.fillReviewParams(picture,loginUser);

        //操作数据库
        boolean isUpdate = pictureService.updateById(picture);
        ThrowUtils.throwIf(!isUpdate, ErrorCode.OPERATION_ERROR, "更新图片失败");
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片（用户使用）
     *
     * @param pictureEditRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(pictureEditRequest == null || pictureEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        pictureService.editPicture(pictureEditRequest,userService.getLoginUser(httpServletRequest));
        return ResultUtils.success(true);
    }

    /**
     * 根据id查找图片（管理员可用）
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return ResultUtils.success(picture);
    }

    /**
     * 根据id查找图片（封装类）
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
//        //只能查询通过审核的图片
//        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
//        pictureQueryWrapper.eq("id",id)
//                .eq("reviewStatus",PictureReviewStatusEnum.PASS.getValue());
//        Picture picture = pictureService.getOne(pictureQueryWrapper);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        if(spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }
        // 获取封装类
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        Space space = spaceService.getById(id);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, userService.getLoginUser(request));
        pictureVO.setPermissionList(permissionList);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页获取图片信息（管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPicturePage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null,ErrorCode.PARAMS_ERROR);
        Page<Picture> picturePage = new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize());
        pictureService.page(picturePage, pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片信息（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOPage(@RequestBody PictureQueryRequest pictureQueryRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制爬虫
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() > 20, ErrorCode.PARAMS_ERROR,"单页显示图片量超上限");
        Page<Picture> picturePage = new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize());
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 公开图库
        if (spaceId == null) {
            // 普通用户默认只能查看已过审的公开数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }
        pictureService.page(picturePage, pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 分页获取图片信息（封装类）
     * 使用多级缓存
     */
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null,ErrorCode.PARAMS_ERROR);
        // 限制爬虫
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() > 20, ErrorCode.PARAMS_ERROR,"单页显示图片量超上限");
        Page<Picture> picturePage = new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize());
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        //构建缓存key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = "wjqpicture:listPictureVOByPage:" + hashKey;
        //1.先查本地缓存
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if(cachedValue != null){
            //如果缓存命中,则返回结果
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        //2.如果未命中则查redis缓存
        cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if(cachedValue != null){
            //如果缓存命中,更新本地缓存，返回结果
            LOCAL_CACHE.put(cacheKey, cachedValue);
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        //3.如果未命中，则先查数据库，然后将数据保存到redis和本地缓存中
        pictureService.page(picturePage, pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 5 - 10 分钟随机过期，防止雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0 , 300);
        //将数据存入redis
        stringRedisTemplate.opsForValue().set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
        //将数据存入本地缓存中
        LOCAL_CACHE.put(cacheKey, cacheValue);
        return ResultUtils.success(pictureVOPage);
    }
    /**
     * 获取预置标签和分类
     * 在项目前期规模不大的时候，我们没必要将标签和分类单独用数据表来维护了，
     * 直接在 PictureController 中写一个接口，返回预设的固定数据即可
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 管理员审核图片
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.pictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 管理员批量上传图片
     * @param pictureUploadByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/search/picture")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = GetImageByImageThrough360.getImagePageUrl(oldPicture.getUrl());
        return ResultUtils.success(resultList);
    }

    /**
     * 以颜色搜图
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByPicture(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest,HttpServletRequest request){
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        List<PictureVO> pictureVOList = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(pictureVOList);
    }

    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request) {
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }



}
