package com.yupi.yupicturebackend.models.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public enum UserRoleEnum {
    USER("用户","user"),
    ADMIN("管理员","admin");

    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static UserRoleEnum getUserRoleEnumByValue(String value){
        if (StrUtil.isEmpty(value)){
            return null;
        }

        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if(userRoleEnum.getValue().equals(value)){
                return userRoleEnum;
            }
        }
        return null;
    }
}
