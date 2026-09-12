package com.ucm.config;

import com.ucm.common.ApiResponse;
import com.ucm.common.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> biz(BizException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> unknown(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error("系统异常: " + e.getMessage());
    }
}
