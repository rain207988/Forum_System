package com.xzy.forum.auth;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.exception.ApplicationException;
import com.xzy.forum.model.User;

public final class AuthContext {

    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setCurrentUser(User user) {
        CURRENT_USER.set(user);
    }

    public static User getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static User requireCurrentUser() {
        User user = CURRENT_USER.get();
        if (user == null) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_UNAUTHORIZED));
        }
        return user;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
