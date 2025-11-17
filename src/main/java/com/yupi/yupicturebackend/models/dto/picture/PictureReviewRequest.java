package com.yupi.yupicturebackend.models.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureReviewRequest implements Serializable{
    private static final long serialVersionUID = -4105625238935771976L;

    // 审核的图片id
    private Long id;

    // 图片审核状态
    private Integer reviewStatus;

    // 图片审核信息
    private String reviewMessage;
}
