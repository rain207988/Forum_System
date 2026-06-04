package com.xzy.forum.exception;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public AppResult<?> handleApplicationException(ApplicationException e, HttpServletResponse response) {
        log.error("业务异常：{}", e.getMessage());
        if (e.getErrorResult() != null) {
            applyHttpStatus(response, e.getErrorResult().getCode());
            return e.getErrorResult();
        }
        return AppResult.failed(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public AppResult<?> handleIllegalArgumentException(IllegalArgumentException e, HttpServletResponse response) {
        log.error("参数异常：{}", e.getMessage());
        applyHttpStatus(response, ResultCode.FAILED_PARAMS_VALIDATE.getCode());
        return AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE.getCode(), e.getMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    public AppResult<?> handleNullPointerException(NullPointerException e, HttpServletResponse response) {
        log.error("空指针异常：{}", e.getMessage());
        applyHttpStatus(response, ResultCode.ERROR_IS_NULL.getCode());
        return AppResult.failed(ResultCode.ERROR_IS_NULL.getCode(), "数据为空");
    }

    @ExceptionHandler(Exception.class)
    public AppResult<?> handleException(Exception e, HttpServletResponse response) {
        log.error("系统异常：", e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return AppResult.failed(ResultCode.ERROR_SERVICES.getCode(), "系统内部错误，请稍后重试");
    }

    private void applyHttpStatus(HttpServletResponse response, long code) {
        if (code == ResultCode.FAILED_UNAUTHORIZED.getCode()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else if (code == ResultCode.FAILED_FORBIDDEN.getCode()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else if (code == ResultCode.FAILED_PARAMS_VALIDATE.getCode()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
