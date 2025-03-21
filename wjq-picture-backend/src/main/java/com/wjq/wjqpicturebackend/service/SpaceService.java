package com.wjq.wjqpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wjq.wjqpicturebackend.model.domain.Space;
import com.wjq.wjqpicturebackend.model.domain.User;
import com.wjq.wjqpicturebackend.model.dto.space.SpaceAddRequest;
import com.wjq.wjqpicturebackend.model.dto.space.SpaceQueryRequest;
import com.wjq.wjqpicturebackend.model.vo.SpaceVO;


import javax.servlet.http.HttpServletRequest;

/**
* @author wjq23
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-03-03 22:31:16
*/
public interface SpaceService extends IService<Space> {


    Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);


    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);


    /**
     * 获取单个空间的封装
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 分页获取空间封装
     *
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    void fillSpaceBySpaceLevel(Space space);

    /**
     * 空间校验
     *
     * @param space
     */
    void validSpace(Space space);

    /**
     * 空间权限校验
     */
    void checkSpaceAuth(Space space,User loginUser);

}
