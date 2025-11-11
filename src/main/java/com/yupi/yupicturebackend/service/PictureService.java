package com.yupi.yupicturebackend.service;

import com.yupi.yupicturebackend.models.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.models.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.models.entity.User;
import com.yupi.yupicturebackend.models.vo.PictureVo;
import org.springframework.web.multipart.MultipartFile;

/**
* @author 24636
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-11-11 14:24:45
*/
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVo uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

}
