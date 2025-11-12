package com.yupi.yupicturebackend.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.common.FileManager;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.models.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.models.dto.picture.PictureQueryRequest;
import com.yupi.yupicturebackend.models.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.models.entity.Picture;
import com.yupi.yupicturebackend.models.entity.User;
import com.yupi.yupicturebackend.models.vo.PictureVo;
import com.yupi.yupicturebackend.models.vo.UserVo;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import com.yupi.yupicturebackend.service.UserService;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 24636
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-11-11 14:24:45
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Override
    public PictureVo uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 1. 检验参数
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要检验图片是否存在
        if(pictureId != null){
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        // 组装上传图片的路径前缀
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        // 上传图片，得到图片的基本信息
        UploadPictureResult uploadPictureResult = fileManager.uploadFile(multipartFile, uploadPathPrefix);
        // 2. 插入数据库，构建要插入数据的Picture信息
        Picture picture = new Picture();
        if(pictureId != null){
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 设置保存的url
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        boolean saveOrUpdate = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!saveOrUpdate, ErrorCode.SYSTEM_ERROR, "保存图片失败");
        // 3. 返回结果
        return PictureVo.toPictureVo(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("name", searchText))
                    .or()
                    .like("introduction", searchText);
        }
        queryWrapper.eq(ObjectUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picScale), "picScale", picScale);
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField),
                sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVo getPictureVo(Picture picture, HttpServletRequest request) {
        if (picture == null) {
            return null;
        }
        PictureVo pictureVo = PictureVo.toPictureVo(picture);
        // 获取用户Id和UserVo对象
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVo userVo = userService.getUserVo(user);
            pictureVo.setUser(userVo);
            pictureVo.setUserId(userId);
        }
        return pictureVo;
    }

    @Override
    public Page<PictureVo> getPictureVoPage(Page<Picture> page, HttpServletRequest request) {
        // 1. 检验参数
        ThrowUtils.throwIf(page == null, ErrorCode.PARAMS_ERROR);
        List<Picture> pageRecords = page.getRecords();
        Page<PictureVo> pictureVoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        // 如果数据为空，直接返回
        if (CollUtil.isEmpty(pageRecords)) {
            return pictureVoPage;
        }
        // 数据不为空,先转换为 VoList
        List<PictureVo> pictureVoList = pageRecords.stream().map(PictureVo::toPictureVo).collect(Collectors.toList());
        // 把pictureList中的userId集合获取出来，去数据库查询用户信息
        Set<Long> userIdSet = pageRecords.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 将pictureVoList与userList进行关联
        pictureVoList.forEach(
                pictureVo -> {
                    // 取出用户id
                    Long userId = pictureVo.getUserId();
                    User user = null;
                    if (userMap.containsKey(userId)) {
                        user = userMap.get(userId).get(0);
                    }
                    pictureVo.setUser(userService.getUserVo(user));
                }
        );
        pictureVoPage.setRecords(pictureVoList);
        return pictureVoPage;
    }

    /**
     * 编写照片验证方法，要求在更新和修改图片时验证图片信息
     *
     * @param picture
     */
    @Override
    public void ValidPicture(Picture picture) {
        // 1. 参数检验
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 2. 验证图片信息
        Long pictureId = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        ThrowUtils.throwIf(ObjectUtil.isEmpty(pictureId), ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }
}




