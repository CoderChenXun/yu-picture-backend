package com.yupi.yupicturebackend.models.dto.picture;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class SearchPictureByPictureRequest implements Serializable {
    private static final long serialVersionUID = 6849201570445628185L;
    /**
     * 图片id
     */
    private Long pictureId;
}
