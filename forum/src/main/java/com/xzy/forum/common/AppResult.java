package com.xzy.forum.common;

import com.fasterxml.jackson.annotation.JsonInclude;

public class AppResult<T> {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private long code;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String message;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private T data;

    public AppResult() {
    }

    public AppResult(long code, String message) {
        this(code, message, null);
    }

    public AppResult(long code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static AppResult success() {
        return new AppResult(ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage());
    }

    public static <T> AppResult<T> success(T data) {
        return new AppResult(ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> AppResult<T> success(String message, T data) {
        return new AppResult(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static AppResult failed(ResultCode failed, String message) {
        return new AppResult(ResultCode.FAILED.getCode(),
                ResultCode.FAILED.getMessage());
    }

    public static <T> AppResult<T> failed(String message) {
        return new AppResult(ResultCode.FAILED.getCode(), message);
    }

    public static <T> AppResult<T> failed(T data) {
        return new AppResult(ResultCode.FAILED.getCode(),
                ResultCode.FAILED.getMessage(), data);
    }

    public static AppResult failed(ResultCode resultCode) {
        return new AppResult(resultCode.getCode(), resultCode.getMessage());
    }

    public static <T> AppResult<T> failed(long code, String message) {
        return new AppResult(code, message);
    }

    public long getCode() {
        return code;
    }

    public void setCode(long code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
