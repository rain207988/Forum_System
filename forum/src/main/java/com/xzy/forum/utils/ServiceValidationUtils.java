package com.xzy.forum.utils;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.exception.ApplicationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ServiceValidationUtils {

    private ServiceValidationUtils() {
    }

    public static void requirePositiveId(Long id, ResultCode resultCode, String fieldName) {
        if (id == null || id <= 0) {
            log.warn("{}，{}={}", resultCode, fieldName, id);
            throw new ApplicationException(AppResult.failed(resultCode));
        }
    }

    public static void requireNonNegative(Integer value, ResultCode resultCode, String fieldName) {
        if (value == null || value < 0) {
            log.warn("{}，{}={}", resultCode, fieldName, value);
            throw new ApplicationException(AppResult.failed(resultCode));
        }
    }

    public static <T> T requireNonNull(T target, ResultCode resultCode, String targetName, Object key) {
        if (target == null) {
            log.warn("{}，{}={}", resultCode, targetName, key);
            throw new ApplicationException(AppResult.failed(resultCode));
        }
        return target;
    }

    public static void requireAffectedOneRow(int row, ResultCode resultCode, String operationName, Object key) {
        if (row != 1) {
            log.warn("{}，{}，key={}", resultCode, operationName, key);
            throw new ApplicationException(AppResult.failed(resultCode));
        }
    }
}
