package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.api.imageSearch.model.ImageSearchResult;
import com.yupi.yupicturebackend.common.DeleteRequest;
import com.yupi.yupicturebackend.models.dto.picture.*;
import com.yupi.yupicturebackend.models.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.models.entity.User;
import com.yupi.yupicturebackend.models.vo.PictureVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
* @author 24636
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-11-11 14:24:45
*/
public interface PictureService extends IService<Picture> {
    PictureVo uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    PictureVo getPictureVo(Picture picture, HttpServletRequest request);

    Page<PictureVo> getPictureVoPage(Page<Picture> page, HttpServletRequest request);

    public void ValidPicture(Picture picture);

    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);

    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    void clearPictureFile(Picture oldPicture);

    void checkPictureAuth(User loginUser, Picture picture);

    void deletePicture(DeleteRequest deleteRequest, User loginUser);

    void updatePicture(PictureUpdateRequest pictureUpdateRequest, User loginUser);

    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    List<ImageSearchResult> searchPictureByPicture(SearchPictureByPictureRequest searchPictureByPictureRequest, User loginUser);

    List<PictureVo> searchPictureByColor(SearchPictureByColorRequest searchPictureByColorRequest, User loginUser);

    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);
}
