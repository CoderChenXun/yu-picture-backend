package com.yupi.yupicturebackend.models.dto.user;

import lombok.Data;


import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -3532716108525252372L;

    /**
     * 账号
     */
    private String userCount;

    /**
     * 密码
     */
    private String userPassword;
}
