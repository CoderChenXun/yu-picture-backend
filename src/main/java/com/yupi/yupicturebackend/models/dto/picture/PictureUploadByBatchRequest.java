package com.yupi.yupicturebackend.models.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadByBatchRequest implements Serializable {

    private static final long serialVersionUID = 4213369891425136313L;

    private String searchText;

    private int count;

    /**
     * 名称前缀
     */
    private String namePrefix;
}
