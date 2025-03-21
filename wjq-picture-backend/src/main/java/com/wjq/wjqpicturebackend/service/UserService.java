package com.wjq.wjqpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wjq.wjqpicturebackend.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wjq.wjqpicturebackend.model.dto.user.UserQueryRequest;
import com.wjq.wjqpicturebackend.model.vo.LoginUserVO;
import com.wjq.wjqpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author wjq23
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-02-24 11:30:12
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   账号
     * @param userPassword  密码
     * @param checkPassword 确认密码
     * @return
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);


    /**
     * 获得加密后的密码
     *
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录
     *
     * @param userAccount
     * @param userPassword
     * @param httpServletRequest
     * @return
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest httpServletRequest);

    /**
     * 获得登录用户脱敏后的信息
     *
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获得当前登录用户的信息
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户退出登录
     *
     * @param request
     * @return
     */
    void userLogout(HttpServletRequest request);

    /**
     * 获取脱敏后的用户信息，用于分页查询
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户列表
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 将查询请求转化为QueryWrapper对象
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 判断用户是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);
}
