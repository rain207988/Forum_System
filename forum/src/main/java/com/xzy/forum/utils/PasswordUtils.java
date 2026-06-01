package com.xzy.forum.utils;

import com.xzy.forum.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordUtils {

    private static final BCryptPasswordEncoder B_CRYPT = new BCryptPasswordEncoder();

    private PasswordUtils() {
    }

    public static String hash(String rawPassword) {
        return B_CRYPT.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, User user) {
        if (user == null || StringUtils.isEmpty(rawPassword) || StringUtils.isEmpty(user.getPassword())) {
            return false;
        }

        String encodedPassword = user.getPassword();
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$")) {
            return B_CRYPT.matches(rawPassword, encodedPassword);
        }

        if (StringUtils.isEmpty(user.getSalt())) {
            return false;
        }
        return MD5Util.md5Salt(rawPassword, user.getSalt()).equalsIgnoreCase(encodedPassword);
    }

    public static boolean isLegacyPassword(User user) {
        return user != null
                && !StringUtils.isEmpty(user.getPassword())
                && !user.getPassword().startsWith("$2a$")
                && !user.getPassword().startsWith("$2b$")
                && !user.getPassword().startsWith("$2y$");
    }
}
