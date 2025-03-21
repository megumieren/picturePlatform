package com.wjq.wjqpicturebackend.exception;

public class ThrowUtils {

    /**
     *
     * @param condition  条件
     * @param runtimeException 抛出的异常
     */
    public static void throwIf(boolean condition,RuntimeException runtimeException) {
        if(condition) {
            throw runtimeException;
        }
    }

    /**
     *
     * @param condition  条件
     * @param errorCode   错误码
     */
    public static void throwIf(boolean condition,ErrorCode errorCode) {
        throwIf(condition,new BusinessException(errorCode));
    }

    /**
     *
     * @param condition 条件
     * @param errorCode  错误码
     * @param message  错误信息
     */
    public static void throwIf(boolean condition,ErrorCode errorCode,String message) {
        throwIf(condition,new BusinessException(errorCode,message));
    }
}
