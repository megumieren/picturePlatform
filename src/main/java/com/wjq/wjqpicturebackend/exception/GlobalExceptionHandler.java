package com.wjq.wjqpicturebackend.exception;

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

//    @ExceptionHandler(RuntimeException.class)
//    public BaseResponse<?> runTimeExceptionHandler(RuntimeException e){
//        return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
//    }
}
