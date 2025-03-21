package com.wjq.wjqpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wjq.wjqpicturebackend.annotation.AuthCheck;
import com.wjq.wjqpicturebackend.common.BaseResponse;
import com.wjq.wjqpicturebackend.common.DeleteRequest;
import com.wjq.wjqpicturebackend.common.ResultUtils;
import com.wjq.wjqpicturebackend.constant.UserConstant;
import com.wjq.wjqpicturebackend.exception.BusinessException;
import com.wjq.wjqpicturebackend.exception.ErrorCode;
import com.wjq.wjqpicturebackend.exception.ThrowUtils;
import com.wjq.wjqpicturebackend.model.domain.User;
import com.wjq.wjqpicturebackend.model.dto.user.*;
import com.wjq.wjqpicturebackend.model.vo.LoginUserVO;
import com.wjq.wjqpicturebackend.model.vo.UserVO;
import com.wjq.wjqpicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        long userId = userService.userRegister(userRegisterRequest.getUserAccount(),
                userRegisterRequest.getUserPassword(), userRegisterRequest.getCheckPassword());
        return ResultUtils.success(userId);
    }

    /**
     *用户登录
     * @param userLoginRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> usrLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO userVO = userService.userLogin(userLoginRequest.getUserAccount(),
                userLoginRequest.getUserPassword(), httpServletRequest);
        return ResultUtils.success(userVO);
    }

    /**
     * 获取当前用户登录状态
     * @param httpServletRequest
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getUserLoginState(HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户退出登录
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest httpServletRequest) {
        userService.userLogout(httpServletRequest);
        return ResultUtils.success(true);
    }

    /**
     * 创建用户
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest,user);
        final String DEFAULT_PASSWORD = "123456";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean userSave = userService.save(user);
        ThrowUtils.throwIf(!userSave,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }


    /**
     * 根据id查询用户
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <=0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 删除用户
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <=0, ErrorCode.PARAMS_ERROR);
        boolean isDelete = userService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!isDelete,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新用户数据
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest){
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest,user);
        boolean isUpdate = userService.updateById(user);
        ThrowUtils.throwIf(!isUpdate,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页查询用户
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest){
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent(); //当前页码
        long pageSize = userQueryRequest.getPageSize(); // 每页显示多少条数据
        QueryWrapper<User> queryWrapper = userService.getQueryWrapper(userQueryRequest);
        Page<User> userPage = userService.page(new Page<User>(current, pageSize), queryWrapper);
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }
}
