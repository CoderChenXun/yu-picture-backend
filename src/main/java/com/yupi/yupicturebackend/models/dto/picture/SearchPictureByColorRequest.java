package com.yupi.yupicturebackend.models.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByColorRequest implements Serializable {
    private static final long serialVersionUID = 1040682156636724299L;

    private String picColor;

    private Long spaceId;
}
