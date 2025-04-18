package com.wjq.wjqpicturebackend.common;

import com.wjq.wjqpicturebackend.exception.ErrorCode;

public class ResultUtils {

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, "ok", data);
    }

    public static BaseResponse<?> error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode);
    }

    public static BaseResponse<?> error(int code, String message){
        return new BaseResponse<>(code,message,null);
    }

    public static BaseResponse<?> error(ErrorCode errorCode, String message){
        return new BaseResponse<>(errorCode.getCode(),message,null);
    }
}
