package com.xzy.forum.exception;

import com.xzy.forum.common.AppResult;

public class ApplicationException extends RuntimeException {

    private static final long serialVersionUID = -3533806916645793660L;

    private final AppResult<?> errorResult;

    public ApplicationException(AppResult<?> errorResult) {
        super(errorResult.getMessage());
        this.errorResult = errorResult;
    }

    public ApplicationException(String message) {
        super(message);
        this.errorResult = null;
    }

    public ApplicationException(Throwable cause) {
        super(cause);
        this.errorResult = null;
    }

    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
        this.errorResult = null;
    }

    public AppResult<?> getErrorResult() {
        return errorResult;
    }
}
