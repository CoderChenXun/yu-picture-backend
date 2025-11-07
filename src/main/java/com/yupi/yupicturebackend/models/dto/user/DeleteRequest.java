package com.yupi.yupicturebackend.models.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {
    private static final long serialVersionUID = 1003973222804243984L;

    private Long id;
}
