package com.yupi.yupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class FilePictureUpload extends PictureUploadTemplate{
    @Override
    protected void transferFileOrUrlToTmpFile(Object inputSource, File tmpFile) {
        MultipartFile file = (MultipartFile) inputSource;
        try {
            file.transferTo(tmpFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String getOriginalFileName(Object inputSource) {
        MultipartFile file = (MultipartFile) inputSource;
        return file.getOriginalFilename();
    }

    @Override
    protected void validPhoto(Object inputSource) {
        // 1. 检验参数
        ThrowUtils.throwIf(inputSource == null, ErrorCode.PARAMS_ERROR);
        MultipartFile file = (MultipartFile) inputSource;
        // 2. 校验文件大小
        final long MAX_SIZE = 1024 * 1024 * 2L;
        ThrowUtils.throwIf(file.getSize() > MAX_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
        // 3. 校验是否是允许的图片类型
        String suffix = FileUtil.getSuffix(file.getOriginalFilename()) ;
        final List<String> allowedFileTypes = Arrays.asList("jpg", "jpeg", "png","webp");
        ThrowUtils.throwIf(!allowedFileTypes.contains(suffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }
}
