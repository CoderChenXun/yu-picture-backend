package com.yupi.yupicturebackend.models.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PictureTagCategory implements Serializable {
    private static final long serialVersionUID = -4319592253600083011L;

    private List<String> tagList;

    private List<String> categoryList;
}
