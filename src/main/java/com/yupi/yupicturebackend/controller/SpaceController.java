package com.yupi.yupicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.DeleteRequest;
import com.yupi.yupicturebackend.common.ResultUtils;
import com.yupi.yupicturebackend.constant.UserConstant;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.models.dto.space.*;
import com.yupi.yupicturebackend.models.entity.Space;
import com.yupi.yupicturebackend.models.entity.User;
import com.yupi.yupicturebackend.models.enums.SpaceLevelEnum;
import com.yupi.yupicturebackend.models.vo.SpaceVo;
import com.yupi.yupicturebackend.service.SpaceService;
import com.yupi.yupicturebackend.service.UserService;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        // 1. 首先进行参数检验
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long newSpaceId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(newSpaceId);
    }

    /**
     * 删除空间
     *
     * @param deleteRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest httpServletRequest) {
        // 1. 检验传入的参数是否合法
        ThrowUtils.throwIf(ObjectUtil.isEmpty(deleteRequest), ErrorCode.PARAMS_ERROR);
        if (deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR);
        }
        // 2. 查询空间
        Space space = spaceService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 检查身份
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 3.1 如果当前用户不为管理员用户并且不是该空间的所属用户，则不允许删除
        if (!userService.isAdmin(loginUser) && !space.getUserId().equals(loginUser.getId())) {
            return ResultUtils.error(ErrorCode.NO_AUTH_ERROR);
        }
        // 3.2 删除该张空间的数据库信息
        boolean result = spaceService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新空间
     *
     * @param spaceUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        // 1. 检验参数
        ThrowUtils.throwIf(spaceUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        if (spaceUpdateRequest.getId() == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 将实体类和dto对象进行转换
        Space space = new Space();
        BeanUtil.copyProperties(spaceUpdateRequest, space);
        // 数据校验
        spaceService.validSpace(space, false);
        // 填充maxCount和maxSize
        spaceService.fillSpaceBySpaceLevel(space);
        // 3. 查询是否有该id的空间
        Space oldSpace = spaceService.getById(spaceUpdateRequest.getId());
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 有匹配上的空间
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 通过id获取空间(脱敏后的信息)
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVo> getSpaceVoById(long id, HttpServletRequest request) {
        // 1. 检验参数
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 2. 获取空间
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 获取空间的vo
        SpaceVo spaceVo = spaceService.getSpaceVo(space, request);
        return ResultUtils.success(spaceVo);
    }


    /**
     * 获取空间列表
     *
     * @param spaceQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpace(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        // 1. 检验参数
        ThrowUtils.throwIf(ObjectUtil.isEmpty(spaceQueryRequest), ErrorCode.PARAMS_ERROR);
        // 2. 获得查询的queryWrapper
        QueryWrapper<Space> queryWrapper = spaceService.getQueryWrapper(spaceQueryRequest);
        Page<Space> page = new Page<>(spaceQueryRequest.getCurrent(), spaceQueryRequest.getPageSize());
        Page<Space> spacePage = spaceService.page(page, queryWrapper);
        ThrowUtils.throwIf(spacePage == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spacePage);
    }

    /**
     * 获取空间列表(脱敏后的信息)
     *
     * @param spaceQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVo>> listSpaceVo(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        // 1. 检验参数
        ThrowUtils.throwIf(ObjectUtil.isEmpty(spaceQueryRequest), ErrorCode.PARAMS_ERROR);
        // 2. 获得查询的queryWrapper
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        // 静止爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        QueryWrapper<Space> queryWrapper = spaceService.getQueryWrapper(spaceQueryRequest);
        Page<Space> page = new Page<>(current, pageSize);
        Page<Space> spacePage = spaceService.page(page, queryWrapper);
        ThrowUtils.throwIf(spacePage == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 获取spaceVo
        Page<SpaceVo> spaceVoPage = spaceService.getSpaceVoPage(spacePage, request);
        return ResultUtils.success(spaceVoPage);
    }


    /**
     * 编辑空间
     *
     * @param spaceEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        // 1. 参数检验
        ThrowUtils.throwIf(ObjectUtil.isEmpty(spaceEditRequest), ErrorCode.PARAMS_ERROR);
        // 2. 将实体类和dto对象进行转换
        Space space = new Space();
        BeanUtil.copyProperties(spaceEditRequest, space);
        // 添加修改时间
        space.setEditTime(new Date());
        User loginUser = userService.getLoginUser(request);
        // 进行数据校验
        spaceService.validSpace(space, false);
        // 3. 查询数据库中是否有该id的空间
        Space oldSpace = spaceService.getById(spaceEditRequest.getId());
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 4. 判断用户是否有权限修改该空间（仅本人和管理员可以编辑）
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 4. 更新数据库中的空间信息
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

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

