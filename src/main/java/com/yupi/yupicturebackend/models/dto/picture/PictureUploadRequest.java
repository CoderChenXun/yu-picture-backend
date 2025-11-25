package com.yupi.yupicturebackend.models.dto.picture;

import lombok.Data;
import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {
    private static final long serialVersionUID = 2869523393421542338L;

    // 图片id用于修改
    private Long id;

    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * spaceId
     */
    private Long spaceId;
}
