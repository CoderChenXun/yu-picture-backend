package com.yupi.yupicturebackend.models.vo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.yupi.yupicturebackend.models.entity.Picture;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class PictureVo implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

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
     * userVo
     */
    private UserVo user;

    private static final long serialVersionUID = -1346946354503342999L;

    // 实现静态方法PictureVo转Picture
    public static Picture toPicture(PictureVo pictureVo) {
        if (pictureVo == null) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureVo, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureVo.getTags()));
        return picture;
    }

    // 实现静态方法Picture转PictureVo
    public static PictureVo toPictureVo(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVo pictureVo = new PictureVo();
        BeanUtil.copyProperties(picture, pictureVo);
        // 将tags字段从JSON字符串转换为List<String>
        pictureVo.setTags(JSONUtil.toList(picture.getTags(), String.class));
        return pictureVo;
    }
}
