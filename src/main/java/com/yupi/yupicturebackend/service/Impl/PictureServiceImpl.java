package com.yupi.yupicturebackend.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.common.FileManager;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.models.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.models.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.models.entity.Picture;
import com.yupi.yupicturebackend.models.entity.User;
import com.yupi.yupicturebackend.models.vo.PictureVo;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;

/**
* @author 24636
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-11-11 14:24:45
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{
    @Resource
    private FileManager fileManager;

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
        return PictureVo.toPictureVo( picture);
    }
}




