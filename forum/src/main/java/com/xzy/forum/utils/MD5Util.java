package com.xzy.forum.utils;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * MD5加密工具类
 *
 */
public class MD5Util {

    /**
     * 普通MD5加密
     *
     * @param str 原始字符串
     * @return 一次MD5加密后的密文
     */
    public static String md5(String str) {
        return DigestUtils.md5Hex(str);
    }

    /**
     * 原始字符串与Key组合进行一次MD5加密
     *
     * @param str 原始字符串
     * @param key 密钥
     * @return 组合字符串一次MD5加密后的密文
     */
    public static String md5(String str, String key) {
        return DigestUtils.md5Hex(str + key);
    }

    /**
     * 原始字符串加密后与扰动字符串组合再进行一次MD5加密
     *
     * @param str  原始字符串
     * @param salt 扰动字符串
     * @return 加密后的密文
     */
    public static String md5Salt(String str, String salt) {
        return DigestUtils.md5Hex(DigestUtils.md5Hex(str) + salt);
    }

    /**
     * 校验原文与盐加密后是否与传入的密文相同
     *
     * @param original   原字符串
     * @param salt       扰动字符串
     * @param ciphertext 密文
     * @return true 相同, false 不同
     */
    public static boolean verifyOriginalAndCiphertext(String original, String salt, String ciphertext) {
        String md5text = md5Salt(original, salt);
        return md5text.equalsIgnoreCase(ciphertext);
    }
}
