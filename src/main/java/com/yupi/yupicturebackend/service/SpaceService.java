package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.models.dto.space.SpaceAddRequest;
import com.yupi.yupicturebackend.models.dto.space.SpaceQueryRequest;
import com.yupi.yupicturebackend.models.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.models.entity.User;
import com.yupi.yupicturebackend.models.vo.SpaceVo;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 24636
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-11-18 14:20:50
 */
public interface SpaceService extends IService<Space> {

    Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    void validSpace(Space space, boolean add);

    void fillSpaceBySpaceLevel(Space space);

    SpaceVo getSpaceVo(Space space, HttpServletRequest request);

    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    Page<SpaceVo> getSpaceVoPage(Page<Space> spacePage, HttpServletRequest request);
}
