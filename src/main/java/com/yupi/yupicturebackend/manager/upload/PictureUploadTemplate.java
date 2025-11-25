package com.yupi.yupicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.yupi.yupicturebackend.common.CosManager;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.models.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;


@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    public UploadPictureResult uploadFile(Object inputSource, String uploadPathPrefix) {
        ThrowUtils.throwIf(StrUtil.isBlank(uploadPathPrefix), ErrorCode.PARAMS_ERROR);
        // 1. 对图片信息进行校验
        validPhoto(inputSource);
        // 2. 上传到 COS
        String uuid = RandomUtil.randomString(16);
        String fileName = getOriginalFileName(inputSource);
        // 组装出文件名称
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(fileName));
        // 组装出上传文件路径
        String uploadFilePath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
        File tmpFile = null ;
        try {
            // 3. 返回包含了图片基本信息的结果
            tmpFile = File.createTempFile(uploadFilePath, null);
            transferFileOrUrlToTmpFile(inputSource, tmpFile);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilePath, tmpFile);
            // 取出imageInfo
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 4. 封装返回结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                // 获取压缩图返回结果
                CIObject compressedCiObject = objectList.get(0);
                CIObject thumbnailCiObject = compressedCiObject;
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
                return getUploadPictureResult(fileName, compressedCiObject, thumbnailCiObject, imageInfo);
            }
            return getUploadPictureResult(fileName, uploadFilePath, tmpFile, putObjectResult);
        }catch (Exception e){
            log.error("file upload failed,filePath：" + uploadFilePath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        }finally {
            deleteFile(tmpFile);
        }
    }

    private UploadPictureResult getUploadPictureResult(String fileName, String uploadFilePath, File tmpFile, PutObjectResult putObjectResult) {
        UploadPictureResult result = new UploadPictureResult();
        ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
        result.setPicName(FileUtil.mainName(fileName));
        result.setPicSize(FileUtil.size(tmpFile));
        result.setPicWidth(width);
        result.setPicHeight(height);
        result.setPicScale(picScale);
        result.setUrl(cosClientConfig.getHost() + "/" + uploadFilePath);
        result.setPicFormat(imageInfo.getFormat());
        // 添加的图片主色调处理
        result.setPicColor(imageInfo.getAve());
        return result;
    }

    private UploadPictureResult getUploadPictureResult(String fileName, CIObject compressedCiObject, CIObject thumbnailCiObject, ImageInfo imageInfo) {
        UploadPictureResult result = new UploadPictureResult();
        int width = compressedCiObject.getWidth();
        int height = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
        result.setPicName(FileUtil.mainName(fileName));
        result.setPicSize(compressedCiObject.getSize().longValue());
        result.setPicWidth(width);
        result.setPicHeight(height);
        result.setPicScale(picScale);
        result.setPicFormat(compressedCiObject.getFormat());
        // 添加的图片主色调处理
        result.setPicColor(imageInfo.getAve());
        result.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        result.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        return result;
    }

    /**
     * 将文件或url转成临时文件
     * @param inputSource
     * @param tmpFile
     */
    protected abstract void transferFileOrUrlToTmpFile(Object inputSource, File tmpFile);

    /**
     * 获取原始文件名
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFileName(Object inputSource);

    /**
     * 校验图片信息
     * @param inputSource
     */
    protected abstract void validPhoto(Object inputSource);

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
