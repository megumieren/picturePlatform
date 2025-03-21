package com.wjq.wjqpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wjq.wjqpicturebackend.annotation.AuthCheck;
import com.wjq.wjqpicturebackend.common.BaseResponse;
import com.wjq.wjqpicturebackend.common.DeleteRequest;
import com.wjq.wjqpicturebackend.common.ResultUtils;
import com.wjq.wjqpicturebackend.constant.UserConstant;
import com.wjq.wjqpicturebackend.exception.BusinessException;
import com.wjq.wjqpicturebackend.exception.ErrorCode;
import com.wjq.wjqpicturebackend.exception.ThrowUtils;
import com.wjq.wjqpicturebackend.manager.auth.SpaceUserAuthManager;
import com.wjq.wjqpicturebackend.model.domain.Picture;
import com.wjq.wjqpicturebackend.model.domain.Space;
import com.wjq.wjqpicturebackend.model.domain.SpaceUser;
import com.wjq.wjqpicturebackend.model.domain.User;
import com.wjq.wjqpicturebackend.model.dto.picture.*;
import com.wjq.wjqpicturebackend.model.dto.space.SpaceAddRequest;
import com.wjq.wjqpicturebackend.model.dto.space.SpaceEditRequest;
import com.wjq.wjqpicturebackend.model.dto.space.SpaceQueryRequest;
import com.wjq.wjqpicturebackend.model.dto.space.SpaceUpdateRequest;
import com.wjq.wjqpicturebackend.model.enums.PictureReviewStatusEnum;
import com.wjq.wjqpicturebackend.model.enums.SpaceLevelEnum;
import com.wjq.wjqpicturebackend.model.vo.PictureTagCategory;
import com.wjq.wjqpicturebackend.model.vo.PictureVO;
import com.wjq.wjqpicturebackend.model.vo.SpaceLevel;
import com.wjq.wjqpicturebackend.model.vo.SpaceVO;
import com.wjq.wjqpicturebackend.service.PictureService;
import com.wjq.wjqpicturebackend.service.SpaceService;
import com.wjq.wjqpicturebackend.service.SpaceUserService;
import com.wjq.wjqpicturebackend.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
public class SpaceController {
    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;


    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private SpaceUserService spaceUserService;

    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long spaceId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(spaceId);
    }

    /**
     * 删除空间
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @Transactional
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        Long spaceId = deleteRequest.getId();
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        //仅管理员和普通用户可以删除
        spaceService.checkSpaceAuth(space, user);
        boolean isDelete = spaceService.removeById(spaceId);
        ThrowUtils.throwIf(!isDelete, ErrorCode.OPERATION_ERROR, "删除空间失败");
        // 删除空间时同时删除里面的图片
        Long pictureCount = pictureService.lambdaQuery().eq(Picture::getSpaceId, spaceId).count();
        if (pictureCount > 0) {
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("spaceId", spaceId);
            List<Picture> list = pictureService.list(queryWrapper);
            boolean isPictureDelete = pictureService.removeByIds(list);
            ThrowUtils.throwIf(!isPictureDelete, ErrorCode.OPERATION_ERROR, "删除空间所有图片失败");
        }
        //删除spaceUser表中的数据
        Long spaceUserCount = spaceUserService.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, user.getId())
                .count();
        if (spaceUserCount > 0) {
            LambdaUpdateWrapper<SpaceUser> spaceUserLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            spaceUserLambdaUpdateWrapper.eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, user.getId());
            boolean result = spaceUserService.remove(spaceUserLambdaUpdateWrapper);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除关联失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 更新空间（仅管理员可用）
     *
     * @param spaceUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        ThrowUtils.throwIf(spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Space space = new Space();
        BeanUtil.copyProperties(spaceUpdateRequest, space);
        //检验合法性
        spaceService.validSpace(space);
        //判断数据库中是否存在对应id空间
        Long spaceId = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        //自动填充空间数据
        spaceService.fillSpaceBySpaceLevel(space);

        //操作数据库
        boolean isUpdate = spaceService.updateById(space);
        ThrowUtils.throwIf(!isUpdate, ErrorCode.OPERATION_ERROR, "更新空间失败");
        return ResultUtils.success(true);
    }

    /**
     * 编辑空间（用户使用）
     *
     * @param spaceEditRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(spaceEditRequest == null || spaceEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Space space = new Space();
        BeanUtil.copyProperties(spaceEditRequest, space);
        //设置编辑时间
        space.setEditTime(new Date());
        //检验合法性
        spaceService.validSpace(space);
        //判断数据库中是否存在对应id空间
        Long spaceId = spaceEditRequest.getId();
        Space oldSpace = spaceService.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);

        User loginUser = userService.getLoginUser(httpServletRequest);
        //仅管理员和普通用户可以编辑
        spaceService.checkSpaceAuth(space, loginUser);
        //自动填充空间数据
        spaceService.fillSpaceBySpaceLevel(space);

        boolean isUpdate = spaceService.updateById(space);
        ThrowUtils.throwIf(!isUpdate, ErrorCode.OPERATION_ERROR, "更新空间失败");
        return ResultUtils.success(true);
    }

    /**
     * 根据id查找空间（管理员可用）
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        return ResultUtils.success(space);
    }

    /**
     * 根据id查找空间（封装类）
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        spaceVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取空间信息（管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpacePage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<Space> spacePage = new Page<>(spaceQueryRequest.getCurrent(), spaceQueryRequest.getPageSize());
        spaceService.page(spacePage, spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }

    /**
     * 分页获取空间信息（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOPage(@RequestBody SpaceQueryRequest spaceQueryRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制爬虫
        ThrowUtils.throwIf(spaceQueryRequest.getPageSize() > 20, ErrorCode.PARAMS_ERROR, "单页显示空间量超上限");
        Page<Space> spacePage = new Page<>(spaceQueryRequest.getCurrent(), spaceQueryRequest.getPageSize());
        spaceService.page(spacePage, spaceService.getQueryWrapper(spaceQueryRequest));
        Page<SpaceVO> spaceVOPage = spaceService.getSpaceVOPage(spacePage, request);
        return ResultUtils.success(spaceVOPage);
    }

    /**
     * 用于给前端展示所有的空间级别信息
     *
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()) // 获取所有枚举
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }


}
