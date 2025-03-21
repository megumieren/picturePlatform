package com.wjq.wjqpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wjq.wjqpicturebackend.model.domain.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wjq.wjqpicturebackend.model.dto.spaceUser.SpaceUserAddRequest;
import com.wjq.wjqpicturebackend.model.dto.spaceUser.SpaceUserQueryRequest;
import com.wjq.wjqpicturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author wjq23
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-03-10 11:16:33
*/
public interface SpaceUserService extends IService<SpaceUser> {

    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    void validSpaceUser(SpaceUser spaceUser, boolean add);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
