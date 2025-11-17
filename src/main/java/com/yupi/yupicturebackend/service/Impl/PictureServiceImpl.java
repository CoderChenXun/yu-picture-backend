package com.yupi.yupicturebackend.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.common.CosManager;
import com.yupi.yupicturebackend.common.FileManager;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.manager.upload.FilePictureUpload;
import com.yupi.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.yupi.yupicturebackend.manager.upload.UrlPictureUpload;
import com.yupi.yupicturebackend.models.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.models.dto.picture.PictureQueryRequest;
import com.yupi.yupicturebackend.models.dto.picture.PictureReviewRequest;
import com.yupi.yupicturebackend.models.dto.picture.PictureUploadByBatchRequest;
import com.yupi.yupicturebackend.models.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.models.entity.Picture;
import com.yupi.yupicturebackend.models.entity.User;
import com.yupi.yupicturebackend.models.enums.PictureReviewStatusEnum;
import com.yupi.yupicturebackend.models.vo.PictureVo;
import com.yupi.yupicturebackend.models.vo.UserVo;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import com.yupi.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
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
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private CosManager cosManager;

    @Override
    public PictureVo uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 1. 检验参数
        ThrowUtils.throwIf(inputSource == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要检验图片是否存在,并检查用户权限
        if (pictureId != null) {
            Picture picture = this.getById(pictureId);
            ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        // 组装上传图片的路径前缀
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        // 上传图片，得到图片的基本信息,根据inputSource的类型判断使用哪个上传方法
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadFile(inputSource, uploadPathPrefix);
        // 2. 插入数据库，构建要插入数据的Picture信息
        Picture picture = new Picture();
        if (pictureId != null) {
            // 更新图片的同时需要更新图片的审核状态
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
        // 设置保存的缩略图url
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 设置保存的图片名称
        String picName = pictureUploadRequest.getPicName();
        if (StrUtil.isNotBlank(picName)) {
            picture.setName(picName);
        }
        // 填充审核参数
        fillReviewParams(picture, loginUser);
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
        // 补充的审核状态
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();

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
        // 补充的审核逻辑
        queryWrapper.eq(ObjectUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjectUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
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

    /**
     * 照片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 1. 进行参数检验
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        ThrowUtils.throwIf(ObjUtil.isEmpty(id) || ObjUtil.isEmpty(reviewStatus), ErrorCode.PARAMS_ERROR);
        // 2. 从数据库中查询出oldPicture
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 判断审核状态是否合法
        PictureReviewStatusEnum currentPictureReviewStatus = PictureReviewStatusEnum.getPictureReviewStatusEnum(reviewStatus);
        // 如果当前审核状态不合法或者当前审核状态为待审核，则为错误
        if (currentPictureReviewStatus == null || currentPictureReviewStatus.equals(PictureReviewStatusEnum.REVIEWING)) {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR);
        }
        // 如果当前审核状态和之前的审核状态相同，则为错误
        if (currentPictureReviewStatus.getValue() == oldPicture.getReviewStatus()) {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "请务重复审核");
        }
        // 4. 更新数据库
        Picture newPicture = new Picture();
        newPicture.setId(id);
        // 设置审核状态
        newPicture.setReviewStatus(reviewStatus);
        newPicture.setReviewMessage(reviewMessage);
        newPicture.setReviewerId(loginUser.getId());
        newPicture.setReviewTime(new Date());
        boolean result = this.updateById(newPicture);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);
    }

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动过审");
        } else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 1. 首先进行参数检验
        String searchText = pictureUploadByBatchRequest.getSearchText();
        int count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count <= 0, ErrorCode.PARAMS_ERROR, "上传数量不合法");
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多上传30张图片");
        // 2. 获取抓取的url
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);

        // 3. 根据fetchUrl抓取HTML文档
        Document document = null;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("抓取HTML文档失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "抓取HTML文档失败");
        }
        // 4. 解析HTML文件，取出图片url
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements elementList = div.select("img.mimg");
        int uploadCount = 0;
        // 如果namePrefix为空，则使用searchText作为namePrefix
        String fileNamePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(fileNamePrefix)) {
            fileNamePrefix = pictureUploadByBatchRequest.getSearchText();
        }
        for (Element element : elementList) {
            String fileUrl = element.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("图片url为空，已跳过{}", fileUrl);
                continue;
            }
            int indexOfQuestionMark = fileUrl.indexOf("?");
            if (indexOfQuestionMark > -1) {
                fileUrl = fileUrl.substring(0, indexOfQuestionMark);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            // 设置图片名称
            if (StrUtil.isNotBlank(fileNamePrefix)) {
                pictureUploadRequest.setPicName(fileNamePrefix + "-" + (uploadCount + 1));
            }
            try {
                PictureVo pictureVo = uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("上传图片成功，图片id为{}", pictureVo.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("上传图片失败，图片url为" + fileUrl, e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // FIXME 注意，这里的 url 包含了域名，实际上只要传 key 值（存储路径）就够了
        cosManager.deleteObject(oldPicture.getUrl());
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

}




