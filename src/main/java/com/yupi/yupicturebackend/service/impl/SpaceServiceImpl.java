package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.models.dto.space.SpaceAddRequest;
import com.yupi.yupicturebackend.models.dto.space.SpaceQueryRequest;
import com.yupi.yupicturebackend.models.entity.Space;
import com.yupi.yupicturebackend.models.entity.User;
import com.yupi.yupicturebackend.models.enums.SpaceLevelEnum;
import com.yupi.yupicturebackend.models.vo.SpaceVo;
import com.yupi.yupicturebackend.models.vo.UserVo;
import com.yupi.yupicturebackend.service.SpaceService;
import com.yupi.yupicturebackend.mapper.SpaceMapper;
import com.yupi.yupicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 24636
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-11-18 14:20:50
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 1. 新增空间
        Space space = new Space();
        String spaceName = spaceAddRequest.getSpaceName();
        Integer spaceLevel = spaceAddRequest.getSpaceLevel();
        if (StrUtil.isBlank(spaceName)) {
            space.setSpaceName("默认空间");
        }
        if (ObjUtil.isEmpty(spaceLevel)) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        space.setSpaceName(spaceName);
        space.setSpaceLevel(spaceLevel);
        // 1.1 进行数据检验
        this.validSpace(space, true);
        // 1.2 填充spaceLevel信息
        this.fillSpaceBySpaceLevel(space);
        // 2. 更新数据库
        // 2.1 获取当前登录用户id
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权创建指定级别的空间");
        }
        // 3. 每个用户只能创建一个空间
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            long newSpaceId = transactionTemplate.execute(status -> {
                // 3.1 查询当前用户是否创建了空间
                boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                if (exists) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户已创建空间");
                }
                // 3.2 新增空间
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                return space.getId();
            });
            return newSpaceId;
        }
    }

    /**
     * 校验新增或修改的Space参数
     *
     * @param space
     * @param add
     */
    @Override
    public void validSpace(Space space, boolean add) {
        // 1. 进行参数检验
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum enumByValue = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 2. 判断是新增还是更新操作
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (ObjUtil.isEmpty(spaceLevel)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间等级不能为空");
            }
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        if (ObjUtil.isNotEmpty(spaceLevel) && enumByValue == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间等级错误");
        }
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 1. 进行参数检验
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum enumByValue = SpaceLevelEnum.getEnumByValue(spaceLevel);
        if (enumByValue != null) {
            if (space.getMaxCount() == null) {
                space.setMaxCount(enumByValue.getMaxCount());
            }
            if (space.getMaxSize() == null) {
                space.setMaxSize(enumByValue.getMaxSize());
            }
        }
    }

    /**
     * 获取获取脱敏后的空间信息
     *
     * @param space
     * @param request
     * @return
     */
    @Override
    public SpaceVo getSpaceVo(Space space, HttpServletRequest request) {
        // 1. 检验参数是否合法
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 2. 通过vo类的静态方法将实体类转换为dto
        SpaceVo spaceVo = SpaceVo.objToVo(space);
        // 3. 获取脱敏后的用户信息
        User loginUser = userService.getLoginUser(request);
        UserVo userVo = userService.getUserVo(loginUser);
        spaceVo.setUser(userVo);
        return spaceVo;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        // 1. 检验参数
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 2. 取出要查询的字段
        Long id = spaceQueryRequest.getId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Long userId = spaceQueryRequest.getUserId();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        // 3. 创建查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        // 4. 返回查询条件
        return queryWrapper;
    }

    /**
     * 获取分页脱敏后的space信息
     *
     * @param spacePage
     * @param request
     * @return
     */
    @Override
    public Page<SpaceVo> getSpaceVoPage(Page<Space> spacePage, HttpServletRequest request) {
        // 1. 参数检验
        ThrowUtils.throwIf(spacePage == null, ErrorCode.PARAMS_ERROR);
        long current = spacePage.getCurrent();
        long pageSize = spacePage.getSize();
        long total = spacePage.getTotal();
        Page<SpaceVo> spaceVoPage = new Page<>(current, pageSize, total);
        List<Space> spaceList = spacePage.getRecords();
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVoPage;
        }
        // 2. 获取recodes
        List<SpaceVo> spaceVoList = spaceList.stream().map(SpaceVo::objToVo).collect(Collectors.toList());
        // 2.1 获取recodes中的用户id信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        // 2.2 通过userIdSet获取用户信息
        Map<Long, List<UserVo>> userVoMap = userService.listByIds(userIdSet).stream().map(userService::getUserVo).collect(Collectors.groupingBy(UserVo::getId));
        // 2.4 将userVoList和spaceVoList进行关联
        spaceVoList.forEach(spaceVo -> {
            Long userId = spaceVo.getUserId();
            UserVo userVo = null;
            if (userVoMap.containsKey(userId)) {
                userVo = userVoMap.get(userId).get(0);
            }
            spaceVo.setUser(userVo);
        });
        spaceVoPage.setRecords(spaceVoList);
        return spaceVoPage;
    }
}




