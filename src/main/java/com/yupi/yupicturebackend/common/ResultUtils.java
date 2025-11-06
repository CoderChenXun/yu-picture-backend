package com.yupi.yupicturebackend.common;

import com.yupi.yupicturebackend.exception.ErrorCode;

public class ResultUtils {
    public static <T> BaseResponse<T> success() {
        return new BaseResponse(ErrorCode.SUCCESS);
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse(0, data, "ok");
    }

    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse(code, null, message);
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
        return error(errorCode.getCode(), message);
    }
}
