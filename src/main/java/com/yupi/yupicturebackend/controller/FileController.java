package com.yupi.yupicturebackend.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.CosManager;
import com.yupi.yupicturebackend.common.ResultUtils;
import com.yupi.yupicturebackend.constant.UserConstant;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {
    @Resource
    private CosManager cosManager;

    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestBody MultipartFile multipartFile){
        // 1. 检验参数
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR);
        File file = null;
        String fileName = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", fileName);
        try {
            // 2. 保存临时文件
            file = File.createTempFile(filePath,null);
            multipartFile.transferTo(file);
            cosManager.putObject(filePath,file);
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error("file upload failed,filePath：" + filePath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        }finally {
            // 3. 删除临时文件
            if(file != null){
                boolean delete = file.delete();
                if(!delete){
                    log.error("file delete failed,filePath：" + filePath);
                }
            }
        }
    }

    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download")
    public void testDownloadFile(String filePath, HttpServletResponse response) throws IOException {
        // 1. 检验参数
        ThrowUtils.throwIf(filePath == null, ErrorCode.PARAMS_ERROR);
        // 2. 从cos对象存储中取出cosObject
        COSObjectInputStream content = null;
        try {
            COSObject cosObject = cosManager.getObject(filePath);
            content = cosObject.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(content);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + filePath);
            //写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download failed,filePath：" + filePath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"下载失败");
        }finally {
            // 3. 释放资源
            if(content != null){
                content.close();
            }
        }
    }
}
