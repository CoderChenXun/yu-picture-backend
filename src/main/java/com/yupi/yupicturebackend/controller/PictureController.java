package com.yupi.yupicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
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
import com.yupi.yupicturebackend.models.dto.picture.PictureEditRequest;
import com.yupi.yupicturebackend.models.dto.picture.PictureQueryRequest;
import com.yupi.yupicturebackend.models.dto.picture.PictureUpdateRequest;
import com.yupi.yupicturebackend.models.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.models.entity.Picture;
import com.yupi.yupicturebackend.models.entity.User;
import com.yupi.yupicturebackend.models.vo.PictureTagCategory;
import com.yupi.yupicturebackend.models.vo.PictureVo;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    /**
     * 上传图片
     * @param multipartFile
     * @return
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVo> upload(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, HttpServletRequest httpServletRequest) {
        // 1. 检验参数
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR);
        // 2. 查询当前登录的角色
        User loginUser = userService.getLoginUser(httpServletRequest);
        PictureVo pictureVo = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVo);
    }

    /**
     * 删除图片
     *
     * @param deleteRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest httpServletRequest) {
        // 1. 检验传入的参数是否合法
        ThrowUtils.throwIf(ObjectUtil.isEmpty(deleteRequest), ErrorCode.PARAMS_ERROR);
        if (deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR);
        }
        // 2. 查询图片
        Picture picture = pictureService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 检查身份
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 3.1 如果当前用户不为管理员用户并且不是该照片的所属用户，则不允许删除
        if (!userService.isAdmin(loginUser) && !picture.getUserId().equals(loginUser.getId())) {
            return ResultUtils.error(ErrorCode.NO_AUTH_ERROR);
        }
        // 3.2 删除该张图片的数据库信息
        boolean result = pictureService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest) {
        // 1. 检验参数
        ThrowUtils.throwIf(ObjectUtil.isEmpty(pictureUpdateRequest), ErrorCode.PARAMS_ERROR);
        if (pictureUpdateRequest.getId() == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 将实体类和dto对象进行转换
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.ValidPicture(picture);
        // 3. 查询是否有该id的 图片
        Picture oldPicture = pictureService.getById(pictureUpdateRequest.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 有匹配上的图片
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 通过id获取图片(脱敏后的信息)
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVo> getPictureVoById(long id,HttpServletRequest request) {
        // 1. 检验参数
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 2. 获取图片
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 获取图片的vo
        PictureVo pictureVo = pictureService.getPictureVo(picture, request);
        return ResultUtils.success(pictureVo);
    }


    /**
     * 获取图片列表
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPicture(@RequestBody PictureQueryRequest pictureQueryRequest) {
        // 1. 检验参数
        ThrowUtils.throwIf(ObjectUtil.isEmpty(pictureQueryRequest), ErrorCode.PARAMS_ERROR);
        // 2. 获得查询的queryWrapper
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        Page< Picture> page = new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize());
        Page<Picture> picturePage = pictureService.page(page, queryWrapper);
        ThrowUtils.throwIf(picturePage == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(picturePage);
    }

    /**
     * 获取图片列表(脱敏后的信息)
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVo>> listPictureVo(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        // 1. 检验参数
        ThrowUtils.throwIf(ObjectUtil.isEmpty(pictureQueryRequest), ErrorCode.PARAMS_ERROR);
        // 2. 获得查询的queryWrapper
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        // 静止爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        Page<Picture> page = new Page<>(current, pageSize);
        Page<Picture> picturePage = pictureService.page(page, queryWrapper);
        ThrowUtils.throwIf(picturePage == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 获取pictureVo
        Page<PictureVo> pictureVoPage = pictureService.getPictureVoPage(picturePage, request);
        return ResultUtils.success(pictureVoPage);
    }

    /**
     * 编辑图片
     * @param pictureEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        // 1. 参数检验
        ThrowUtils.throwIf(ObjectUtil.isEmpty(pictureEditRequest), ErrorCode.PARAMS_ERROR);
        // 2. 将实体类和dto对象进行转换
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureEditRequest, picture);
        // 将前端收集的List<String>转换成JSON字符串
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 添加修改时间
        picture.setEditTime(new Date());
        // 进行数据校验
        pictureService.ValidPicture(picture);
        // 3. 查询数据库中是否有该id的图片
        Picture oldPicture = pictureService.getById(pictureEditRequest.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 4. 判断用户是否有权限修改该图片（仅本人和管理员可以编辑）
        User loginUser = userService.getLoginUser(request);
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 4. 更新数据库中的图片信息
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 获取标签和分类
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

}
