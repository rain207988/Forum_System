package com.xzy.forum.exception;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public AppResult handleApplicationException(ApplicationException e) {
        log.error("业务异常：{}", e.getMessage());
        if (e.getErrorResult() != null) {
            return e.getErrorResult();
        }
        return AppResult.failed(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public AppResult handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("参数异常：{}", e.getMessage());
        return AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE.getCode(), e.getMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    public AppResult handleNullPointerException(NullPointerException e) {
        log.error("空指针异常：{}", e.getMessage());
        return AppResult.failed(ResultCode.ERROR_IS_NULL.getCode(), "数据为空");
    }

    @ExceptionHandler(Exception.class)
    public AppResult handleException(Exception e) {
        log.error("系统异常：", e);
        return AppResult.failed(ResultCode.ERROR_SERVICES.getCode(), "系统内部错误，请稍后重试");
    }
}
