package com.wjq.wjqpicturebackend.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.wjq.wjqpicturebackend.common.BaseResponse;
import com.wjq.wjqpicturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e){
        log.error(e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginException(NotLoginException e) {
        log.error("NotLoginException", e);
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, e.getMessage());
    }

    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e) {
        log.error("NotPermissionException", e);
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, e.getMessage());
    }


//    @ExceptionHandler(RuntimeException.class)
//    public BaseResponse<?> runTimeExceptionHandler(RuntimeException e){
//        return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
//    }
}
