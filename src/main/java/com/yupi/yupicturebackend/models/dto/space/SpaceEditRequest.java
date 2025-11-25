package com.yupi.yupicturebackend.models.dto.space;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class SpaceEditRequest implements Serializable {
    private static final long serialVersionUID = 8847342004380555106L;

    /**
     * 编辑的spaceId
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;
}
