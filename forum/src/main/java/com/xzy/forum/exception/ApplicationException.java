package com.xzy.forum.exception;


import com.xzy.forum.common.AppResult;
/**
 * ⾃定义异常
 1
 * @Author ⽐特就业课
 */
public class ApplicationException extends RuntimeException {
    private static final long serialVersionUID = -3533806916645793660L;
    // ⾃定义错误
    protected AppResult errorResult;
    // 指定状态码，异常描述
    public ApplicationException(AppResult errorResult) {
        super(errorResult.getMessage());
        this.errorResult = errorResult;
    }
    // ⾃定义异常描述
    public ApplicationException(String message) {
        super(message);
    }
    // 指定异常
    public ApplicationException(Throwable cause) {
        super(cause);
    }
    // ⾃定义异常描述，异常信息
    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
    public AppResult getErrorResult() {
        return errorResult;
    }
}