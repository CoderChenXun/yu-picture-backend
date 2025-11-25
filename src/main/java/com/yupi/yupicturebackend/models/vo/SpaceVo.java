package com.yupi.yupicturebackend.models.vo;

import cn.hutool.core.bean.BeanUtil;
import com.yupi.yupicturebackend.models.entity.Space;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class SpaceVo implements Serializable {
    private static final long serialVersionUID = 4048241669466721978L;
    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    private Long totalCount;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 脱敏后的用户信息
     */
    private UserVo user;

    public static Space voToObj(SpaceVo spaceVo) {
        if (spaceVo == null) {
            return null;
        }
        Space space = new Space();
        BeanUtil.copyProperties(spaceVo, space);
        return space;
    }

    public static SpaceVo objToVo(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVo spaceVo = new SpaceVo();
        BeanUtil.copyProperties(space, spaceVo);
        return spaceVo;
    }
}
