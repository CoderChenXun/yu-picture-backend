package com.yupi.yupicturebackend.models.dto.user;

import lombok.Data;
import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 8351609979357065427L;

    /**
     * 账号
     */
    private String userCount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 校验密码
     */
    private String checkPassword;
}
