package com.yupi.yupicturebackend.common;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@Deprecated
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
            double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
            result.setPicName(FileUtil.mainName(fileName));
            result.setPicSize(FileUtil.size(tmpFile));
            result.setPicWidth(width);
            result.setPicHeight(height);
            result.setPicScale(picScale);
            result.setUrl(cosClientConfig.getHost() + "/" + uploadFilePath);
            result.setPicFormat(imageInfo.getFormat());
            return result;
        } catch (Exception e) {
            log.error("file upload failed,filePath：" + uploadFilePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteFile(tmpFile);
        }
    }

    public UploadPictureResult uploadFile(String fileUrl, String uploadPathPrefix) {
        ThrowUtils.throwIf(StrUtil.isBlank(uploadPathPrefix), ErrorCode.PARAMS_ERROR);
        // TODO：校验url信息
        validPhoto(fileUrl);
        // 2. 上传到 COS
        String uuid = RandomUtil.randomString(16);
        // TODO 获取文件名
        String fileName = FileUtil.mainName(fileUrl);
        // 组装出文件名称
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(fileName));
        // 组装出上传文件路径
        String uploadFilePath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
        File tmpFile = null;
        try {
            // 3. 返回包含了图片基本信息的结果
            tmpFile = File.createTempFile(uploadFilePath, null);
            // 保存临时文件
            HttpUtil.downloadFile(fileUrl, tmpFile);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilePath, tmpFile);
            // 封装返回结果
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
            return result;
        } catch (Exception e) {
            log.error("file upload failed,filePath：" + uploadFilePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
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
        String suffix = FileUtil.getSuffix(file.getOriginalFilename());
        final List<String> allowedFileTypes = Arrays.asList("jpg", "jpeg", "png", "webp");
        ThrowUtils.throwIf(!allowedFileTypes.contains(suffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    public void validPhoto(String fileUrl) {
        // 检验url
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");

        // 1. 验证url格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式错误");
        }
        // 2. 检查url的协议是否是 http 或 https
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"), ErrorCode.PARAMS_ERROR, "文件地址协议错误");
        // 3. 发送head请求
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                // 有可能是不支持head协议，认为检验通过
                return;
            }
            // 4. 检查文件类型是否符合规范
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                final List<String> allowedFileTypes = Arrays.asList("image/jpg", "image/jpeg", "image/png", "image/webp");
                ThrowUtils.throwIf(!allowedFileTypes.contains(contentType.toLowerCase()), ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            // 5. 检查文件大小是否符合规范
            String contentLength = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLength)) {
                try {
                    long fileSize = Long.parseLong(contentLength);
                    final long MAX_SIZE = 1024 * 1024 * 2L;
                    ThrowUtils.throwIf(fileSize > MAX_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    public void deleteFile(File file) {
        if (file == null) {
            return;
        }
        boolean delete = file.delete();
        if (!delete) {
            log.error("file delete failed,filePath：" + file.getAbsolutePath());
        }
    }
}
