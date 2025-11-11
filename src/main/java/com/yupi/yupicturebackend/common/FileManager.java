package com.yupi.yupicturebackend.common;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.models.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class FileManager {
    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    public UploadPictureResult uploadFile(MultipartFile file, String uploadPathPrefix) {
        ThrowUtils.throwIf(StrUtil.isBlank(uploadPathPrefix), ErrorCode.PARAMS_ERROR);
        // 1. 对图片信息进行校验
        validPhoto(file);
        // 2. 上传到 COS
        String uuid = RandomUtil.randomString(16);
        String fileName = file.getOriginalFilename();
        // 组装出文件名称
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(fileName));
        // 组装出上传文件路径
        String uploadFilePath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
        File tmpFile = null ;
        try {
            // 3. 返回包含了图片基本信息的结果
            tmpFile = File.createTempFile(uploadFilePath, null);
            file.transferTo(tmpFile);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilePath, tmpFile);
            // 封装返回结果
            UploadPictureResult result = new UploadPictureResult();
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            double picScale = NumberUtil.round(width * 1.0 / height,2).doubleValue();
            result.setPicName(FileUtil.mainName(fileName));
            result.setPicSize(FileUtil.size(tmpFile));
            result.setPicWidth(width);
            result.setPicHeight(height);
            result.setPicScale(picScale);
            result.setUrl(cosClientConfig.getHost() + "/" + uploadFilePath);
            result.setPicFormat(imageInfo.getFormat());
            return result;
        }catch (Exception e){
            log.error("file upload failed,filePath：" + uploadFilePath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        }finally {
            deleteFile(tmpFile);
        }
    }

    public void validPhoto(MultipartFile file) {
        // 1. 检验参数
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR);
        // 2. 校验文件大小
        final long MAX_SIZE = 1024 * 1024 * 2L;
        ThrowUtils.throwIf(file.getSize() > MAX_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
        // 3. 校验是否是允许的图片类型
        String suffix = FileUtil.getSuffix(file.getOriginalFilename()) ;
        final List<String> allowedFileTypes = Arrays.asList("jpg", "jpeg", "png","webp");
        ThrowUtils.throwIf(!allowedFileTypes.contains(suffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    public void deleteFile(File file) {
        if (file == null){
            return;
        }
        boolean delete = file.delete();
        if(!delete){
            log.error("file delete failed,filePath：" + file.getAbsolutePath());
        }
    }
}
